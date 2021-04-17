package loaders;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;

public class LoadToOceanDistance extends LoaderBase {
  private static final Logger logger = LoggerFactory.getLogger(LoadToOceanDistance.class);
  public void loadRelations(Model model, Connection connection) throws SQLException {
    try(Statement stmt = connection.createStatement()) {
      ResultSet result = stmt.executeQuery("SELECT id, distance FROM place_to_ocean_distance_v3");
      int cnt = 0;
      while(result.next()) {
        cnt++;
        String id = result.getString("id");
        double distance = result.getDouble("distance");
        model.add(
                model.createResource(Namespaces.GKD + id),
                model.createProperty(Namespaces.GKP + "P26"),
                model.createTypedLiteral(distance)
        );
      }
      logger.info("Loaded {} lines.\n", cnt);
    }
  }

  @Override
  public void doLoad(Model model, String inputPath) throws IOException {
    logger.info("Loading to ocean distance...");
    String url = "jdbc:postgresql://localhost:5433/postgres?user=postgres&password=root";
    try(Connection connection = DriverManager.getConnection(url)) {
      loadRelations(model, connection);
    } catch(SQLException e) {
      throw new IOException("", e);
    }
  }
}
