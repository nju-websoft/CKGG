package loaders;

import namespaces.Namespaces;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.Utils;

import java.io.IOException;

import static namespaces.Namespaces.GKB;
import static namespaces.Namespaces.GKP;
import static namespaces.Namespaces.GKD;

public abstract class LoaderBase {
  private static Logger logger = LoggerFactory.getLogger(LoaderBase.class);

  public Dataset createDataset() {
    return DatasetFactory.create();
  }
  public Model createModel(Dataset dataset, String srcName) {
    Model model = dataset.getNamedModel(Namespaces.GKS + srcName);
    model.setNsPrefix("gkb", GKB);
    model.setNsPrefix("gkp", GKP);
    model.setNsPrefix("gkd", GKD);
    return model;
  }

  public abstract void doLoad(Model model, String deprecated) throws IOException;

  public void load(String srcName, String deprecated, String outputPath) throws IOException {
    Dataset dataset = createDataset();
    Model model = createModel(dataset, srcName);
    doLoad(model, deprecated);
    Utils.write(dataset, Namespaces.GKS + srcName, outputPath);
  }

  public void loadWithReify(String srcName, String uri, String deprecated, String outputPath) throws IOException {
    Dataset dataset = createDataset();
    Model model = createModel(dataset, srcName);
    Model rmodel = createModel(dataset, "reified");
    doLoad(model, deprecated);
    Utils.convertToReify(model, rmodel, uri);
    logger.info("rmodel size {}", rmodel.size());
    Utils.write(dataset, Namespaces.GKS + srcName, outputPath);
    Utils.write(dataset, Namespaces.GKS + "reified", outputPath + "_reified");
  }
}
