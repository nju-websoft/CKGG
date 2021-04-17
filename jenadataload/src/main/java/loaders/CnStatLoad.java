package loaders;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.jena.ext.com.google.common.io.Files;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;

public class CnStatLoad extends LoaderBase {
    private static final Gson gson = new Gson();
    private static final Logger logger = LoggerFactory.getLogger(CnStatLoad.class);

    private static class Data {

        private static class Node {
            public Double data;
            public String reg;
            public String year;
        }

        String pred;
        List<Node> data;
    }

    String cnStat;
    String statsJson;

    public CnStatLoad(String cnStat, String statsJson) {
        logger.info("cnstatload {} {}", cnStat, statsJson);
        this.cnStat = cnStat;
        this.statsJson = statsJson;
    }
    
    @Override
    public void doLoad(Model model, String deprecated) throws IOException {
        Model cnStatModel = RDFDataMgr.loadModel(cnStat);

        List<Data> data = gson.fromJson(Files.asCharSource(new File(statsJson), StandardCharsets.UTF_8).read(), new TypeToken<List<Data>>(){}.getType());
        for(Data singleData: data) {
            for(Data.Node singleNode: singleData.data) {
                String reg = singleNode.reg;
                while(reg.length() != 12) {
                    reg += '0';
                }
                Query query = QueryFactory.create("SELECT ?s WHERE {?s <http://w3id.org/ckgg/1.0/ontology/properties/P762> \"" + reg + "\"}");
                try (QueryExecution qexec = QueryExecutionFactory.create(query, cnStatModel)) {
                    ResultSet results = qexec.execSelect();
                    while (results.hasNext()) {
                        QuerySolution soln = results.nextSolution();
                        Resource r = soln.getResource("s").inModel(model);
                        if(singleNode.data == null) {
                            throw new IllegalArgumentException();
                        }
                        r.addProperty(model.createProperty(Namespaces.GKP 
                                + singleData.pred), model.createTypedLiteral(singleNode.data));
                    }
                }
            }
        }
    }
}
