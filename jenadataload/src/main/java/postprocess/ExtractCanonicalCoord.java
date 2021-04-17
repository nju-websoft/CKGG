package postprocess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import loaders.LoaderBase;
import namespaces.Namespaces;
import namespaces.Namespaces.PROV;
import utils.Utils;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class ExtractCanonicalCoord extends LoaderBase {
  private static final Logger logger = LoggerFactory.getLogger(ExtractCanonicalCoord.class);

  private static class Coord {
    double lat;
    double lon;
    Resource source;
    Literal literal;

    Coord(Resource source, Literal literal) {
      this.literal = literal;
      String wkt = literal.getString();
      this.source = source;
      Pattern pattern = Pattern.compile("^Point\\(([^\\s]+) ([^\\s]+)\\)$", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(wkt);
      if(!matcher.find()) {
        logger.error("matcher can't find {}", wkt);
        throw new RuntimeException();
      }
      lon = Double.parseDouble(matcher.group(1));
      lat = Double.parseDouble(matcher.group(2));
    }
  }

  String endpoint;

  public ExtractCanonicalCoord(String endpoint) {
    this.endpoint = endpoint;
  }

  Coord mergeCoord(List<Coord> coords) {
    if(coords.isEmpty()) {
      throw new IllegalArgumentException();
    }
    int n = coords.size();
    Coord ret = null;
    double minSum = 1e100;
    for(int i = 0; i < n; i++) {
      double disSum = 0;
      for(int j = 0; j < n; j++) {
        if(i == j) continue;
        disSum += Utils.distance(coords.get(i).lat, coords.get(j).lat, coords.get(i).lon, coords.get(j).lon, 0.0, 0.0);
      }
      if(disSum < minSum) {
        minSum = disSum;
        ret = coords.get(i);
      }
    }
    if(ret == null) {
      throw new RuntimeException();
    }
    return ret;
  }

  @Override
  public void doLoad(Model model, String deprecated) throws IOException {
    VirtGraph graph = new VirtGraph(endpoint, "dba", "dba");
    graph.setReadFromAllGraphs(true);

    ParameterizedSparqlString pss = new ParameterizedSparqlString();
    // step 1. get all coord
    pss.setNsPrefix("gkp", Namespaces.GKP);
    pss.setNsPrefix("rdf", RDF.uri);
    pss.setCommandText(
      "SELECT * { SELECT ?s ?o ?g {\n" +
      "  ?stmt rdf:subject ?s.\n" +
      "  ?stmt rdf:predicate gkp:P23.\n" +
      "  ?stmt rdf:object ?o.\n" +
      "  ?stmt <http://www.w3.org/ns/prov#wasDerivedFrom> ?g.\n" +
      "} ORDER BY ?s }\n"
    );

    try(VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), graph)) {
      ResultSet resultSet = vqe.execSelect();
      String lastSubj = "";
      List<Coord> coords = new ArrayList<>();
      while(resultSet.hasNext()) {
        QuerySolution qs = resultSet.next();
        String curSubj = qs.get("s").asResource().getURI();
        if(curSubj.equals(lastSubj)) {
          coords.add(new Coord(qs.get("g").asResource(), qs.get("o").asLiteral()));
        } else {
          if(!coords.isEmpty()) {
            Coord merged = mergeCoord(coords);
            ReifiedStatement stmt = model.createReifiedStatement(
              model.createStatement(model.createResource(lastSubj), model.createProperty(Namespaces.GKP + "P23"), merged.literal.inModel(model))
            );
            stmt.addProperty(model.createProperty(PROV.wasDerivedFrom), merged.source);
          }
          coords = new ArrayList<>();
          coords.add(new Coord(qs.get("g").asResource(), qs.get("o").asLiteral()));
          lastSubj = curSubj;
        }
      }
      if(!coords.isEmpty()) {
        Coord merged = mergeCoord(coords);
        ReifiedStatement stmt = model.createReifiedStatement(
          model.createStatement(model.createResource(lastSubj), model.createProperty(Namespaces.GKP + "P23"), merged.literal.inModel(model))
        );
        stmt.addProperty(model.createProperty(PROV.wasDerivedFrom), merged.source);
      }
    }
  }
}
