package alignment.wdgn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.Utils;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class QueryExecutor {
  public static final Logger logger = LoggerFactory.getLogger(QueryExecutor.class);

  VirtGraph graph;
  ParameterizedSparqlString pss;

  public static final int BATCH_SIZE = 1024;

  public QueryExecutor(String uri) {
    graph = new VirtGraph("http://www.wikidata.org/", uri, "dba", "dba");
    pss = new ParameterizedSparqlString();
    pss.setCommandText(
            "SELECT ?o WHERE { ?s <http://www.w3.org/2000/01/rdf-schema#label> ?o }"
    );
  }

  public Set<String> getSingleWdNames(String uri) {
    pss.setIri("s", uri);
    try(VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), graph)) {
      Set<String> ret = new HashSet<>();

      ResultSet results = vqe.execSelect();
      while(results.hasNext()) {
        QuerySolution result = results.next();
        Literal node = result.get("o").asLiteral();
        String lang = node.getLanguage();
        String value = node.getString();
        if(Utils.isZh(lang, value)) {
          ret.add(lang.replaceAll("\\s+", " ") + "\t" + value.replaceAll("\\s+", " "));
        }
      }
      return ret;
    }
  }

  public Map<String, Set<String>> getMultipleWdNames(List<String> uris) {
    Map<String, Set<String>> ret = new HashMap<>();

    logger.info("COUNT {}", uris.size());
    for(int i = 0; i < uris.size(); i += BATCH_SIZE) {
      logger.info("BATCH {}", i);
      List<String> batch = new ArrayList<>();
      for(int j = 0; j < BATCH_SIZE && i + j < uris.size(); j++) {
        batch.add("w:" + uris.get(i + j));
      }
      ParameterizedSparqlString qpss = new ParameterizedSparqlString();
      qpss.setCommandText(
              "SELECT ?s ?o WHERE { ?s <http://www.w3.org/2000/01/rdf-schema#label> ?o FILTER (?s IN (" + String.join(",", batch) + "))}"
      );
      qpss.setNsPrefix("w", "http://www.wikidata.org/entity/");
      try(VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(qpss.asQuery(), graph)) {
        ResultSet results = vqe.execSelect();
        if(!results.hasNext()) {
          throw new RuntimeException("empty result");
        }
        while(results.hasNext()) {
          QuerySolution result = results.next();
          Literal node = result.get("o").asLiteral();
          String[] uri = result.get("s").asResource().getURI().split("/");
          String id = uri[uri.length - 1];
          String lang = node.getLanguage();
          String value = node.getString();

          lang = lang.replaceAll("\\s+", " ");
          value = value.replaceAll("\\s+", " ");
          if(lang.isEmpty()) lang = "none";

          if(Utils.isZh(lang, value)) {
            ret.computeIfAbsent(id, k -> new HashSet<>()).add(lang + "\t" + value);
          }
        }
      }
    }
    return ret;
  }
}