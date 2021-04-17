package release;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;

import namespaces.Namespaces;
import utils.Utils;

public class PlainToReified {
    Model model;
    String datasetPath;
    String reifyUri;

    public PlainToReified(String datasetPath, String reifyUri) {
        this.datasetPath = datasetPath;
        this.reifyUri = reifyUri;
    }

    /**
     * 
     * @param outputPath 不用加后缀
     * @throws IOException
     */
    public void runmain(String outputPath, String outputGraphName) throws IOException {
        model = RDFDataMgr.loadModel(datasetPath);
        Model rmodel = ModelFactory.createDefaultModel();
        Utils.convertToReify(model, rmodel, reifyUri);
        Utils.write(rmodel, Namespaces.GKS + outputGraphName, outputPath);
    }
}
