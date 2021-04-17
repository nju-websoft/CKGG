package prealign;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.google.common.io.Files;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;
import utils.Utils;

/**
 * 把其他格式的数据转化到新的namespace
 */
public class ConvertData {
  private static final Logger logger = LoggerFactory.getLogger(ConvertData.class);

  private Map<String, String> mapToNewNs;
  private Dataset dataset = DatasetFactory.create();

  class StreamConverter implements StreamRDF {
    StreamRDF writer;
    public StreamConverter(StreamRDF writer) {
      this.writer = writer;
    }

    @Override
    public void start() {
      writer.start();
    }

    @Override
    public void triple(Triple triple) {
      // 去除掉数据中的尾斜杠
      String subjStr = Utils.removeTrailingSlash(triple.getSubject().getURI());
      if(subjStr.contains(Namespaces.GKD)) {
        throw new RuntimeException();
      }
      if(!subjStr.contains(Namespaces.GKDA)) {
        subjStr = mapToNewNs.get(subjStr);
      }
      if(subjStr == null) return;

      if(triple.getObject().isLiteral()) {
        writer.triple(new Triple(NodeFactory.createURI(subjStr), triple.getPredicate(), triple.getObject()));
      } else {
        String objStr = Utils.removeTrailingSlash(triple.getObject().getURI());
        if(objStr.contains(Namespaces.GKD)) {
          throw new RuntimeException();
        }
        if(objStr.contains(Namespaces.GEONAMES)) {
          objStr = mapToNewNs.get(objStr);
        }
        if(objStr == null) return;
        writer.triple(new Triple(NodeFactory.createURI(subjStr), triple.getPredicate(), NodeFactory.createURI(objStr)));
      }
    }

    @Override
    public void quad(Quad quad) {
      // do nothing
    }

    @Override
    public void base(String base) {
      // do nothing
    }

    @Override
    public void prefix(String prefix, String iri) {
      // do nothing
    }

    @Override
    public void finish() {
      writer.finish();
    }
  }

  class StreamConverterReified implements StreamRDF {
    StreamRDF writer;
    public StreamConverterReified(StreamRDF writer) {
      this.writer = writer;
    }

    @Override
    public void start() {
      writer.start();
    }

    @Override
    public void triple(Triple triple) {
      // 去除掉数据中的尾斜杠
      String subjStr = Utils.removeTrailingSlash(triple.getSubject().getURI());
      if(subjStr.contains(Namespaces.GKD)) {
        throw new RuntimeException();
      }
      if(!subjStr.contains(Namespaces.GKDA)) {
        subjStr = mapToNewNs.get(subjStr);
      }
      if(subjStr == null) return;

      if(triple.getObject().isLiteral()) {
        Node stmt = NodeFactory.createBlankNode();
        writer.triple(new Triple(stmt, RDF.type.asNode(), RDF.Statement.asNode()));
        writer.triple(new Triple(stmt, RDF.subject.asNode(), NodeFactory.createURI(subjStr)));
        writer.triple(new Triple(stmt, RDF.predicate.asNode(), triple.getPredicate()));
        writer.triple(new Triple(stmt, RDF.object.asNode(), triple.getObject()));
        writer.triple(new Triple(stmt, NodeFactory.createURI(Namespaces.PROV.wasDerivedFrom),
                triple.getSubject().getURI().contains(Namespaces.GKDA) ?
                NodeFactory.createLiteral("http://sws.geonames.org/", XSDDatatype.XSDanyURI) :
                NodeFactory.createURI(triple.getSubject().getURI())));
      } else {
        String objStr = Utils.removeTrailingSlash(triple.getObject().getURI());
        if(objStr.contains(Namespaces.GKD)) {
          throw new RuntimeException();
        }
        if(objStr.contains(Namespaces.GEONAMES)) {
          objStr = mapToNewNs.get(objStr);
        }
        if(objStr == null) return;
        Node stmt = NodeFactory.createBlankNode();
        writer.triple(new Triple(stmt, RDF.type.asNode(), RDF.Statement.asNode()));
        writer.triple(new Triple(stmt, RDF.subject.asNode(), NodeFactory.createURI(subjStr)));
        writer.triple(new Triple(stmt, RDF.predicate.asNode(), triple.getPredicate()));
        writer.triple(new Triple(stmt, RDF.object.asNode(), NodeFactory.createURI(objStr)));
        writer.triple(new Triple(stmt, NodeFactory.createURI(Namespaces.PROV.wasDerivedFrom),
                triple.getSubject().getURI().contains(Namespaces.GKDA) ?
                        NodeFactory.createLiteral("http://sws.geonames.org/", XSDDatatype.XSDanyURI) :
                        NodeFactory.createURI(triple.getSubject().getURI())));
      }
    }

    @Override
    public void quad(Quad quad) {
      // do nothing
    }

    @Override
    public void base(String base) {
      // do nothing
    }

    @Override
    public void prefix(String prefix, String iri) {
      // do nothing
    }

    @Override
    public void finish() {
      writer.finish();
    }
  }

  private void loadMapToNewNs(String nodeMapPath) {
    mapToNewNs = new HashMap<>();
    Dataset aligns = RDFDataMgr.loadDataset(nodeMapPath);
    logger.info("Loading {} statements", aligns.getDefaultModel().size());
    aligns.getDefaultModel().listStatements().forEachRemaining(x -> {
      mapToNewNs.put(x.getObject().asResource().getURI(), x.getSubject().getURI());
    });
  }

  private void loadDbpedia() {
    logger.info("load dbpedia");
    Dataset dbpedia = RDFDataMgr.loadDataset("dbpedia/dbpedia_data_original.nt");
    logger.info("Loading {} statements", dbpedia.getDefaultModel().size());
    Model model = dataset.getNamedModel(Namespaces.GKS + "dbpedia");
    Model rmodel = dataset.getNamedModel(Namespaces.GKS + "reified");
    dbpedia.getDefaultModel().listStatements().forEachRemaining(x -> {
      String subjStr = mapToNewNs.get(x.getSubject().getURI());
      if (subjStr == null)
        return;
      Resource subj = model.createResource(subjStr);

      if (x.getObject().isLiteral()) {
        Statement statement = model.createStatement(subj, x.getPredicate().inModel(model), x.getObject().inModel(model));
        model.add(statement);
        new Utils.ReifiedStatementBuilder(statement, rmodel).setDerivedBy(x.getSubject().getURI());
      } else {
        String objStr = x.getObject().asResource().getURI();
        if (objStr.contains(Namespaces.GKD)) {
          throw new RuntimeException();
        }
        if (objStr.contains(Namespaces.DBR)) {
          objStr = mapToNewNs.get(objStr);
        }
        if (objStr == null)
          return;
        Statement statement = model.createStatement(subj, x.getPredicate().inModel(model), model.createResource(objStr));
        model.add(statement);
        new Utils.ReifiedStatementBuilder(statement, rmodel).setDerivedBy(x.getSubject().getURI());
      }
    });
  }

  private void loadWikidata(String inputPath, String outputModelName, String derivedFromURI) {
    logger.info("load wikidata");
    Dataset wikidata = RDFDataMgr.loadDataset(inputPath);
    logger.info("Loading {} statements", wikidata.getDefaultModel().size());
    Model model = dataset.getNamedModel(Namespaces.GKS + outputModelName);
    Model rmodel = dataset.getNamedModel(Namespaces.GKS + "reified");
    wikidata.getDefaultModel().listStatements().forEachRemaining(x -> {
      String subjStr = mapToNewNs.get(x.getSubject().getURI());
      if (subjStr == null)
        return;
      Resource subj = model.createResource(subjStr);

      if (x.getObject().isLiteral()) {
        Statement statement = model.createStatement(subj, x.getPredicate().inModel(model), x.getObject().inModel(model));
        model.add(statement);
        if(derivedFromURI != null) {
          new Utils.ReifiedStatementBuilder(statement, rmodel).setDerivedByLiteral(derivedFromURI);
        } else {
          new Utils.ReifiedStatementBuilder(statement, rmodel).setDerivedBy(x.getSubject().getURI());
        }
      } else {
        String objStr = x.getObject().asResource().getURI();
        if (objStr.contains(Namespaces.GKD)) {
          throw new RuntimeException();
        }
        if (objStr.contains(Namespaces.WD)) {
          objStr = mapToNewNs.get(objStr);
        }
        if (objStr == null)
          return;
        Statement statement = model.createStatement(subj, x.getPredicate().inModel(model), model.createResource(objStr));
        model.add(statement);
        if(derivedFromURI != null) {
          new Utils.ReifiedStatementBuilder(statement, rmodel).setDerivedByLiteral(derivedFromURI);
        } else {
          new Utils.ReifiedStatementBuilder(statement, rmodel).setDerivedBy(x.getSubject().getURI());
        }
      }
    });
  }

  private void loadGeonames() throws IOException {
    logger.info("load geonames");

    try(FileOutputStream outputStream = new FileOutputStream(new File("output/geonames.nt.gz"));
        FileChannel chan = outputStream.getChannel()) {
      chan.truncate(0);
    }
    Files.asCharSink(new File("output/geonames.nt.graph"), StandardCharsets.UTF_8).write(Namespaces.GKS + "geonames");

    // fall back to use input stream processing
    for(File file: new File("output/geonames").listFiles((d, name) -> name.toLowerCase().endsWith(".nt.gz"))) {
      OutputStream outputStream = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File("output/geonames.nt.gz"), true)));
      StreamRDF writer = new StreamConverter(StreamRDFWriter.getWriterStream(
        outputStream, Lang.NT
      ));
      logger.info("reading {} in streaming fashion", file.getName());
      RDFDataMgr.parse(writer, file.getAbsolutePath());
      outputStream.flush();
      outputStream.close();
    }

    logger.info("load geonames reified");

    try(FileOutputStream outputStream = new FileOutputStream(new File("output/geonames_reified.nt.gz"));
        FileChannel chan = outputStream.getChannel()) {
      chan.truncate(0);
    }
    Files.asCharSink(new File("output/geonames_reified.nt.graph"), StandardCharsets.UTF_8).write(Namespaces.GKS + "reified");

    // fall back to use input stream processing
    for(File file: new File("output/geonames").listFiles((d, name) -> name.toLowerCase().endsWith(".nt.gz"))) {
      OutputStream outputStream = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File("output/geonames_reified.nt.gz"), true)));
      StreamRDF writer = new StreamConverterReified(StreamRDFWriter.getWriterStream(
              outputStream, Lang.NT
      ));
      logger.info("reading {} in streaming fashion", file.getName());
      RDFDataMgr.parse(writer, file.getAbsolutePath());
      outputStream.flush();
      outputStream.close();
    }
  }

  private void saveData() throws IOException {
    logger.info("saving...");
    // Utils.write(dataset, Namespaces.GKS + "dbpedia", "output/dbpedia");
    Utils.write(dataset, Namespaces.GKS + "wikidata", "output/wikidata");
    Utils.write(dataset, Namespaces.GKS + "wikidata-locclass", "output/wikidata_locclass");
    Utils.write(dataset, Namespaces.GKS + "natural_earth", "output/natural_earth");

    Utils.write(dataset, Namespaces.GKS + "reified", "output/reified");
  }

  public void runmain(String nodeMapPath) throws IOException {
    loadMapToNewNs(nodeMapPath);
    // loadDbpedia();
    loadWikidata("temp_output/new_align/nodes/wikidata_nodes.nt.gz", "wikidata-locclass", null);
    loadWikidata("output/wikidata/wikidata.nt.gz", "wikidata", null);
    loadWikidata("output/wikidata/natural_earth.nt.gz", "natural_earth", "http://www.naturalearthdata.com/");
    saveData();
    loadGeonames();
  }

  public static void main(String[] args) throws IOException {
    new ConvertData().runmain("output/new_align.nt.gz");
  }
}