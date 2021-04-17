package release;

import java.io.IOException;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

import namespaces.Namespaces;
import utils.Utils;

public class ReifiedToPlain {
    Model model;
    String datasetPath;

    public ReifiedToPlain(String datasetPath) {
        this.datasetPath = datasetPath;
    }

    /**
     * 
     * @param outputPath 不用加后缀
     * @throws IOException
     */
    public void runmain(String outputPath, String outputGraphName) throws IOException {
        model = RDFDataMgr.loadModel(datasetPath);

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("rdf", RDF.uri);
        pss.setCommandText("CONSTRUCT {?s ?p ?o} WHERE {?n a rdf:Statement; rdf:subject ?s; rdf:predicate ?p; rdf:object ?o.}");
        try(QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(), model)) {
            Model rmodel = qe.execConstruct();
            Utils.write(rmodel, Namespaces.GKS + outputGraphName, outputPath);
        }
    }
}
