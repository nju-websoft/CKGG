package namespaces;

public class Namespaces {
  public static final String GKS = "http://tempuri/src/";
  public static final String GKB = "http://w3id.org/ckgg/1.0/ontology/classes/";
  public static final String GKP = "http://w3id.org/ckgg/1.0/ontology/properties/";
  public static final String GKD = "http://w3id.org/ckgg/1.0/instances/location/";
  public static final String GKDA = "http://w3id.org/ckgg/1.0/instances/auxiliary/";
  public static final String WGS84POS = "http://www.w3.org/2003/01/geo/wgs84_pos#";

  // prov-o namespaces
  public static final class PROV {
    public static final String PREFIX = "http://www.w3.org/ns/prov#";
    public static final String wasDerivedFrom = PREFIX + "wasDerivedFrom";
  }

  // Wikidata namespaces
  public static final String WD = "http://www.wikidata.org/entity/";
  public static final String WDT = "http://www.wikidata.org/prop/direct/";

  // DBpeda namespaces
  public static final String DBR = "http://dbpedia.org/resource/";

  // Geonames namespaces
  public static final String GN = "http://sws.geonames.org/";
  public static final String GEONAMES = GN;

  // Geosparql
  public static final class GEOSPARQL {
    public static final String PREFIX = "http://www.opengis.net/ont/geosparql#";
    public static final String wktLiteral = PREFIX + "wktLiteral";
  }

  private Namespaces(){}
}
