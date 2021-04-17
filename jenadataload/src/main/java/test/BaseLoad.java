package test;

import loaders.LoadAdm;
import namespaces.Namespaces;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import load.AuxLoader;
import utils.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BaseLoad {
  private static final Logger logger = LoggerFactory.getLogger(BaseLoad.class);
  private static final String[] GEONAME_COLS = {"", "P2", "", "", "P21", "P20", "",
          "", "P12", "", "P13", "P14", "P15", "P16", "P177", "", "P22", "P48", ""};

  public static final String SRCURI = Namespaces.GKS + "geonames";
  private AuxLoader auxLoader;

  public void runmain(String inputPath, String outputPath) throws IOException {
    Dataset dataset = DatasetFactory.create();
    Model model = dataset.getNamedModel(SRCURI);
    auxLoader = new AuxLoader("auxiliary.txt");

    Map<String, String> countryMap = new CountryLoader().getCountryMap();
    Map<String, String> admMap = new LoadAdm().getAdmMap(inputPath);

    File geonames = new File(inputPath);
    logger.info("Base Load file {}", geonames);
    try(BufferedReader reader =  new BufferedReader(new InputStreamReader(new FileInputStream(geonames), StandardCharsets.UTF_8))) {
      String line;
      int cnt = 0;
      while ((line = reader.readLine()) != null) {
        processLine(model, countryMap, admMap, line);
        cnt += 1;
      }
      logger.info("Loaded {} lines.", cnt);
      Utils.write(dataset, SRCURI, outputPath);
    }
  }

  private void processLine(Model model, Map<String, String> countryMap, Map<String, String> admMap, String line) throws IOException {
    String[] resLine = line.split("\t");
    Resource resource = model.createResource(Namespaces.GEONAMES + resLine[0]);
    resource.addProperty(RDF.type, model.createResource(Namespaces.GKB + "Q5"));

    // 预处理 加入行政区划信息
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("/").append(resLine[8]);
    for (int i = 1; i <= 4; i++) {
      stringBuilder.append(resLine[9 + i]);
      stringBuilder.append("/");
      String outcode;
      if((outcode = admMap.get(stringBuilder.toString())) != null) {
        resource.addProperty(model.createProperty(Namespaces.GKP + GEONAME_COLS[9 + i]), model.createResource(Namespaces.GEONAMES + outcode));
      }
    }

    if(!resLine[6].trim().isEmpty() || !resLine[7].trim().isEmpty()) {
      resource.addProperty(model.createProperty(Namespaces.GKP + "P8"), resLine[6] + "." + resLine[7]);
    }

    for (int i = 1; i < GEONAME_COLS.length; i++) {
      if(GEONAME_COLS[i].equals("")) continue;
      if(resLine[i].equals("")) continue;
      resource.addProperty(model.createProperty(Namespaces.GKP + "P23"), model.createTypedLiteral(
        String.format("POINT(%s %s)", resLine[5], resLine[4]), Namespaces.GEOSPARQL.wktLiteral
      ));

      if(GEONAME_COLS[i].equals("P48")) {
        resource.addProperty(model.createProperty(Namespaces.GKP + GEONAME_COLS[i]), model.createResource(auxLoader.loadAuxUri(resLine[i])));
      } else if(GEONAME_COLS[i].equals("P12")) {
        if (countryMap.containsKey(resLine[i])) {
          resource.addProperty(model.createProperty(Namespaces.GKP + GEONAME_COLS[i]), model.createResource(countryMap.get(resLine[i])));
        } else {
          logger.warn("国家代码未知 {}", resLine[i]);
        }
      } else if(GEONAME_COLS[i].equals("P13") || GEONAME_COLS[i].equals("P14") || GEONAME_COLS[i].equals("P15") || GEONAME_COLS[i].equals("P16")) {
        // already added
      } else if(GEONAME_COLS[i].equals("P2")){
        resource.addProperty(RDFS.label, model.createLiteral(resLine[i], "en"));
      } else if(GEONAME_COLS[i].equals("P20") || GEONAME_COLS[i].equals("P21") || GEONAME_COLS[i].equals("P22")) {
        if(GEONAME_COLS[i].equals("P20")) {
          resource.addProperty(model.createProperty(Namespaces.WGS84POS + "lon"),
              model.createTypedLiteral(Double.parseDouble(resLine[i])));
        } else if(GEONAME_COLS[i].equals("P21")) {
          resource.addProperty(model.createProperty(Namespaces.WGS84POS + "lat"),
              model.createTypedLiteral(Double.parseDouble(resLine[i])));
        } else {
          resource.addProperty(model.createProperty(
              Namespaces.GKP + GEONAME_COLS[i]),
              model.createTypedLiteral(Double.parseDouble(resLine[i])));
        }
      } else if(GEONAME_COLS[i].equals("P177")) {
        long val = Long.parseLong(resLine[i]);
        if(val <= 0) {
          // manually ignore zero population
          continue;
        }
        resource.addProperty(model.createProperty(Namespaces.GKP + GEONAME_COLS[i]), model.createTypedLiteral(val));
      } else {
        resource.addProperty(model.createProperty(Namespaces.GKP + GEONAME_COLS[i]), resLine[i]);
      }
    }
  }

  public static void main(String[] args) throws IOException {
    new BaseLoad().runmain("/home/ylshen/elinga/geonames/allCountries.txt", "./output/geonames/base_info");
  }
}
