package release;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import com.google.common.io.Files;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import loaders.LoaderBase;
import namespaces.Namespaces;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class LoadBySparql {
    private String virtUri;
    private String sparqlPath;

    private static final Logger logger = LoggerFactory.getLogger(LoadBySparql.class);

    public LoadBySparql(String virtUri, String sparqlPath) {
        this.virtUri = virtUri;
        this.sparqlPath = sparqlPath;
    }

    public void runmain(String outputPath) throws IOException {
        new File(outputPath).getParentFile().mkdirs();

        logger.info("Load by sparql {}, {}", virtUri, sparqlPath);

        VirtGraph graph = new VirtGraph(virtUri, "dba", "dba");
        graph.setReadFromAllGraphs(true);

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("ckgp", Namespaces.GKP);
        pss.setNsPrefix("ckgc", Namespaces.GKB);
        pss.setNsPrefix("rdf", RDF.uri);
        pss.setNsPrefix("rdfs", RDFS.uri);
        pss.setNsPrefix("prov", Namespaces.PROV.PREFIX);
        pss.setNsPrefix("wgs84_pos", Namespaces.WGS84POS);
        pss.setNsPrefix("bif", "bif:");

        pss.setCommandText(Files.asCharSource(new File(sparqlPath), StandardCharsets.UTF_8).read());

        logger.info("sparql\n{}", pss.getCommandText());
        try (VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), graph);
             FileOutputStream fos = new FileOutputStream(new File(outputPath));
             GZIPOutputStream gos = new GZIPOutputStream(fos, 64 * 1024)) {
            StreamRDF rdf = StreamRDFWriter.getWriterStream(gos, Lang.NT);

            ResultSet resultSet = vqe.execSelect();
            resultSet.forEachRemaining(x -> {
                rdf.triple(new Triple(x.get("s").asNode(), x.get("p").asNode(), x.get("o").asNode()));
            });

            logger.info("result count {}", resultSet.getRowNumber());
            rdf.finish();
        }
    }
}
