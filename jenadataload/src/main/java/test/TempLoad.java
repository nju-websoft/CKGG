package test;

import static namespaces.Namespaces.GKP;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import loaders.GridLoaderBase;
import namespaces.Namespaces;
import utils.LongLatGrid;
import utils.Utils;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class TempLoad extends GridLoaderBase {
  private static final Logger logger = LoggerFactory.getLogger(TempLoad.class);
  private Dataset dataset;
  private Model model;
  private Model rmodel;
  private String srcName;
  private String outputPath;
  private int cur = 1;

  private String canonicalGraph;

  public TempLoad(String canonicalGraph) {
    this.canonicalGraph = canonicalGraph;
  }

  @Override
  public void load(String srcName, String inputPath, String binaryPath, String outputPath) throws IOException {
    this.srcName = srcName;
    this.outputPath = outputPath;
    dataset = createDataset();
    model = createModel(dataset, srcName);
    rmodel = createModel(dataset, "reified");
    doLoad(inputPath, binaryPath);
  }

  private void writeModel() throws IOException {
    Utils.write(dataset, Namespaces.GKS + srcName, outputPath + "/" + cur);
    Utils.write(dataset, Namespaces.GKS + "reified", outputPath + "_reified" + "/" + cur, ".ttl", RDFFormat.TURTLE);
    cur++;
    dataset = createDataset();
    model = createModel(dataset, srcName);
    rmodel = createModel(dataset, "reified");
  }

  public void doLoad(String inputPath, String binaryPath) throws IOException {
    LongLatGrid grid = new LongLatGrid(new long[]{12, 360, 180}, -180, -90, 1);
    grid.load(binaryPath);

    VirtGraph graph = new VirtGraph(inputPath, "dba", "dba");
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
        double sum = 0;
        for(int i = 0; i < 12; i++) {
          double val = grid.queryByLongLat(new long[]{i}, lon, lat);
          sum += val;
          if(Double.isNaN(val)) continue;
          Resource n = model.createResource();
          addStatement(x.getResource("s").inModel(model), model.createProperty(GKP + "P75"), n);
          addStatement(n, RDF.type, model.createResource(Namespaces.GKB + "Q1632"));
          addStatement(n, model.createProperty(GKP + "P1633"), model.createTypedLiteral(i + 1));
          addStatement(n, model.createProperty(GKP + "P1634"), model.createTypedLiteral(val));
        }
        if(!Double.isNaN(sum)) {
          addStatement(x.getResource("s").inModel(model), model.createProperty(GKP + "P71"), model.createTypedLiteral(sum / 12));
        }
        cnt++;
        if(cnt % 1000000 == 1000000 - 1) {
          writeModel();
        }
      }
      if(cnt % 1000000 != 1000000 - 1) {
        writeModel();
      }
      logger.info("loaded {} lines", cnt);
    }
  }

  private void addStatement(Resource s, Property property, RDFNode n) {
    Statement stmt = model.createStatement(s, property, n);
    model.add(stmt);
    new Utils.ReifiedStatementBuilder(stmt, rmodel).setDerivedByLiteral("http://berkeleyearth.org/data/");
  }

  @Override
  public void doLoad(Model model, String inputPath, String binaryPath) throws IOException {
    throw new NotImplementedException();
  }
}
