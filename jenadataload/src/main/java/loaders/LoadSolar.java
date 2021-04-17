package loaders;

import java.io.IOException;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;
import utils.LongLatGrid;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class LoadSolar extends LoaderBase {
  private static final Logger logger = LoggerFactory.getLogger(LoadSolar.class);
  LongLatGrid grid;

  String virtuosoJdbcUri;
  String binaryPath;
  String canonicalGraph;

  public LoadSolar(String virtuosoJdbcUri, String binaryPath, String canonicalGraph) {
    this.virtuosoJdbcUri = virtuosoJdbcUri;
    this.binaryPath = binaryPath;
    this.canonicalGraph = canonicalGraph;
  }

  @Override
  public void doLoad(Model model, String deprecated) throws IOException {
    grid = new LongLatGrid(new long[]{43200, 13800}, -180, -55, 360.0 / 43200.0);
    grid.load(binaryPath);

    VirtGraph graph = new VirtGraph(virtuosoJdbcUri, "dba", "dba");
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
      VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), graph)
    ) {
      ResultSet resultSet = vqe.execSelect();
      int cnt = 0;
      while(resultSet.hasNext()) {
        QuerySolution x = resultSet.next();
        String outer = x.get("o").asLiteral().getString();
        String inner = outer.substring(6, outer.length() - 1);
        double lon = Double.parseDouble(inner.split(" ")[0]);
        double lat = Double.parseDouble(inner.split(" ")[1]);
        double result = grid.queryByLongLat(new long[0], lon, lat);
        if (Double.isNaN(result)) continue;
        model.add(x.getResource("s").inModel(model), model.createProperty(Namespaces.GKP + "P39"), model.createTypedLiteral(result * 365.24219));
        cnt++;
      }
      logger.info("loaded {} lines", cnt);
    }
  }
}
