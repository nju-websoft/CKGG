package test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Files;
import com.google.gson.Gson;

import alignment.AddStatCode;
import loaders.*;
import namespaces.Namespaces;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import postprocess.ExportCanonicalCoordToCsv;
import postprocess.ExtractCanonicalCoord;
import postprocess.ExtractSlim;
import prealign.ConvertData;
import prealign.LoadNodes;
import utils.Utils;

public class AllDataLoad {
  private static final Logger logger = LoggerFactory.getLogger(AllDataLoad.class);
  private static final Gson gson = new Gson();
  private static final String mainVirtuosoUri = "jdbc:virtuoso://localhost:1113";

  private static void processOntology() throws IOException {
    logger.info("Processing ontology");
    new File("output/ontology").mkdirs();
    Files.asCharSink(new File("output/ontology/ontology.owl.graph"), StandardCharsets.UTF_8).write(Namespaces.GKS + "ontology_manual");
    Files.asCharSink(new File("output/ontology/mapping.owl.graph"), StandardCharsets.UTF_8).write(Namespaces.GKS + "ontology_manual");
    Files.copy(new File("../release/ontology.owl"), new File("output/ontology/ontology.owl"));
    Files.copy(new File("../release/mapping.owl"), new File("output/ontology/mapping.owl"));
  }

  // private static void tempConvert() throws IOException {
  //   Dataset aligns = RDFDataMgr.loadDataset("output/align.nt.gz");
  //   Dataset output = DatasetFactory.create();
  //   Utils.convertToReify(aligns.getDefaultModel(), output.getNamedModel(Namespaces.GKS + "reified"), null);
  //   Utils.write(output, Namespaces.GKS + "reified", "output/align_reified");
  // }

  public static void main(String[] args) throws IOException {
    processOntology();
    Files.asCharSink(new File("output/global.graph"), StandardCharsets.UTF_8).write(Namespaces.GKS + "unspecified");

    // pass 1, alignment required
    // new LoadNodes("temp_output/new_align").load("unspecified", null, "output/new_align");
    // new LoadWikidataTriples().load("wikidata", "jdbc:virtuoso://localhost:1112", "output/wikidata/wikidata");
    // new LoadNaturalEarth().load("natural_earth", "jdbc:postgresql://localhost:5433/postgres?user=postgres&password=root", "output/wikidata/natural_earth");
    // new LoadGeonamesTriples().runmain("output/geonames");
    // new ConvertData().runmain("output/new_align.nt.gz");
    logger.warn("Should reload here because of changed entity id!");

    // reload here, change hardcoded uri, and run pass 2
    // new AddStatCode().runmain("jdbc:virtuoso://localhost:1113", "/home/ylshen/elinga/province-city-china/mydata/processed.jsonl", "output/province_city_china_align");
    // new ExtractCanonicalCoord("jdbc:virtuoso://localhost:1113").load("canonical_coord", null, "output/canonical_coord");
    logger.warn("Should reload here because of canonical coord loaded!");
    new ExportCanonicalCoordToCsv("canonical_coord", "natural_earth").runmain(
       "jdbc:virtuoso://localhost:1113",
       "/home/ylshen/elinga/ne_dataset/postgis_ne/import/new_coord.csv",
       "/home/ylshen/elinga/ne_dataset/postgis_ne/import/new_region.csv"
    );

    logger.warn("Should reload db and run sql in docs/postgres.sql here!");
    // add gis info to db, and run pass 3, pass 3
    logger.warn("Add gis info to PostGIS db!");
    // new LoadGeoFeatureBelong("jdbc:postgresql://localhost:5433/postgres?user=postgres&password=root", "belongs_v4")
    //     .loadWithReify("natural_earth", "http://www.naturalearthdata.com/", null, "./output/feature_belong");
    // new PrecipLoad("canonical_coord").load(
    //  "precip",
    //  "jdbc:virtuoso://localhost:1113",
    //  "/home/ylshen/elinga/precip/precip.bin",
    //  "output/precip"
    // );
    // new TempLoad("canonical_coord").load(
    //  "temperature",
    //  "jdbc:virtuoso://localhost:1113",
    //  "/home/ylshen/elinga/best_dataset/best_monthly.bin",
    //  "output/temperature"
    // );
    // new LoadSolar("jdbc:virtuoso://localhost:1113", "/home/ylshen/elinga/globalsolaraltas/solar.bin.gz", "canonical_coord").loadWithReify(
    //         "globalsolaratlas",
    //         "http://globalsolaratlas.info/",
    //         null,
    //         "output/globalsolaraltas"
    // );
    // new LoadToOceanDistance().loadWithReify("natural_earth", "http://www.naturalearthdata.com/", null, "output/to_ocean_distance");
    // new LoadOceanCurrent().loadWithReify("wikipedia-manual", "http://en.wikipedia.org/wiki/Ocean_current",null, "output/ocean_current");
    // new LoadNewClimate().loadWithReify("other-manual", null, null, "output/climate");
    logger.warn("Should load all data into virtuoso here!");

    // new ExtractSlim(mainVirtuosoUri, "sparql/load_slim_nonfunctional.sparql").runmain("slim", "output/release/slim_others_1");
    // new ExtractSlim(mainVirtuosoUri, "sparql/load_slim_functional.sparql", true).runmain("slim", "output/release/slim_others_2");
  }
}
