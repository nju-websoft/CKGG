package test;

import loaders.LoaderBase;
import namespaces.Namespaces;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import load.AuxLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static namespaces.Namespaces.GKD;
import static namespaces.Namespaces.GKP;

public class TzLoader extends LoaderBase {
  @Override
  public void doLoad(Model model, String inputPath) throws IOException {
    AuxLoader auxLoader = new AuxLoader("auxiliary.txt");
    File geonames = new File(inputPath);
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(geonames), StandardCharsets.UTF_8))) {
      String line;
      // ignore first line
      line = reader.readLine();
      while ((line = reader.readLine()) != null) {
        String[] resLine = line.split("\t");
        Resource resource = model.createResource(auxLoader.loadAuxUri(resLine[1]));
        resource.addProperty(RDFS.label, model.createLiteral(resLine[1]));
        resource.addProperty(RDF.type, model.createResource(Namespaces.GKB + "Q48"));
        resource.addProperty(model.createProperty(GKP + "P1623"), model.createTypedLiteral(Double.parseDouble(resLine[2])));
      }
    }
  }

  public static void main(String[] args) throws IOException {
    new TzLoader().load("geonames", "/home/ylshen/elinga/geonames/timeZones.txt", "output/geonames/timezones");
  }
}

