package loaders;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterables;
import com.hankcs.hanlp.HanLP;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;
import utils.Utils;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
/**
 * 加入Wikidata中的实体的属性，要求为
 * 
 * 1. 类型为地理实体
 * 2. 有经纬度信息
 * 3. 有中文标签
 */
public class LoadWikidataTriples extends LoaderBase {
    private static final Logger logger = LoggerFactory.getLogger(LoadWikidataTriples.class);

    interface Inserter {
        public void insert(QuerySolution result, Model model);
    }

    private Map<String, Inserter> inserters;

    public LoadWikidataTriples() {
        Pattern pattern = Pattern.compile("^Point\\(([\\-0-9\\.]+) ([\\-0-9\\.]+)\\)$");
        inserters = new HashMap<>();
        inserters.put(Namespaces.WDT + "P625", (result, model) -> {
            String subj = result.get("s").asResource().getURI();
            String node = result.get("o").asLiteral().getString();
            Matcher matcher = pattern.matcher(node);
            if(!matcher.find()) {
                throw new RuntimeException();
            }
            model.add(model.createResource(subj), model.createProperty(Namespaces.GKP + "P23"), model.createTypedLiteral(node, Namespaces.GEOSPARQL.wktLiteral));
            double longitude = Double.parseDouble(matcher.group(1));
            double latitude = Double.parseDouble(matcher.group(2));
            model.add(model.createResource(subj), model.createProperty(Namespaces.WGS84POS + "lon"), model.createTypedLiteral(longitude));
            model.add(model.createResource(subj), model.createProperty(Namespaces.WGS84POS + "lat"), model.createTypedLiteral(latitude));
        });
        inserters.put(RDFS.label.getURI(), (result, model) -> {
            String subj = result.get("s").asResource().getURI();
            Literal node = result.get("o").asLiteral();
            if(!Utils.isZh(node.getLanguage(), node.getString())) {
                return;
            }
            Literal newNode = model.createLiteral(HanLP.t2s(node.getString()), node.getLanguage());
            model.add(model.createResource(subj), RDFS.label, newNode);
        });
    }

    private void loadFromQuery(Model model, VirtGraph graph, ParameterizedSparqlString pss) {
        try(VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), graph)) {
            ResultSet results = vqe.execSelect();
    
            int cnt = 0;
            while(results.hasNext()) {
                QuerySolution result = results.next();
                String puri = result.get("p").asResource().getURI();
    
                inserters.get(puri).insert(result, model);
                cnt++;
            }
            logger.info("Loaded {} triples.", cnt);
        }
    }

    private void loadBatchCoord(Model model, VirtGraph graph, ParameterizedSparqlString pss, List<String> subjects) {
        pss.setCommandText(
            "SELECT DISTINCT ?s ?p ?o WHERE {\n" +
            "    ?s ?p ?o.\n" +
            "    FILTER(?s IN (" + String.join(",", subjects) + ")). \n" +
            "    FILTER(?p = wdt:P625).\n" +
            "    FILTER(regex(?o, '^Point\\\\([\\\\-0-9\\\\.]+ [\\\\-0-9\\\\.]+\\\\)$')).\n" +
            "}"
        );
        loadFromQuery(model, graph, pss);
    }

    private void loadBatchLabel(Model model, VirtGraph graph, ParameterizedSparqlString pss, List<String> subjects) {
        pss.setCommandText(
            "SELECT DISTINCT ?s ?p ?o WHERE {\n" +
            "    ?s ?p ?o.\n" +
            "    FILTER(?s IN (" + String.join(",", subjects) + ")). \n" +
            "    FILTER(?p = rdfs:label).\n" +
            "    FILTER(langMatches(lang(?o), 'zh')).\n" +
            "}"
        );
        loadFromQuery(model, graph, pss);
    }

    @Override
    public void doLoad(Model model, String inputPath) throws IOException {
        VirtGraph graph = new VirtGraph("http://www.wikidata.org/", inputPath, "dba", "dba");
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("wd", "http://www.wikidata.org/entity/");
        pss.setNsPrefix("wdt", "http://www.wikidata.org/prop/direct/");
        pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

        Set<String> wdUris = RDFDataMgr.loadDataset("output/new_align.nt.gz").getDefaultModel().listStatements()
            .filterKeep(x -> x.getObject().asResource().getURI().contains(Namespaces.WD))
            .mapWith(x -> x.getObject().asResource().getURI().replace(Namespaces.WD, "wd:")).toSet();
        int batchSize = 1024;
        Iterables.partition(wdUris, batchSize).forEach(subjects -> {
            loadBatchCoord(model, graph, pss, subjects);
            loadBatchLabel(model, graph, pss, subjects);
        });
    }
}
