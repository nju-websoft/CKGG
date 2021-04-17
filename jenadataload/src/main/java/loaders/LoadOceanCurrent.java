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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import load.AuxLoader;
import namespaces.Namespaces;

public class LoadOceanCurrent extends LoaderBase {
  private static final Logger logger = LoggerFactory.getLogger(LoadOceanCurrent.class);

  public void loadCurrents(Model model, Connection connection) throws SQLException, IOException {
    AuxLoader auxLoader = new AuxLoader("auxiliary.txt");

    try(Statement stmt = connection.createStatement()) {
      ResultSet result = stmt.executeQuery("SELECT name, ST_ASTEXT(ST_UNION(ST_TRANSFORM(wkb_geometry, 4326))) AS line FROM export_output GROUP BY name");
      int cnt = 0;
      while(result.next()) {
        cnt++;
        String name = result.getString("name");
        String type;
        if(name.endsWith("暖流")) {
          type = "Q1102";
        } else if(name.endsWith("寒流")) {
          type = "Q1101";
        } else if(name.equals("赤道逆流")) {
          type = "Q1102";
        } else if(name.equals("南极环流") || name.equals("西风漂流")) {
          type = "Q1101";
        } else {
          throw new RuntimeException("Unknown type for " + name);
        }
        Resource resource = model.createResource(auxLoader.loadAuxUri(name));
        resource.addProperty(RDFS.label, model.createLiteral(name, "zh-cn"));
        resource.addProperty(RDF.type, model.createResource(Namespaces.GKB + type));
        resource.addProperty(RDF.type, model.createResource(Namespaces.GKB + "Q703"));
        resource.addProperty(model.createProperty(Namespaces.GKP + "P705"), model.createTypedLiteral(result.getString("line"), Namespaces.GEOSPARQL.wktLiteral));
      }
      logger.info("loaded {} types of ocean flow", cnt);
    }
  }

  public void loadRelations(Model model, Connection connection) throws SQLException, IOException {
    AuxLoader auxLoader = new AuxLoader("auxiliary.txt");

    try(Statement stmt = connection.createStatement()) {
      ResultSet result = stmt.executeQuery("SELECT id, ocean_flow_name FROM all_place_ocean_flows_v3");
      while(result.next()) {
        String id = result.getString("id");
        String oceanFlowName = result.getString("ocean_flow_name");
        model.add(
                model.createResource(Namespaces.GKD + id),
                model.createProperty(Namespaces.GKP + "P31"),
                model.createResource(auxLoader.loadAuxUri(oceanFlowName))
        );
      }
    }
  }

  @Override
  public void doLoad(Model model, String inputPath) throws IOException {
    String url = "jdbc:postgresql://localhost:5433/postgres?user=postgres&password=root";
    try(Connection connection = DriverManager.getConnection(url)) {
      loadCurrents(model, connection);
      loadRelations(model, connection);
    } catch(SQLException e) {
      throw new IOException("", e);
    }
  }
}
