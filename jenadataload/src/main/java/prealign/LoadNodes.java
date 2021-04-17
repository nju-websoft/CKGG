package prealign;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import loaders.LoaderBase;
import namespaces.Namespaces;
import utils.Utils;

/**
 * 合并节点
 */
public class LoadNodes extends LoaderBase {
    private static final Logger logger = LoggerFactory.getLogger(LoadNodes.class);

    private String inputDir;
    private Dataset dataset;
    private Map<String, Integer> nodes;
    private List<String> nodeNames;
    private List<Set<Integer>> graph;

    private boolean[] visited;
    private int[] belong;
    private List<List<Integer>> comps;

    private List<List<Integer>> validComps;
    private Set<Integer> validNodes;

    private int addNode(String uri) {
        if(nodes.containsKey(uri)) return nodes.get(uri);
        nodes.put(uri, graph.size());
        nodeNames.add(uri);
        graph.add(new HashSet<>());
        return nodes.size() - 1;
    }

    private void loadAllEdges() {
        dataset = DatasetFactory.create();

        File dir = new File(inputDir, "edges");
        for(File file: dir.listFiles((d, name) -> name.toLowerCase().endsWith(".nt.gz"))) {
            RDFDataMgr.read(dataset, file.getAbsolutePath());
            logger.info("dataset size {}", dataset.getDefaultModel().size());
        }
        nodes = new HashMap<>();
        nodeNames = new ArrayList<>();
        graph = new ArrayList<>();

        dataset.getDefaultModel().listStatements().forEachRemaining(stmt -> {
            if(!stmt.getPredicate().equals(OWL.sameAs)) {
                logger.info("bad statement, expect {}, actual {}.", OWL.sameAs, stmt);
            }
            String subj = Utils.removeTrailingSlash(stmt.getSubject().getURI());
            String obj = Utils.removeTrailingSlash(stmt.getObject().asNode().getURI());
            int subjId = addNode(subj);
            int objId = addNode(obj);
            graph.get(subjId).add(objId);
            graph.get(objId).add(subjId);
        });
        logger.info("{} edges, {} nodes", dataset.getDefaultModel().size(), nodes.size());
    }

    private void loadAllNodes() {
        dataset = DatasetFactory.create();

        File dir = new File(inputDir, "nodes");
        for(File file: dir.listFiles((d, name) -> name.toLowerCase().endsWith(".nt.gz"))) {
            RDFDataMgr.read(dataset, file.getAbsolutePath());
            logger.info("dataset size {}", dataset.getDefaultModel().size());
        }

        validNodes = new HashSet<>();
        dataset.getDefaultModel().listStatements().forEachRemaining(stmt -> {
            if(!stmt.getPredicate().equals(RDF.type)) {
                logger.error("bad statement, expect {}, actual {}.", RDF.type, stmt);
            }
            if(!stmt.getObject().asNode().getURI().equals(Namespaces.GKB + "Q5")) {
                logger.error("bad statement, expect {}, actual {}.", Namespaces.GKB + "Q5", stmt);
            }
            String subj = Utils.removeTrailingSlash(stmt.getSubject().getURI());
            int subjId = addNode(subj);
            validNodes.add(subjId);
        });

        logger.info("{} valid nodes", validNodes.size());
    }

    private void dfs(int parent, int cur) {
        if(visited[cur]) return;
        visited[cur] = true;
        belong[cur] = parent;
        comps.get(comps.size() - 1).add(cur);
        for(int y: graph.get(cur)) {
            dfs(parent, y);
        }
    }

    private void findAllComp() {
        int n = graph.size();
        visited = new boolean[n];
        belong = new int[n];
        comps = new ArrayList<>();
        for(int i = 0; i < n; i++) {
            if(visited[i]) continue;
            comps.add(new ArrayList<>());
            dfs(i, i);
        }
        logger.info("{} comps", comps.size());
    }

    private void filterComp() {
        validComps = comps.stream().filter(nodes -> nodes.stream().anyMatch(x -> validNodes.contains(x))).collect(Collectors.toList());
        logger.info("{} valid comps", validComps.size());
    }

    private void outputComp(Model model) {
        int n = validComps.size();
        for(int i = 0; i < n; i++) {
            // incresing index from 1
            Resource subj = model.createResource(Namespaces.GKD + (i + 1));
            for(Integer uri: validComps.get(i)) {
                subj.addProperty(OWL.sameAs, model.createResource(nodeNames.get(uri)));
            }
        }
    }

    public LoadNodes(String inputDir) {
        this.inputDir = inputDir;
    }

    @Override
    public void doLoad(Model model, String deprecated) throws IOException {
        loadAllEdges();

        loadAllNodes();
        
        findAllComp();

        filterComp();

        outputComp(model);
    }

    public static void main(String[] args) throws IOException {
        new LoadNodes("temp_output/new_align").load("unspecified", null, "temp_output/new_align/output/new_align");
    }
}
