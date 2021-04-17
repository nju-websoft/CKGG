package loaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.hankcs.hanlp.HanLP;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;
import utils.Utils;

/** 从geonames中读取别名 */
public class AlternateNameLoad extends LoaderBase {
  private static final Logger logger = LoggerFactory.getLogger(AlternateNameLoad.class);

  @Override
  public void doLoad(Model model, String inputPath) throws IOException {
    logger.info("loading altername from {}", inputPath);
    try(BufferedReader reader = new BufferedReader(new FileReader(new File(inputPath)))) {
      reader.lines().map(x -> x.split("\t")).filter(x -> Utils.isZh(x[2], x[3])).forEach(x -> {
        Resource resource = model.createResource(Namespaces.GEONAMES + x[1]);
        resource.addLiteral(RDFS.label, model.createLiteral(HanLP.t2s(x[3]), x[2]));
      });
    }
  }

  public static void main(String[] args) throws IOException {
    new AlternateNameLoad().load("geonames", "/home/ylshen/elinga/geonames/alternateNamesV2.txt", "output/geonames/labels");
  }
}
