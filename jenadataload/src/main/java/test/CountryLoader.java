package test;

import loaders.LoaderBase;
import namespaces.Namespaces;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CountryLoader extends LoaderBase {
  boolean mergeChinese;

  /**
   * 加载国家信息
   * @param mergeChinese 是否合并港澳台
   */
  public CountryLoader(boolean mergeChinese) {
    super();
    this.mergeChinese = mergeChinese;
  }

  public CountryLoader() {
    this(false);
  }

  public Map<String, String> getCountryMap() throws IOException {
    File geonames = new File("/home/ylshen/elinga/geonames/countryInfo.txt");
    Map<String, String> ret = new HashMap<>();

    try(BufferedReader reader =  new BufferedReader(new InputStreamReader(new FileInputStream(geonames), StandardCharsets.UTF_8))) {
      String line;
      // ignore first line
      while ((line = reader.readLine()) != null) {
        if(line.startsWith("#")) continue;
        String[] resLine = line.split("\t");
        ret.put(resLine[0], Namespaces.GEONAMES + resLine[16]);
      }
      if(mergeChinese) {
        ret.put("HK", ret.get("CN"));
        ret.put("TW", ret.get("CN"));
        ret.put("MO", ret.get("CN"));
      }
    }
    return ret;
  }
  @Override
  public void doLoad(Model model, String inputPath) throws IOException {
    File geonames = new File(inputPath);
    try(BufferedReader reader =  new BufferedReader(new InputStreamReader(new FileInputStream(geonames), StandardCharsets.UTF_8))) {
      String line;
      // ignore first line
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("#")) continue;
        String[] resLine = line.split("\t");
        if(mergeChinese && (resLine[0].equals("HK") || resLine[0].equals("TW") || resLine[0].equals("MO"))) {
          continue;
        }
        Resource resource = model.createResource(Namespaces.GEONAMES + resLine[16]);
        resource.addProperty(RDF.type, model.createResource(Namespaces.GKB + "Q763"));
        resource.addProperty(model.createProperty(Namespaces.GKP + "P762"), resLine[0]);
      }
    }
  }

  public static void main(String[] args) throws IOException {
    new CountryLoader().load("geonames", "/home/ylshen/elinga/geonames/countryInfo.txt", "./output/geonames/countries");
  }
}
