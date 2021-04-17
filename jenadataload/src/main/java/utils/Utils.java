package utils;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import namespaces.Namespaces;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class Utils {
  private static final Logger logger = LoggerFactory.getLogger(Utils.class);

  public static String getId(String s, String seperator) {
    String[] sa = s.split(seperator);
    return sa[sa.length - 1];
  }

  public static int gridQuery(double lon, double lat, int scale, int[] grid) {
    int lonp = (int)Math.floor((lon + 180) * scale) % (360 * scale);
    int latp = (int)Math.floor((lat + 90) * scale) % (180 * scale);
    int p = lonp * 180 * scale + latp;
    if(grid[p] != -1) return grid[p];
    Map<Integer, Integer> map = new HashMap<>();
    for (int i = -1; i <= 1; i++) {
      for (int j = -1; j <= 1; j++) {
        int nlonp = (lonp + i + 360 * scale) % (360 * scale);
        int nlatp = (latp + j + 180 * scale) % (180 * scale);
        int np = nlonp * 180 * scale + nlatp;
        if(grid[np] == -1) continue;
        map.put(grid[np], map.getOrDefault(grid[np], 0) + 1);
      }
    }
    return map.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).map(Map.Entry::getKey).orElse(-1);
  }

  public static void main(String[] args) throws IOException {
    byte[] bytes = ByteStreams.toByteArray(new BufferedInputStream(new FileInputStream("D:\\elinga\\koppen\\koppen.bin")));
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    int[] arr = new int[byteBuffer.asIntBuffer().remaining()];
    byteBuffer.asIntBuffer().get(arr);
    logger.info("{}", gridQuery(114.175, 22.2783, 2, arr));
  }

  /**
   * Calculate distance between two points in latitude and longitude taking
   * into account height difference. If you are not interested in height
   * difference pass 0.0. Uses Haversine method as its base.
   *
   * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
   * el2 End altitude in meters
   * @returns Distance in Meters
   */
  public static double distance(double lat1, double lat2, double lon1,
                                double lon2, double el1, double el2) {

    final int R = 6371; // Radius of the earth

    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double distance = R * c * 1000; // convert to meters

    double height = el1 - el2;

    distance = Math.pow(distance, 2) + Math.pow(height, 2);

    return Math.sqrt(distance);
  }

  public static boolean isZh(String langCode, String content) {
    content = content.replaceAll("\\s+", " ");
    if(langCode == null || langCode.equals("")) {
      // 规则1：语言为空，全为汉字
      return content.matches("^[\\u4e00-\\u9fa5]+$");
    } else if (langCode.startsWith("zh")) {
      // 规则2：语言为中文，至少有一个汉字
      return content.matches(".*[\\u4e00-\\u9fa5].*");
    } else {
      return false;
    }
  }

  public static String removeTrailingSlash(String input) {
    return input.replaceAll("/+$", "");
  }

  public static void write(Dataset dataset, String srcUri, String outputPath) throws IOException {
    write(dataset, srcUri, outputPath, ".nt", RDFFormat.NT);
  }

  public static void write(Model model, String srcUri, String outputPath) throws IOException {
    write(model, srcUri, outputPath, ".nt", RDFFormat.NT);
  }

  public static void write(Dataset dataset, String srcUri, String outputPath, String suffix, RDFFormat format) throws IOException {
    write(dataset.getNamedModel(srcUri), srcUri, outputPath, suffix, format);
  }

  public static void write(Model model, String srcUri, String outputPath, String suffix, RDFFormat format) throws IOException {
    logger.info("Writing dataset <{}> to {}", srcUri, outputPath);
    File frag = new File(outputPath + suffix + ".gz");
    frag.getParentFile().mkdirs();
    try (OutputStream stream = new GZIPOutputStream(new FileOutputStream(frag))) {
      RDFDataMgr.write(stream, model, format);
      stream.flush();
    }
    Files.asCharSink(new File(outputPath + suffix + ".graph"), StandardCharsets.UTF_8).write(srcUri);
    logger.info("File Wrote");
  }

  /**
   * @deprecated
   */
  @Deprecated
  public static Set<String> getOnlyZhMap() {
    Set<String> uriset = new HashSet<>();
    Model model = RDFDataMgr.loadModel("./output/geonames/alternate_name_gn.nt.gz");
    for (ResIterator it = model.listSubjects(); it.hasNext(); ) {
      Resource r = it.next();
      uriset.add(r.getURI());
    }
    model = RDFDataMgr.loadModel("./output/geonames/alternate_name_wd.nt.gz");
    for (ResIterator it = model.listSubjects(); it.hasNext(); ) {
      Resource r = it.next();
      uriset.add(r.getURI());
    }
    return uriset;
  }

  public static class ReifiedStatementBuilder {
    private ReifiedStatement builtStatement;
    public ReifiedStatementBuilder(Statement statement, Model model) {
      this.builtStatement = model.createReifiedStatement(
              model.createStatement(
                      statement.getSubject().inModel(model),
                      statement.getPredicate().inModel(model),
                      statement.getObject().inModel(model)
              )
      );
    }
    public ReifiedStatementBuilder setDerivedBy(String uri) {
      if(uri == null) return this;
      this.builtStatement.addProperty(
              this.builtStatement.getModel().createProperty(Namespaces.PROV.wasDerivedFrom),
              this.builtStatement.getModel().createResource(uri)
      );
      return this;
    }
    public ReifiedStatementBuilder setDerivedByLiteral(String uri) {
      if(uri == null) return this;
      this.builtStatement.addProperty(
              this.builtStatement.getModel().createProperty(Namespaces.PROV.wasDerivedFrom),
              this.builtStatement.getModel().createTypedLiteral(uri, XSDDatatype.XSDanyURI)
      );
      return this;
    }
    public ReifiedStatement getBuiltStatement() {
      return builtStatement;
    }
  }

  public static void convertToReify(Model model, Model rmodel, String uri) {
    model.listStatements().forEachRemaining(x -> new Utils.ReifiedStatementBuilder(x, rmodel).setDerivedByLiteral(uri));
  }
}
