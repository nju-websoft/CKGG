package loaders;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.jena.rdf.model.Model;

import namespaces.Namespaces;

public class LoadNaturalEarth extends LoaderBase {
  @Override
  public void doLoad(Model model, String inputPath) throws IOException {
    try(Connection connection = DriverManager.getConnection(inputPath)) {
      try(Statement stmt = connection.createStatement()) {
        ResultSet result = stmt.executeQuery("SELECT wikidataid, ST_ASTEXT(geom) AS geom FROM all_wikidata_regions_v2");
        while(result.next()) {
          String wikidataUrl = Namespaces.WD + result.getString("wikidataid");
          String geom = result.getString("geom");
          model.add(
            model.createResource(wikidataUrl),
            model.createProperty(Namespaces.GKP + "P24"),
            model.createTypedLiteral(geom, Namespaces.GEOSPARQL.wktLiteral)
          );
        }
      }
    } catch(SQLException e) {
      throw new IOException("", e);
    }
  }
}
