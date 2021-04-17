package expr;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import com.google.common.collect.Streams;
import com.google.common.io.Files;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import loaders.LoaderBase;
import namespaces.Namespaces;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class GetExpr2Entities {
    private String virtUri;
    private String sparqlPath;
    private int lowerBound;
    private int upperBound;
    private int sampleSize;

    private static final Logger logger = LoggerFactory.getLogger(GetExpr2Entities.class);

    /**
     * 
     * @param virtUri
     * @param sparqlPath
     * @param lowerBound inclusive
     * @param upperBound inclusive
     * @param sampleSize
     */
    public GetExpr2Entities(String virtUri, String sparqlPath, int lowerBound, int upperBound, int sampleSize) {
        this.virtUri = virtUri;
        this.sparqlPath = sparqlPath;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.sampleSize = sampleSize;
    }

    private <T> List<T> randomSample(List<T> list, Random random, int count) {
        List<Integer> range = new ArrayList<>();
        for(int i = 0; i < list.size(); i++) {
            range.add(i);
        }
        Collections.shuffle(range, random);
        List<T> ret = new ArrayList<>();
        for(int i = 0; i < count; i++) {
            ret.add(list.get(range.get(i)));
        }
        return ret;
    }

    public void runmain(String outputPath) throws IOException {
        new File(outputPath).getParentFile().mkdirs();

        logger.info("Load by sparql {}, {}", virtUri, sparqlPath);

        VirtGraph graph = new VirtGraph(virtUri, "dba", "dba");
        graph.setReadFromAllGraphs(true);

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("ckgp", Namespaces.GKP);
        pss.setNsPrefix("ckgc", Namespaces.GKB);
        pss.setNsPrefix("rdf", RDF.getURI());
        pss.setNsPrefix("rdfs", RDFS.getURI());
        pss.setNsPrefix("owl", OWL.getURI());
        pss.setNsPrefix("prov", Namespaces.PROV.PREFIX);
        pss.setNsPrefix("wgs84_pos", Namespaces.WGS84POS);
        pss.setNsPrefix("bif", "bif:");

        pss.setCommandText(Files.asCharSource(new File(sparqlPath), StandardCharsets.UTF_8).read());
        pss.setLiteral("lb", lowerBound);
        pss.setLiteral("ub", upperBound);

        logger.info("sparql\n{}", pss.getCommandText());
        try (VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), graph);
             FileWriter fos = new FileWriter(new File(outputPath));
             BufferedWriter bos = new BufferedWriter(fos)) {
            ResultSet resultSet = vqe.execSelect();
            List<String> uris = Streams.stream(resultSet).map(x -> x.get("s").asNode().getURI()).sorted().collect(Collectors.toList());
            Random random = new Random(19260817);
            List<String> samples = randomSample(uris, random, sampleSize);
            samples.forEach(x -> {
                try {
                    bos.write(x + '\n');
                } catch (IOException e) {
                    throw new RuntimeException("", e);
                }
            });

            logger.info("result count {}", resultSet.getRowNumber());
            bos.flush();
        }
    }
}
