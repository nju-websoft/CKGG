package postprocess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;
import utils.Utils;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

/**
 * Extracts canonical coord and sampled region from natural earth
 */
public class ExportCanonicalCoordToCsv {
  private static final Logger logger = LoggerFactory.getLogger(ExportCanonicalCoordToCsv.class);

  String canonicalGraph;
  String neGraph;

  public ExportCanonicalCoordToCsv(String canonicalGraph, String neGraph) {
    this.canonicalGraph = canonicalGraph;
    this.neGraph = neGraph;
  }

  public void runmain(String dburi, String outputCoordPath, String outputRegionPath) throws IOException {
    VirtGraph graph = new VirtGraph(dburi, "dba", "dba");
    graph.setReadFromAllGraphs(true);

    ParameterizedSparqlString pss = new ParameterizedSparqlString();
    // step 1. get all coord
    pss.setNsPrefix("gkp", Namespaces.GKP);
    pss.setNsPrefix("gks", Namespaces.GKS);
    pss.setNsPrefix("rdf", RDF.uri);
    pss.setCommandText(
      "SELECT ?s ?o WHERE { GRAPH gks:" + canonicalGraph + " {\n" +
      "  ?n rdf:subject ?s.\n" +
      "  ?n rdf:predicate gkp:P23.\n" +
      "  ?n rdf:object ?o.\n" +
      "}}\n"
    );

    try(
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputCoordPath)));
      VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), graph)
    ) {
      ResultSet resultSet = vqe.execSelect();
      int cnt = 0;
      while(resultSet.hasNext()) {
        QuerySolution x = resultSet.next();
        String outer = x.get("o").asLiteral().getString();
        if(!outer.substring(0, 6).equalsIgnoreCase("POINT(")) {
          throw new RuntimeException(outer);
        }
        String inner = outer.substring(6, outer.length() - 1);
        String lon = inner.split(" ")[0];
        String lat = inner.split(" ")[1];
        writer.write(Utils.getId(x.get("s").asResource().getURI(), "/") + "\t" + lon + "\t" + lat + "\n");
        cnt++;
      }
      logger.info("loaded {} lines", cnt);
      writer.flush();
    }

    pss.setCommandText(
      "SELECT ?s (SAMPLE(?o) AS ?os) WHERE { GRAPH gks:" + neGraph + " {\n" +
      "  ?s gkp:P24 ?o.\n" +
      "}} GROUP BY ?s\n"
    );

    try(
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputRegionPath)));
      VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), graph)
    ) {
      ResultSet resultSet = vqe.execSelect();
      int cnt = 0;
      while(resultSet.hasNext()) {
        QuerySolution x = resultSet.next();
        writer.write(Utils.getId(x.get("s").asResource().getURI(), "/") + "\t" + x.get("os").asLiteral().getString() + "\n");
        cnt++;
      }
      logger.info("loaded {} lines", cnt);
      writer.flush();
    }
  }

  public static void main(String[] args) throws IOException {
    new ExportCanonicalCoordToCsv("canonical_coord", "natural_earth").runmain("jdbc:virtuoso://localhost:1111", "ne_dataset/postgis_ne/import/new_coord.csv", "ne_dataset/postgis_ne/import/new_region.csv");
  }
}
