package loaders;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import namespaces.Namespaces;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static namespaces.Namespaces.GKP;

/**
 * 加载行政区信息
 */
public class LoadAdm extends LoaderBase {

  public Map<String, String> getAdmMap(String inputPath) throws IOException {
    Map<String, String> ret = new HashMap<>();
    File geonames = new File(inputPath);
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(geonames), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] resLine = line.split("\t");
        // feat code
        if(resLine[7].matches("^ADM[1-4]$")) {
          int admLevel = resLine[7].charAt(3) - '0';
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append("/").append(resLine[8]);
          for (int i = 1; i <= admLevel; i++) {
            stringBuilder.append(resLine[9 + i]);
            stringBuilder.append("/");
          }
          ret.put(stringBuilder.toString(), resLine[0]);
        }
      }
    }
    return ret;
  }

  @Override
  public void doLoad(Model model, String inputPath) throws IOException {
    File geonames = new File(inputPath);
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(geonames), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] resLine = line.split("\t");
        // feat code
        if(resLine[7].matches("^ADM[1-4]$")) {
          Resource resource = model.createResource(Namespaces.GEONAMES + resLine[0]);
          int admLevel = resLine[7].charAt(3) - '0';
          resource.addProperty(model.createProperty(GKP + "P762"), model.createLiteral(resLine[9 + admLevel]));
        }
      }
    }
  }

  public static void main(String[] args) throws IOException {
    new LoadAdm().load("geonames", "/home/ylshen/elinga/geonames/allCountries.txt", "./output/geonames/admcodes");
  }
}
