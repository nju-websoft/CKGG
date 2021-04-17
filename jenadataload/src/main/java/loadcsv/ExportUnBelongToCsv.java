package loadcsv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;
import utils.Utils;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

/**
 * 筛选掉高级别地点，防止这些地点由于缺少多边形被添加了错误的上位关系
 */
public class ExportUnBelongToCsv {
    private static final Logger logger = LoggerFactory.getLogger(ExportUnBelongToCsv.class);

    String virtUri;
    String graphName;
    public ExportUnBelongToCsv(String virtUri) {
        this.virtUri = virtUri;
    }
    
    public void runmain(String outputPath) throws IOException {
        VirtGraph graph = new VirtGraph(virtUri, "dba", "dba");
        graph.setReadFromAllGraphs(true);

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("gkp", Namespaces.GKP);
        pss.setNsPrefix("gkb", Namespaces.GKB);
        pss.setNsPrefix("gks", Namespaces.GKS);
        pss.setCommandText(
            "SELECT DISTINCT ?s WHERE {\n" +
            "?s a gkb:Q5.\n" +
            "FILTER (NOT EXISTS { ?s gkp:P8 ?o. VALUES ?o {\"A.PCLI\" \"A.ADM1\"} })\n" +
            "}"
        );
        logger.info("sparql\n{}", pss.getCommandText());
        try (
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputPath)));
            VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), graph)
        ) {
            ResultSet resultSet = vqe.execSelect();
            resultSet.forEachRemaining(x -> {
                String s = Utils.getId(x.get("s").asResource().getURI(), "/");
                try {
                    writer.write(s.trim() + '\n');
                } catch (IOException e) {
                    throw new RuntimeException("", e);
                }
            });
        }
    }
}
