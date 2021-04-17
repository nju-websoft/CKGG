package loaders;

import namespaces.Namespaces;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import utils.Utils;

import java.io.IOException;

import static namespaces.Namespaces.GKB;

public abstract class GridLoaderBase extends LoaderBase {
  @Override
  public final void doLoad(Model model, String inputPath) {
    throw new NotImplementedException();
  }

  public abstract void doLoad(Model model, String inputPath, String binaryPath) throws IOException;

  @Override
  public final void load(String srcName, String inputPath, String outputPath) {
    throw new NotImplementedException();
  }

  @Override
  public void loadWithReify(String srcName, String uri, String inputPath, String outputPath) throws IOException {
    throw new NotImplementedException();
  }

  public void load(String srcName, String inputPath, String binaryPath, String outputPath) throws IOException {
    Dataset dataset = createDataset();
    Model model = createModel(dataset, srcName);
    doLoad(model, inputPath, binaryPath);
    Utils.write(dataset, GKB + "src/" + srcName, outputPath);
  }

  public void loadWithReify(String srcName, String uri, String inputPath, String binaryPath, String outputPath) throws IOException {
    Dataset dataset = createDataset();
    Model model = createModel(dataset, srcName);
    Model rmodel = createModel(dataset, Namespaces.GKS + "reified");
    doLoad(model, inputPath, binaryPath);
    model.listStatements().forEachRemaining(x -> new Utils.ReifiedStatementBuilder(x, rmodel).setDerivedByLiteral(uri));
    Utils.write(dataset, GKB + "src/" + srcName, outputPath);
    Utils.write(dataset, Namespaces.GKS + "reified", outputPath + "_reified");
  }
}
