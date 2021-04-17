package alignment;

import static namespaces.Namespaces.GKB;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.OWL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;
import utils.Utils;

public class GeonamesSelfAlign {
  private static final Logger logger = LoggerFactory.getLogger(GeonamesSelfAlign.class);

  private Dataset dataset;

  GeonamesSelfAlign() {
    dataset = DatasetFactory.create();
  }

  static class AdmNode {
    String uri;
    String featureCode;
    String engName;
    List<String> names;
    List<String> parents;
    Double lat;
    Double lon;

    public AdmNode(String uri) {
      this.uri = uri;
      names = new ArrayList<>();
      // 国家, 四级行政区
      parents = new ArrayList<>(Arrays.asList(null, null, null, null, null));
    }

    @Override
    public String toString() {
      return "AdmNode{" +
              "uri='" + uri + '\'' +
              ", featureCode='" + featureCode + '\'' +
              ", names=" + names +
              ", parents=" + parents +
              ", lat=" + lat +
              ", lon=" + lon +
              '}';
    }
  }

  private PrintStream histStream;

  public double score(AdmNode node1, AdmNode node2) {
    // 必须不能有矛盾的上层实体
    for(int i = 0; i < 5; i++) {
      if(node1.parents.get(i) == null || node2.parents.get(i) == null) continue;
      if(!node1.parents.get(i).equals(node2.parents.get(i))) return 0;
    }
    // 必须有至少一个单词重合
    Set<String> node1Names = Arrays.stream(node1.engName.split("\\s+")).map(String::toLowerCase).collect(Collectors.toSet());
    Set<String> node2Names = Arrays.stream(node2.engName.split("\\s+")).map(String::toLowerCase).collect(Collectors.toSet());
    if(Sets.intersection(node1Names, node2Names).isEmpty()) {
      return 0;
    }
    // 对P.PPL实体，重名较多，因此使用更严格的筛选
    double c;
    double d = Utils.distance(node1.lat, node2.lat, node1.lon, node2.lon, 0, 0);
    if(node1.featureCode.equals("P.PPL") || node2.featureCode.equals("P.PPL")) {
      histStream.println("0," + d);
      c = 10000;
    } else {
      histStream.println("1," + d);
      c = 70000;
    }
    return d < c ? 1 : 0;
    // 弃用代码，摘自osm
    // if(d > c) return 0;
    // double dprime = 1 - d/c;
    // return 1 / (1 + Math.exp(-12 * dprime + 6));
  }

  Map<String, Set<AdmNode>> geonamesNameMap = new HashMap<>();
  Map<String, AdmNode> geonamesUriMap = new HashMap<>();

  public void getGeonamesCoord(Connection connection) throws SQLException {
    try(Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery("SELECT geonameid, latitude, longitude FROM geoname");
      while(resultSet.next()) {
        String geonameId = resultSet.getString("geonameid");
        double latitude = resultSet.getDouble("latitude");
        double longitude = resultSet.getDouble("longitude");
        String uri = Namespaces.GN + geonameId;
        AdmNode node = geonamesUriMap.computeIfAbsent(uri, AdmNode::new);
        node.uri = uri;
        node.lat = latitude;
        node.lon = longitude;
      }
    }
  }

  public void getGeonames(Connection connection) throws SQLException {
    // NOTE: 之前这儿是加入了wikidata中来的标签来做额外判断，但是修改成新对齐方式后这样做起来很复杂，而且不一定有效，所以现在只用geonames本身的标签
    try(Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery("SELECT geonameid, isolanguage, alternatename FROM alternatename");
      while(resultSet.next()) {
        String geonameId = resultSet.getString("geonameid");
        String isoLanguage = resultSet.getString("isolanguage");
        String alternateName = resultSet.getString("alternatename");
        String uri = Namespaces.GN + geonameId;
        if(!Utils.isZh(isoLanguage, alternateName)) continue;

        AdmNode node = geonamesUriMap.computeIfAbsent(uri, AdmNode::new);
        node.names.add(alternateName);
        
        geonamesNameMap.computeIfAbsent(alternateName, k -> new HashSet<>());
        geonamesNameMap.get(alternateName).add(node);
      }
    }
  }

  public void getGeonamesInfo(Connection connection) throws SQLException {
    try(Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery("SELECT geonameid, name, fclass, fcode, country, admin1, admin2, admin3, admin4 FROM geoname");
      while(resultSet.next()) {
        String geonameId = resultSet.getString("geonameid");
        String name = resultSet.getString("name");
        String type = resultSet.getString("fclass") + "." + resultSet.getString("fcode");
        List<String> parents = new ArrayList<>(Arrays.asList(
          resultSet.getString("country"),
          resultSet.getString("admin1"),
          resultSet.getString("admin2"),
          resultSet.getString("admin3"),
          resultSet.getString("admin4")
        ));
        String uri = Namespaces.GN + geonameId;

        // fully qualified parent name
        for (int i = 1; i < 5; i++) {
          if(parents.get(i - 1) == null) {
            parents.set(i, null);
          }
          if(parents.get(i) != null) {
            parents.set(i, parents.get(i - 1) + "/" + parents.get(i));
          }
        }

        AdmNode node = geonamesUriMap.computeIfAbsent(uri, AdmNode::new);
        node.engName = name;
        node.featureCode = type;
        node.parents = parents;
      }
    }
  }

  /**
   * 准备工作：把geoname和alternatename加入postgres中
   * @throws IOException
   * @see docs/postgres.sql
   */
  public void runmain() throws IOException {
    // 直方图输出位置
    File histFile = new File("geonames_hist.txt");
    histStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(histFile)));

    String url = "jdbc:postgresql://localhost:5433/postgres?user=postgres&password=root";
    try(Connection connection = DriverManager.getConnection(url)) {
      // 获取坐标位置，存入 geonamesUriMap 中
      logger.info("getGeonamesCoord");
      getGeonamesCoord(connection);
      // 获取中文标签信息，存入 geonamesUriMap 和 geonamesNameMap 中
      logger.info("getGeonames");
      getGeonames(connection);
      // 获取行政区，类别，英文名称信息，存入 geonamesUriMap 中
      logger.info("getGeonamesInfo");
      getGeonamesInfo(connection);
    } catch(SQLException e) {
      throw new IOException("", e);
    }
    
    logger.info("example node: {}", geonamesUriMap.get(Namespaces.GN + "1799962"));

    Model model = dataset.getNamedModel(Namespaces.GKS + "geonames");
    // 分块匹配
    logger.info("matching");
    Set<String> stringSet = new HashSet<>();
    geonamesNameMap.forEach((k, v) -> {
      AdmNode[] arr = v.toArray(new AdmNode[0]);
      for (int i = 0; i < arr.length; i++) {
        for(int j = i + 1; j < arr.length; j++) {
          if(score(arr[i], arr[j]) > 0) {
            if(arr[i].uri.compareTo(arr[j].uri) > 0) {
              stringSet.add(arr[j].uri + "\t" + arr[i].uri);
            } else {
              stringSet.add(arr[i].uri + "\t" + arr[j].uri);
            }
          }
        }
      }
    });
    histStream.flush();
    histStream.close();
    stringSet.stream().map(x -> x.split("\t")).forEach(x -> model.add(model.createResource(x[0]), OWL.sameAs, model.createResource(x[1])));
    Utils.write(dataset, Namespaces.GKS + "geonames", "temp_output/new_align/edges/geonames_to_geonames");
  }

  public static void main(String[] args) throws IOException {
    new GeonamesSelfAlign().runmain();
  }
}
