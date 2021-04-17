package test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Files;
import com.google.gson.Gson;

import alignment.AddStatCode;
import alignment.GeonamesSelfAlign;
import alignment.wdgn.GatherEntity;
import alignment.wdgn.WikidataToGeonamesAlign;
import belong.LoadGeoFeatureBelongByAdmin;
import loadcsv.ExportUnBelongToCsv;
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
import expr.GetExpr2Entities;
import release.LoadBySparql;
import release.LoadBySparqlWithDedup;
import release.PlainToReified;
import release.ReifiedToPlain;
import utils.Utils;

public class NewMain {
  private static final Logger logger = LoggerFactory.getLogger(NewMain.class);
  private static final String mainVirtuosoUri = "jdbc:virtuoso://localhost:1113";
  private static final String postgisUri = "jdbc:postgresql://localhost:5433/postgres?user=postgres&password=root";
  private static final String wikidataVirtuosoUri = "jdbc:virtuoso://localhost:1112";

  private static void processOntology() throws IOException {
    logger.info("Processing ontology");
    new File("output/ontology").mkdirs();
    Files.asCharSink(new File("output/ontology/ontology.owl.graph"), StandardCharsets.UTF_8).write(Namespaces.GKS + "ontology_manual");
    Files.asCharSink(new File("output/ontology/mapping.owl.graph"), StandardCharsets.UTF_8).write(Namespaces.GKS + "ontology_manual");
    Files.copy(new File("../release/ontology.owl"), new File("output/ontology/ontology.owl"));
    Files.copy(new File("../release/mapping.owl"), new File("output/ontology/mapping.owl"));
  }

  public static void main(String[] args) throws IOException {
    // // step 1, process ontology
    // processOntology();

    // // step 2, gather entity and align
    // GatherEntity.main(args);
    // WikidataToGeonamesAlign.main(args);
    // GeonamesSelfAlign.main(args);
    // new LoadNodes("temp_output/new_align").loadWithReify("new_align", null, null, "output/new_align");

    // step 3, load wikidata, geonames and base natural earth data
    // new LoadWikidataTriples().load("wikidata_triples", wikidataVirtuosoUri, "output/wikidata/wikidata");
    // new LoadNaturalEarth().load("natural_earth_base", postgisUri, "output/wikidata/natural_earth");
    // new LoadGeonamesTriples().runmain("output/geonames");
    // new ConvertData().runmain("output/new_align.nt.gz");
    // logger.warn("Should reload here because of changed entity id!");

    // step 4, add government data
    // new AddStatCode().runmain(mainVirtuosoUri, "/home/ylshen/elinga/province-city-china/mydata/processed.jsonl", "2834351", "output/province_city_china_align");
    // new CnStatLoad("output/province_city_china_align.nt.gz", "/home/ylshen/elinga/cnstat/selected_datas.json").loadWithReify(
    //   "cn_stat_data", "http://www.stats.gov.cn/", null, "output/cn_stat_data");
    // TODO add stat data

    // step 5, add canonical coord and export to csv, to be added to postgis
    // new ExtractCanonicalCoord(mainVirtuosoUri).load("canonical_coord", null, "output/canonical_coord");
    // logger.warn("Should load canonical coord here!");
    // new ExportUnBelongToCsv(mainVirtuosoUri).runmain("/home/ylshen/elinga/ne_dataset/postgis_ne/import/unbelong_id.csv");
    // new ExportCanonicalCoordToCsv("canonical_coord", "natural_earth").runmain(
    //    mainVirtuosoUri,
    //    "/home/ylshen/elinga/ne_dataset/postgis_ne/import/new_coord.csv",
    //    "/home/ylshen/elinga/ne_dataset/postgis_ne/import/new_region.csv"
    // );
    // logger.warn("Should reload db and run sql in docs/postgres.sql here!");

    // step 6, load belongs
    // new LoadGeoFeatureBelong(postgisUri, "belongs_v4")
    //     .loadWithReify("feature_belong", "http://www.naturalearthdata.com/", null, "./output/feature_belong");
    // new LoadGeoFeatureBelongByAdmin(mainVirtuosoUri, "output/belong_by_admin").runmain();

    // // step 7, load grid data
    // new PrecipLoad("canonical_coord").load(
    //  "precip",
    //  mainVirtuosoUri,
    //  "/home/ylshen/elinga/precip/precip.bin",
    //  "output/precip"
    // );
    // new TempLoad("canonical_coord").load(
    //  "temperature",
    //  mainVirtuosoUri,
    //  "/home/ylshen/elinga/best_dataset/best_monthly.bin",
    //  "output/temperature"
    // );
    // new LoadSolar(mainVirtuosoUri, "/home/ylshen/elinga/globalsolaraltas/solar.bin.gz", "canonical_coord").loadWithReify(
    //         "globalsolaratlas",
    //         "http://globalsolaratlas.info/",
    //         null,
    //         "output/globalsolaraltas"
    // );

    // step 8, load gis data
    // new LoadToOceanDistance().loadWithReify("ocean_distance", "http://www.naturalearthdata.com/", null, "output/to_ocean_distance");
    // new LoadOceanCurrent().loadWithReify("ocean_current", "http://en.wikipedia.org/wiki/Ocean_current", null, "output/ocean_current");
    // new LoadNewClimate().loadWithReify("new_climate", null, null, "output/climate");
    // logger.warn("Should load all data into virtuoso here!");

    // step 9, load application version
    // new LoadBySparql(mainVirtuosoUri, "sparql/coord_to_slim.sparql").runmain("output/release_slim/coord.nt.gz");
    // new LoadBySparql(mainVirtuosoUri, "sparql/pop_to_slim.sparql").runmain("output/release_slim/pop.nt.gz");
    // new LoadBySparql(mainVirtuosoUri, "sparql/gis_to_slim.sparql").runmain("output/release_slim/gis.nt.gz");
    // new LoadBySparql(mainVirtuosoUri, "sparql/altitude_to_slim.sparql").runmain("output/release_slim/altitude.nt.gz");
    // new LoadBySparql(mainVirtuosoUri, "sparql/type_to_slim.sparql").runmain("output/release_slim/type.nt.gz");
    // new LoadBySparql(mainVirtuosoUri, "sparql/tz_to_slim.sparql").runmain("output/release_slim/tz.nt.gz");
    // new LoadBySparql(mainVirtuosoUri, "sparql/tz_loc_to_slim.sparql").runmain("output/release_slim/tz_loc.nt.gz");
    // new LoadBySparql(mainVirtuosoUri, "sparql/label_to_slim.sparql").runmain("output/release_slim/label.nt.gz");
    // new LoadBySparqlWithDedup(mainVirtuosoUri, "sparql/admin_to_slim.sparql").runmain("output/release_slim/admin.nt.gz");
    // new LoadBySparql(mainVirtuosoUri, "sparql/admin_code_to_slim.sparql").runmain("output/release_slim/admin_code.nt.gz");
    // new LoadBySparql(mainVirtuosoUri, "sparql/geonames_code_to_slim.sparql").runmain("output/release_slim/geonames_code.nt.gz");
    // new ReifiedToPlain("../other_data/wiki_links.nt.gz").runmain("output/wiki_links", "wiki_links");
    // new PlainToReified("../other_data/entity_rank_score.nt.gz", null).runmain("output/entity_rank_score_reified", "entity_rank_score_reified");

    // step 10, load size data
    // new GetExpr2Entities(mainVirtuosoUri, "sparql/get_entities_with_component_size.sparql", 2, 2, 100).runmain("expr_output/component_2.txt");
    // new GetExpr2Entities(mainVirtuosoUri, "sparql/get_entities_with_component_size.sparql", 3, 3, 100).runmain("expr_output/component_3.txt");
    // new GetExpr2Entities(mainVirtuosoUri, "sparql/get_entities_with_component_size.sparql", 4, 1000000000, 100).runmain("expr_output/component_4_more.txt");
  }
}
