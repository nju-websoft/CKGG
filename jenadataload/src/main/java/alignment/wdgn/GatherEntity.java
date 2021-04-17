package alignment.wdgn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;
import utils.Utils;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class GatherEntity {
  public static final Logger logger = LoggerFactory.getLogger(GatherEntity.class);
  Dataset dataset;

  public GatherEntity() {
    dataset = DatasetFactory.create();
  }

  private void findValidGeonames(String geonamesPath) throws IOException {
    logger.warn("findValidGeonames");
    Model model = dataset.getNamedModel(Namespaces.GKS + "geonames");
    File file = new File(geonamesPath);
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      reader.lines().map(x -> x.split("\t")).map(x -> x[0]).distinct().forEach(x -> {
        model.add(model.createResource(Namespaces.GN + x), RDF.type, model.createResource(Namespaces.GKB + "Q5"));
      });
    }
  }

  /**
   * 加入Wikidata中的实体
   * 
   * 1. 类型为地理实体
   * 2. 有经纬度信息，并且符合标准
   * 3. 有中文标签
   */
  private void findValidWikidataEntities(String sparqlEndpoint) {
    logger.warn("findValidWikidataEntities");
    Model model = dataset.getNamedModel(Namespaces.GKS + "wikidata");
    logger.info("Creating query...");
    VirtGraph graph = new VirtGraph("http://www.wikidata.org/", sparqlEndpoint, "dba", "dba");
    ParameterizedSparqlString pss = new ParameterizedSparqlString();
    pss.setNsPrefix("wd", "http://www.wikidata.org/entity/");
    pss.setNsPrefix("wdt", "http://www.wikidata.org/prop/direct/");
    pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    pss.setCommandText(
            "SELECT DISTINCT ?s WHERE {" +
              "?s wdt:P31/wdt:P279* wd:Q27096213." +
              "?s wdt:P625 ?co." +
              "?s rdfs:label ?o." +
              "FILTER(regex(?co, '^Point\\\\([\\\\-0-9\\\\.]+ [\\\\-0-9\\\\.]+\\\\)$'))\n" +
              "FILTER(" +
                  "(langMatches(LANG(?o), 'zh') && regex(?o, '.*[\\u4e00-\\u9fa5].*')) ||" + 
                  "(!(langMatches(LANG(?o), '*')) && regex(?o, '^[\\u4e00-\\u9fa5]+$'))" +
              ")." +
            "}"
    );
    logger.info("{}", pss.getCommandText());
    try(VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), graph)) {
      ResultSet results = vqe.execSelect();
      logger.info("Create query complete");
      int cnt = 0;
      while(results.hasNext()) {
        QuerySolution result = results.next();
        String uri = result.get("s").asResource().getURI();
        model.add(model.createResource(uri), RDF.type, model.createResource(Namespaces.GKB + "Q5"));
        cnt++;
      }
      logger.info("Loaded {} places.", cnt);
      if(!results.hasNext()) {
        logger.info("Finished");
      }
    }
  }

  public void runmain(String geonamesPath, String sparqlEndpoint) throws IOException {
    findValidGeonames(geonamesPath);
    findValidWikidataEntities(sparqlEndpoint);
    // output as nq
    Utils.write(dataset, Namespaces.GKS + "geonames", "temp_output/new_align/nodes/geonames_nodes");
    Utils.write(dataset, Namespaces.GKS + "wikidata", "temp_output/new_align/nodes/wikidata_nodes");
  }

  public static void main(String[] args) throws IOException {
    (new GatherEntity()).runmain("/home/ylshen/elinga/geonames/allCountries.txt", "jdbc:virtuoso://localhost:1112");
  }
}
