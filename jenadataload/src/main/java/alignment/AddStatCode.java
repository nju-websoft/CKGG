package alignment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;
import utils.Pair;
import utils.Utils;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class AddStatCode {
  private static final Logger logger = LoggerFactory.getLogger(AddStatCode.class);
  private static final Gson gson = new Gson();

  static class DatasetNode {
    String uri;
    Set<String> featureCodes;
    Set<String> names;
    List<DatasetNode> parents;

    public DatasetNode() {
      names = new HashSet<>();
      featureCodes = new HashSet<>();
      // 四级行政区
      parents = new ArrayList<>(Arrays.asList(null, null, null, null));
    }
  }

  static class ChnAdmNode {
    String code;
    String type;
    String canonicalName;
    Set<String> names;
    List<ChnAdmNode> parents;

    public ChnAdmNode() {
      names = new HashSet<>();
      parents = new ArrayList<>(Arrays.asList(null, null, null, null));
    }
  }

  static class ChnAdmRawNode {
    public String code;
    public String type;
    public String name;
    public boolean hide;
    public List<String> path;
  }

  static interface Scorer {
    public double score(ChnAdmNode chnAdmNode, DatasetNode datasetNode);
  }

  Map<String, String> codeMap = new HashMap<>();
  Map<String, Integer> predMap = new HashMap<>();
  List<String> preds;
  List<String> ethnics;
  Scorer scorer;

  public AddStatCode() {
    scorer = new DefaultScorer();
    codeMap.put("PROVINCE", "A.ADM1");
    codeMap.put("CITY", "A.ADM2");
    codeMap.put("COUNTY", "A.ADM3");
    codeMap.put("TOWN", "A.ADM4");
    preds = Arrays.asList("P13", "P14", "P15", "P16");
    for(int i = 0; i < 4; i++) {
      predMap.put(preds.get(i), i);
    }
    ethnics = Arrays.asList(
      "汉族",
      "蒙古族",
      "回族",
      "藏族",
      "维吾尔族",
      "苗族",
      "彝族",
      "壮族",
      "布依族",
      "朝鲜族",
      "满族",
      "侗族",
      "瑶族",
      "白族",
      "土家族",
      "哈尼族",
      "哈萨克族",
      "傣族",
      "黎族",
      "傈僳族",
      "佤族",
      "畲族",
      "拉祜族",
      "水族",
      "东乡族",
      "纳西族",
      "景颇族",
      "柯尔克孜族",
      "土族",
      "达斡尔族",
      "仫佬族",
      "羌族",
      "布朗族",
      "撒拉族",
      "毛南族",
      "仡佬族",
      "锡伯族",
      "阿昌族",
      "普米族",
      "塔吉克族",
      "怒族",
      "乌孜别克族",
      "俄罗斯族",
      "鄂温克族",
      "德昂族",
      "保安族",
      "裕固族",
      "京族",
      "塔塔尔族",
      "独龙族",
      "鄂伦春族",
      "赫哲族",
      "门巴族",
      "珞巴族",
      "基诺族",
      "高山族"
    );
  }

  public List<String> processNames(String name) {
    List<String> allNames = new ArrayList<>();
    // 预处理1：去掉后缀，以便匹配，只会去掉一次
    List<String> suffixes = Arrays.asList(
            "特别行政区", "省", "市", "自治区",
            "地区", "盟", "自治州",
            "林区", "特区", "区", "自治县", "县", "自治旗", "旗",
            "街道", "民族乡", "苏木", "乡", "镇"
    );
    allNames.add(name);
    for(String s: suffixes) {
      if(name.endsWith(s)) {
        String newName = name.substring(0, name.length() - s.length());
        allNames.add(newName);
      }
    }
    // 预处理2：去掉民族后缀
    for(int i = 0; i < allNames.size(); i++) {
      String curName = allNames.get(i);
      while(true) {
        String oldCurName = curName;
        for(String s: ethnics) {
          if(curName.endsWith(s)) {
            curName = curName.substring(0, curName.length() - s.length());
            break;
          }
        }
        if(curName.equals(oldCurName)) break;
        allNames.add(curName);
      }
    }
    // 预处理3：去重
    return allNames.stream().distinct().collect(Collectors.toList());
  }

  Map<String, ChnAdmNode> codeToChnAdmNode = new HashMap<>();
  void readChnAdm(String inputPccPath) throws IOException {
    List<ChnAdmRawNode> rawNodes;
    try (BufferedReader reader = new BufferedReader(new FileReader(inputPccPath))) {
      rawNodes = reader.lines().map(x -> gson.fromJson(x, ChnAdmRawNode.class)).filter(x -> !x.hide).collect(Collectors.toList());
    }
    logger.info("node count is {}", rawNodes.size());
    logger.info("first node is {}", gson.toJson(rawNodes.get(0)));
    for(ChnAdmRawNode node: rawNodes) {
      ChnAdmNode newNode = new ChnAdmNode();
      newNode.code = node.code;
      newNode.type = codeMap.get(node.type);
      newNode.canonicalName = node.name;
      newNode.names.addAll(processNames(node.name));
      codeToChnAdmNode.put(newNode.code, newNode);
    }
    for(ChnAdmRawNode node: rawNodes) {
      ChnAdmNode newNode = codeToChnAdmNode.get(node.code);
      for(int i = 0; i < node.path.size() && i < 4; i++) {
        ChnAdmNode associatedNode = codeToChnAdmNode.get(node.path.get(i));
        if(associatedNode != null) {
          newNode.parents.set(i, associatedNode);
        }
      }
    }
    logger.info("node count is {}", codeToChnAdmNode.size());
    logger.info("first node is {}", gson.toJson(codeToChnAdmNode.values().stream().findFirst().orElse(null)));
  }

  Map<String, DatasetNode> codeToDatasetNode = new HashMap<>();
  void readDataset(String sparqlEndpoint) {
    VirtGraph endpoint;
    endpoint = new VirtGraph(sparqlEndpoint, "dba", "dba");
    endpoint.setReadFromAllGraphs(true);

    ParameterizedSparqlString pss = new ParameterizedSparqlString();
    pss.setNsPrefix("gkp", Namespaces.GKP);
    pss.setNsPrefix("gkd", Namespaces.GKD);
    pss.setNsPrefix("rdfs", RDFS.uri);

    // 第一步：加载所有候选实体
    pss.setCommandText(
      "SELECT DISTINCT ?subj WHERE {\n" +
      "  ?subj gkp:P8 ?gncls.\n" +
      "  ?subj gkp:P12 gkd:" + chinaId + ".\n" +
      "  FILTER(regex(?gncls, '^[APLSV]')).\n" +
      "}\n"
    );
    try(VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), endpoint)) {
      vqe.execSelect().forEachRemaining(x -> {
        DatasetNode node = new DatasetNode();
        node.uri = x.get("subj").asResource().getURI();
        codeToDatasetNode.put(node.uri, node);
      });
    }
    // 第二步：加载所有实体类别
    pss.setCommandText(
      "SELECT DISTINCT ?subj ?gncls WHERE {\n" +
      "  ?subj gkp:P8 ?gncls.\n" +
      "  ?subj gkp:P12 gkd:" + chinaId + ".\n" +
      "  FILTER(regex(?gncls, '^[APLSV]')).\n" +
      "}\n"
    );
    try(VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), endpoint)) {
      vqe.execSelect().forEachRemaining(x -> {
        String uri = x.get("subj").asResource().getURI();
        String gncls = x.get("gncls").asLiteral().getString();
        Optional.ofNullable(codeToDatasetNode.get(uri)).ifPresent(y -> y.featureCodes.add(gncls));
      });
    }
    // 第三步：加载所有实体标签
    pss.setCommandText(
      "SELECT DISTINCT ?subj ?label WHERE {\n" +
      "  ?subj rdfs:label ?label.\n" +
      "  ?subj gkp:P8 ?gncls.\n" +
      "  ?subj gkp:P12 gkd:" + chinaId + ".\n" +
      "  FILTER(regex(?gncls, '^[APLSV]')).\n" +
      "}\n"
    );
    try(VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), endpoint)) {
      vqe.execSelect().forEachRemaining(x -> {
        String uri = x.get("subj").asResource().getURI();
        String label = x.get("label").asLiteral().getString();
        Optional.ofNullable(codeToDatasetNode.get(uri)).ifPresent(y -> y.names.add(label));
      });
    }
    // 第四步：加载所有实体上级实体（多值直接替换）
    pss.setCommandText(
      "SELECT DISTINCT ?subj ?pred ?obj WHERE {\n" +
      "  ?subj ?pred ?obj.\n" +
      "  ?subj gkp:P8 ?gncls.\n" +
      "  ?subj gkp:P12 gkd:" + chinaId + ".\n" +
      "  FILTER(regex(?gncls, '^[APLSV]')).\n" +
      "  FILTER(?pred IN (gkp:P13, gkp:P14, gkp:P15, gkp:P16)).\n" +
      "  FILTER(?subj != ?obj).\n" +
      "}\n"
    );
    try(VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(pss.asQuery(), endpoint)) {
      vqe.execSelect().forEachRemaining(x -> {
        String pred = x.get("pred").asResource().getURI();
        String rawPred = Arrays.stream(pred.split("/")).reduce("", (fst, snd) -> snd);
        int predIndex = predMap.get(rawPred);
        
        String uri = x.get("subj").asResource().getURI();
        String obj = x.get("obj").asResource().getURI();
        Optional.ofNullable(codeToDatasetNode.get(uri)).ifPresent(y -> y.parents.set(predIndex, codeToDatasetNode.get(obj)));
      });
    }
    logger.info("node count is {}", codeToDatasetNode.size());
    logger.info("first node is {}", gson.toJson(codeToDatasetNode.values().stream().findFirst().orElse(null)));
  }

  List<Pair<ChnAdmNode, DatasetNode>> candidiates = new ArrayList<>();
  /**
   * 将所有同名实体对进行比对
   */
  private void recallPairs() {
    Map<String, List<ChnAdmNode>> nameJoinMap = new HashMap<>();
    codeToChnAdmNode.values().forEach(
      x -> {
        x.names.forEach(
          y -> nameJoinMap.computeIfAbsent(y, k -> new ArrayList<>()).add(x)
        );
      }
    );
    codeToDatasetNode.values().forEach(
      x -> x.names.forEach(
        y -> {
          Optional.ofNullable(nameJoinMap.get(y)).ifPresent(
            z -> z.forEach(w -> candidiates.add(new Pair<>(w, x)))
          );
        }
      )
    );
  }

  class DefaultScorer implements Scorer {
    @Override
    public double score(ChnAdmNode chnAdmNode, DatasetNode datasetNode) {
      double ans = 0;
      // 若存在完全同名，分数最高
      if(datasetNode.names.stream().anyMatch(x -> chnAdmNode.canonicalName.equals(x))) {
        ans += 1e8;
      }
      // 若存在相同上层实体，分数更高，存在不同上层实体，则除非完全同名，否则不匹配
      for (int i = 0; i < 4; i++) {
        ChnAdmNode cnparent = chnAdmNode.parents.get(i);
        DatasetNode gnparent = datasetNode.parents.get(i);
        if(gnparent == null || cnparent == null || !matchedG2C.containsKey(gnparent.uri)) continue;
        if(matchedG2C.get(gnparent.uri).equals(cnparent.code)) ans += (4 - i) * 1e4;
        else ans -= 1e5;
      }
      // 同级别行政实体分数更高
      if(datasetNode.featureCodes.contains(chnAdmNode.type)) {
        ans += 100;
      }
      // P类和A类分数更高
      else if(datasetNode.featureCodes.stream().anyMatch(x -> x.matches("^A\\.ADM.*$"))) {
        ans += 30;
      }
      else if(datasetNode.featureCodes.stream().anyMatch(x -> x.charAt(0) == 'A')) {
        ans += 20;
      }
      else if(datasetNode.featureCodes.stream().anyMatch(x -> x.charAt(0) == 'P')) {
        ans += 10;
      }
      // 最后再看match上的名字删了多少个字，剩下的字越多，越可能match
      int maxLen = 0;
      for(String gname: datasetNode.names) {
        for(String cname: chnAdmNode.names) {
          if(gname.equals(cname)) {
            maxLen = Math.max(maxLen, cname.length());
          }
        }
      }
      ans += maxLen;
      return ans;
    }
  }

  Map<String, String> matchedC2G = new HashMap<>();
  Map<String, String> matchedG2C = new HashMap<>();
  private void layeredMatch() {
    for(String code: Arrays.asList("A.ADM1", "A.ADM2", "A.ADM3", "A.ADM4")) {
      logger.info("layer {}", code);
      SimpleWeightedGraph<String, DefaultWeightedEdge> g = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

      Set<String> xNodes = new HashSet<>();
      Set<String> yNodes = new HashSet<>();
      candidiates.stream()
        .filter(x -> x.x.type.equals(code) && !matchedC2G.containsKey(x.x.code) && !matchedG2C.containsKey(x.y.uri))
        .forEach(x -> {
          g.addVertex(x.x.code);
          xNodes.add(x.x.code);
          g.addVertex(x.y.uri);
          yNodes.add(x.y.uri);
          DefaultWeightedEdge e = g.addEdge(x.x.code, x.y.uri);
          if(e != null) g.setEdgeWeight(e, scorer.score(x.x, x.y));
        });
      logger.info("matching");
      MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> matchAlg = new MaximumWeightBipartiteMatching<>(g, xNodes, yNodes);
      MatchingAlgorithm.Matching<String, DefaultWeightedEdge> matching = matchAlg.getMatching();
      for (DefaultWeightedEdge edge : matching) {
        String src = g.getEdgeSource(edge);
        String dst = g.getEdgeTarget(edge);
        if (!xNodes.contains(src) || !yNodes.contains(dst)) throw new RuntimeException();
        if(matchedC2G.containsKey(src) || matchedG2C.containsKey(dst)) throw new RuntimeException();
        matchedC2G.put(src, dst);
        matchedG2C.put(dst, src);
      }
    }
  }

  private void constructDataset(String outputPath) throws IOException {
    Dataset dataset = DatasetFactory.create();
    Model model = dataset.getNamedModel(Namespaces.GKS + "cn_code_align");
    Model rmodel = dataset.getNamedModel(Namespaces.GKS + "reified");
    matchedC2G.entrySet().stream()
      .map((e) -> new Pair<ChnAdmNode, DatasetNode>(codeToChnAdmNode.get(e.getKey()), codeToDatasetNode.get(e.getValue())))
      .forEach(p -> {
        Statement statement = model.createStatement(model.createResource(p.y.uri), RDFS.label, model.createLiteral(p.x.canonicalName));
        model.add(statement);
        new Utils.ReifiedStatementBuilder(statement, rmodel).setDerivedByLiteral("http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2020/index.html");
        statement = model.createStatement(model.createResource(p.y.uri), model.createProperty(Namespaces.GKP + "P762"), model.createLiteral(p.x.code));
        model.add(statement);
        new Utils.ReifiedStatementBuilder(statement, rmodel).setDerivedByLiteral("http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2020/index.html");
        for(int i = 0; i < 4; i++) {
          if(p.x.parents.get(i) == null) break;
          String nuri = matchedC2G.get(p.x.parents.get(i).code);
          if(nuri == null) break;
          statement = model.createStatement(model.createResource(p.y.uri), model.createProperty(Namespaces.GKP + preds.get(i)), model.createResource(nuri));
          model.add(statement);
          new Utils.ReifiedStatementBuilder(statement, rmodel).setDerivedByLiteral("http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2020/index.html");
        }
      });
    Utils.write(dataset, Namespaces.GKS + "cn_code_align", outputPath);
    Utils.write(dataset, Namespaces.GKS + "reified", "output/cn_code_align_reified");
  }

  String chinaId;

  /**
   * 进行中国行政区到图谱的对齐
   * 
   * @param sparqlEndpoint 导入了基本信息的图谱地址
   * @param inputPccPath 输入的中国行政区数据
   * @param outputPath 输出的路径（不带.nt.gz后缀）
   * @throws IOException
   */
  public void runmain(String sparqlEndpoint, String inputPccPath, String chinaId, String outputPath) throws IOException {
    this.chinaId = chinaId;
    readChnAdm(inputPccPath);
    readDataset(sparqlEndpoint);
    recallPairs();
    layeredMatch();
    constructDataset(outputPath);
  }
}
