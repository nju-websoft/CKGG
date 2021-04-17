package alignment.wdgn;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.LiteralRequiredException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;
import utils.Utils;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class WikidataToGeonamesAlign {
  public static final Logger logger = LoggerFactory.getLogger(WikidataToGeonamesAlign.class);

  Set<String> validGeonames;
  Dataset dataset;

  public WikidataToGeonamesAlign() {
    dataset = DatasetFactory.create();
  }

  private void addEdge(String x, String y, String source) {
    Model model = dataset.getNamedModel(source);
    model.add(model.createResource(x), OWL.sameAs, model.createResource(y));
  }

  /**
   * we extract all geonames to wikidata alignment as edges
   * @param geonamesAlternativeNamesPath
   * @throws IOException
   */
  private void findGeonamesToWikidataAlign(String geonamesAlternativeNamesPath) throws IOException {
    logger.warn("findGeonamesToWikidataAlign");
    File file = new File(geonamesAlternativeNamesPath);
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      reader.lines().map(x -> x.split("\t")).filter(x -> x[2].equals("wkdt")).forEach(x -> {
        addEdge(Namespaces.GEONAMES + x[1], Namespaces.WD + x[3], Namespaces.GKS + "geonames");
      });
    }
  }

  /**
   * first we put all valid geonames to a set to avoid invalid link
   * @param geonamesPath
   * @throws IOException
   */
  private void findValidGeonames(String geonamesPath) throws IOException {
    logger.warn("findValidGeonames");
    File file = new File(geonamesPath);
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      validGeonames = reader.lines().map(x -> x.split("\t")).map(x -> x[0]).collect(Collectors.toSet());
    }
  }

  /**
   * we extract all wikidata to geonames links
   * @param sparqlEndpoint
   */
  private void findWikidataToGeonamesAlign(String sparqlEndpoint) {
    logger.warn("findWikidataToGeonamesAlign");
    VirtGraph set = new VirtGraph("http://www.wikidata.org/", sparqlEndpoint, "dba", "dba");

    Query sparql = QueryFactory.create(
            "SELECT distinct ?s ?o WHERE { ?s <http://www.wikidata.org/prop/direct/P1566> ?o }");

    try(VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, set)) {
      ResultSet results = vqe.execSelect();

      int edgeCnt = 0;
      int failedCnt = 0;
      while (results.hasNext()) {
        QuerySolution result = results.next();
        try {
          Resource from = result.get("s").asResource();
          String to = result.get("o").asLiteral().getString();
          if(!validGeonames.contains(to)) {
            continue;
          }
          String[] split = from.getURI().split("/");
          String froms = split[split.length - 1];
          addEdge(Namespaces.WD + froms, Namespaces.GEONAMES + to, Namespaces.GKS + "wikidata");
          edgeCnt++;
        } catch(LiteralRequiredException e) {
          failedCnt++;
        }
      }
      logger.info("findGeonamesToWikidataAlign {} {}", edgeCnt, failedCnt);
    }
  }

  public void runmain(String geonamesPath, String geonamesAlternativeNamesPath, String sparqlEndpoint) throws IOException {
    findValidGeonames(geonamesPath);
    findGeonamesToWikidataAlign(geonamesAlternativeNamesPath);
    findWikidataToGeonamesAlign(sparqlEndpoint);
    // output as nq
    Utils.write(dataset, Namespaces.GKS + "geonames", "temp_output/new_align/edges/geonames_to_wikidata");
    Utils.write(dataset, Namespaces.GKS + "wikidata", "temp_output/new_align/edges/wikidata_to_geonames");
  }

  public static void main(String[] args) throws IOException {
    (new WikidataToGeonamesAlign()).runmain("/home/ylshen/elinga/geonames/allCountries.txt", "/home/ylshen/elinga/geonames/alternateNamesV2.txt", "jdbc:virtuoso://localhost:1112");
  }
}
