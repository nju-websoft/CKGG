package loaders;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import load.AuxLoader;
import namespaces.Namespaces;

public class LoadNewClimate extends LoaderBase {
  private static final Logger logger = LoggerFactory.getLogger(LoadNewClimate.class);

  public void loadCurrents(Model model, Connection connection) throws SQLException, IOException {
    AuxLoader auxLoader = new AuxLoader("auxiliary.txt");

    try(Statement stmt = connection.createStatement()) {
      ResultSet result = stmt.executeQuery("SELECT name, MIN(description) as description, st_astext(st_union(wkb_geometry)) AS wkt FROM new_climate_mapping inner join new_climate on new_climate_mapping.id=new_climate.id GROUP BY name");
      while(result.next()) {
        String name = result.getString("name");
        String description = result.getString("description");
        String polygon = result.getString("wkt");
        Resource resource = model.createResource(auxLoader.loadAuxUri(name));
        resource.addProperty(RDFS.label, model.createLiteral(name, "zh-cn"));
        resource.addProperty(SKOS.definition, model.createLiteral(description, "zh-cn"));
        resource.addProperty(RDF.type, model.createResource(Namespaces.GKB + "Q86"));
        resource.addProperty(model.createProperty(Namespaces.GKP + "P1228"), model.createTypedLiteral(
            polygon, Namespaces.GEOSPARQL.wktLiteral));
      }
    }
  }

  public void loadRelations(Model model, Connection connection) throws SQLException, IOException {
    AuxLoader auxLoader = new AuxLoader("auxiliary.txt");

    try(Statement stmt = connection.createStatement()) {
      ResultSet result = stmt.executeQuery("SELECT id, climate_name FROM all_place_climate_v3");
      while(result.next()) {
        String id = result.getString("id");
        String climateName = result.getString("climate_name");
        model.add(
                model.createResource(Namespaces.GKD + id),
                model.createProperty(Namespaces.GKP + "P86"),
                model.createResource(auxLoader.loadAuxUri(climateName))
        );
      }
    }
  }

  @Override
  public void doLoad(Model model, String inputPath) throws IOException {
    logger.info("Loading new climate...");
    String url = "jdbc:postgresql://localhost:5433/postgres?user=postgres&password=root";
    try(Connection connection = DriverManager.getConnection(url)) {
      loadCurrents(model, connection);
      loadRelations(model, connection);
    } catch(SQLException e) {
      throw new IOException("", e);
    }
  }
}
