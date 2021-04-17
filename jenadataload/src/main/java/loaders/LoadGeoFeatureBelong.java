package loaders;

import namespaces.Namespaces;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;

public class LoadGeoFeatureBelong extends LoaderBase {
  private static final Logger logger = LoggerFactory.getLogger(LoadGeoFeatureBelong.class);

  private String dbUri;
  private String tableName;

  public LoadGeoFeatureBelong(String dbUri, String tableName) {
    this.dbUri = dbUri;
    this.tableName = tableName;
  }

  public void addEdge(Model model, String cid, String pid) {
    model.add(model.createResource(Namespaces.GKD + cid), model.createProperty(Namespaces.GKP + "P10"), model.createResource(Namespaces.GKD + pid));
    model.add(model.createResource(Namespaces.GKD + pid), model.createProperty(Namespaces.GKP + "P18"), model.createResource(Namespaces.GKD + cid));
  }

  @Override
  public void doLoad(Model model, String inputPath) throws IOException {
    if(inputPath != null) {
      throw new IllegalArgumentException("input path has no usage");
    }
    logger.info("loading geofeature v2...");
    String url = dbUri;
    try(Connection connection = DriverManager.getConnection(url)) {
      try(Statement stmt = connection.createStatement()) {
        ResultSet result = stmt.executeQuery("SELECT from_id, to_id FROM " + tableName);
        while(result.next()) {
          String fromId = result.getString("from_id");
          String toId = result.getString("to_id");
          addEdge(model, fromId, toId);
        }
      }
    } catch(SQLException e) {
      throw new IOException("", e);
    }
  }
}
