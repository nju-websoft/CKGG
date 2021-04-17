package belong;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.ext.com.google.common.io.Files;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;
import namespaces.Namespaces.PROV;
import utils.Utils;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

/**
 * 需要导入 geonames_reified 和 cn_code_align_reified
 */
public class LoadGeoFeatureBelongByAdmin {
    private static final Logger logger = LoggerFactory.getLogger(LoadGeoFeatureBelongByAdmin.class);

    private String virtUri;
    private String outputPath;
    private Model slimModel;
    private Model reifiedModel;

    /**
     * 谓词顺序，越靠后越优先
     */
    private List<String> preds;
    
    /**
     * 当前最大级别
     */
    private Map<Integer, Integer> curMax;

    public LoadGeoFeatureBelongByAdmin(String virtUri, String outputPath) {
        this.virtUri = virtUri;
        this.outputPath = outputPath;

        preds = Arrays.asList("P12", "P13", "P14", "P15", "P16");
        curMax = new HashMap<>();
    }

    int getOrder(String s) {
        int ret = preds.indexOf(s);
        if(ret == -1) throw new RuntimeException("bad predicate " + s);
        return ret;
    }

    boolean check(int sid, int pid) {
        if(curMax.containsKey(sid)) {
            if(curMax.get(sid) < pid) {
                throw new RuntimeException("not ordered");
            }
            return curMax.get(sid) == pid;
        }
        curMax.put(sid, pid);
        return true;
    }

    public void runmain() throws IOException {
        logger.info("loading belong by admin");

        slimModel = ModelFactory.createDefaultModel();
        reifiedModel = ModelFactory.createDefaultModel();

        VirtGraph graph = new VirtGraph(virtUri, "dba", "dba");
        graph.setReadFromAllGraphs(true);

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("ckgp", Namespaces.GKP);
        pss.setNsPrefix("rdf", RDF.uri);
        pss.setNsPrefix("prov", Namespaces.PROV.PREFIX);

        pss.setCommandText(Files.asCharSource(new File("sparql/load_belong_by_admin.sparql"), StandardCharsets.UTF_8).read());
        
        logger.info("sparql\n{}", pss.getCommandText());
        try (VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), graph)) {
            ResultSet resultSet = vqe.execSelect();
            resultSet.forEachRemaining(x -> {
                if(!check(
                    Integer.parseInt(Utils.getId(x.get("s").asResource().getURI(), "/")),
                    getOrder(Utils.getId(x.get("p").asResource().getURI(), "/")))
                ) {
                    return;
                }
                x.get("s").asResource().inModel(slimModel).addProperty(
                    slimModel.createProperty(Namespaces.GKP + "P10"),
                    x.get("o").inModel(slimModel)
                );
                ReifiedStatement statement = reifiedModel.createReifiedStatement(
                    reifiedModel.createStatement(
                        x.get("s").asResource().inModel(reifiedModel),
                        reifiedModel.createProperty(Namespaces.GKP + "P10"),
                        x.get("o").inModel(reifiedModel)
                    )
                );
                reifiedModel.add(statement, reifiedModel.createProperty(PROV.wasDerivedFrom), x.get("g").inModel(reifiedModel));

                x.get("o").asResource().inModel(slimModel).addProperty(slimModel.createProperty(Namespaces.GKP + "P18"),
                        x.get("s").inModel(slimModel));
                statement = reifiedModel.createReifiedStatement(
                        reifiedModel.createStatement(x.get("o").asResource().inModel(reifiedModel),
                                reifiedModel.createProperty(Namespaces.GKP + "P18"), x.get("s").inModel(reifiedModel)));
                reifiedModel.add(statement, reifiedModel.createProperty(PROV.wasDerivedFrom),
                        x.get("g").inModel(reifiedModel));
            });
        }

        Utils.write(slimModel, Namespaces.GKS + "belong_by_admin", outputPath);
        Utils.write(reifiedModel, Namespaces.GKS + "belong_by_admin_reified", outputPath + "_reified");
    }
}
