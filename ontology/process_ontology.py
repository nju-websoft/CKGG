import os
import random

random.seed(0)
os.environ['PYTHONHASHSEED'] = '0'

from typing import Dict
import rdflib
import yaml
import re
from pprint import pprint
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("--ignore_explicit_instance", action="store_true")
parser.add_argument("--use_string_for_enum", action="store_true")
parser.add_argument("--prefix", default="http://w3id.org/ckgg/1.0/ontology/")
parser.add_argument("--input", default="ontology.yml")
args = parser.parse_args()
print(args)

input_tree = yaml.load(open(args.input, encoding='utf-8'), Loader=yaml.FullLoader)

def to_uriref(g, s):
    return URIRef([str(x[1]) for x in g.namespace_manager.namespaces() if x[0] == s.split(":")[0]][0] + s.split(":")[1])

def get_data_mapper():
    cur_id = 1
    # names
    name_dict = {'排序分数': 7, 'Wikipedia链接': 6}
    name_dict = {'名称': 2, '纬度': 21, '经度': 20, '所属国家': 12, '所属一级行政区': 13, '所属二级行政区': 14, '所属三级行政区': 15, '所属四级行政区': 16,
                 '地点': 5, '人口总量': 177, '海拔高度': 22, '时区': 48, '坐标': 23, '范围': 24, 'Geonames类别': 8, '行政区代码': 762,
                 '国家': 763, 'GMT偏移量': 1623, '上级地点': 10, '下级地点': 18, '月降水量属性': 1629, '月降水量': 90, '月降水量属性月份': 1630,
                 '月降水量值': 1631, '年降水量': 89, '月平均气温属性': 1632, '月平均气温': 75, '年均温': 71, '月平均气温属性月份': 1633, '月平均气温值': 1634, 
                 '太阳年辐射量': 39, '距海距离': 26, '暖流': 1102, '寒流': 1101, '影响洋流': 31, '气候类型': 86, '洋流位置': 705, '气候类型区域': 1228,
                 '洋流': 703}
    # add from selected_ids.json
    name_dict = {**name_dict, **{"人口性别比": 755, "单位产值能耗": 175, "粮食产量": 128, "植被覆盖率": 101, "失业率": 158, "死亡率": 750, "出生率": 749,
                 "地区生产总值": 118, "自然增长率": 751, "人均生产总值": 117}}
    # add from onto alignment
    name_dict = {**name_dict, **{
        "礁": 419,
        "铬矿": 950,
        "海沟": 479,
        "啤酒厂": 970,
        "动物园": 919,
        "气象站": 868,
        "咸水湖": 333,
        "沙丘": 441,
        "山峰": 516,
        "农场": 936,
        "矿场": 939,
        "海港": 881,
        "煤矿": 944,
        "洋盆": 480,
        "甘蔗制糖厂": 966,
        "港口": 880,
        "防洪堤坝": 916,
        "海岭": 481,
        "半岛": 432,
        "山坡": 519,
        "发电站": 911,
        "建筑": 863,
        "湿地": 631,
        "绿洲": 629,
        "河口": 395,
        "大坝": 910,
        "隧道": 903,
        "瀑布": 390,
        "古迹": 1014,
        "山系": 518,
        "盆地": 575,
        "冰川": 473,
        "沙漠": 622,
        "渔场": 1054,
        "行政区": 787,
        "桥梁": 904,
        "冲积平原": 460,
        "铜矿": 955,
        "陡坡": 511,
        "山脊": 505,
        "滩涂": 469,
        "海角": 368,
        "工厂": 959,
        "湖泊": 329,
        "面粉厂": 993,
        "宿营地": 870,
        "陡崖": 510,
        "海峡": 365,
        "泉": 388,
        "台地": 565,
        "沼泽": 627,
        "铁矿": 949,
        "街道": 820,
        "温泉": 389,
        "大洋": 387,
        "鱼塘": 932,
        "自然保护区": 726,
        "岛屿": 418,
        "天然气井": 946,
        "博物馆": 920,
        "二级行政区": 805,
        "水库": 345,
        "山地": 504,
        "寺庙": 1003,
        "高原": 498,
        "丘陵": 564,
        "汽车站": 898,
        "铁路": 892,
        "金矿": 957,
        "水电站": 914,
        "疗养院": 869,
        "浅海": 347,
        "海滩": 470,
        "河流": 211,
        "森林": 606,
        "草原": 617,
        "航空港": 899,
        "一级行政区": 799,
        "火车站": 900,
        "鞍部": 509,
        "四级行政区": 819,
        "海湾": 353,
        "火山": 468,
        "锡矿": 953,
        "山谷": 507,
        "三级行政区": 810,
        "工业区": 1057,
        "珊瑚礁": 421,
        "油井": 945,
        "运河": 260,
        "大陆": 410,
        "海": 381,
        "群岛": 425
    }}
    used_ids = set(name_dict.values())
    true_used = set()

    assert len(name_dict) == len(used_ids)

    def data_mapper(prefix: str, names: str):
        nonlocal cur_id, name_dict, true_used
        true_used.add(names)
        if names in name_dict:
            return prefix + str(name_dict[names])
        name_dict[names] = cur_id
        cur_id += 1
        while cur_id in used_ids:
            cur_id += 1
        return prefix + str(name_dict[names])

    def checker():
        nonlocal true_used, name_dict
        print(name_dict.keys() - true_used)
        assert len(true_used) == len(name_dict)

    return data_mapper, checker

P, checker = get_data_mapper()

class Tree:
    def __init__(self):
        self.classes = []
        self.instances = []
        self.properties = []

        self.mapping = {}
        self.cur_id = 0

    def dfs(self, pid, x):
        def add_to_place(pid, xid, x):
            self.mapping[xid] = x
            if x[0] == '_':
                self.properties.append((pid, xid))
            elif x[0] == '@':
                self.instances.append((pid, xid))
            else:
                self.classes.append((pid, xid))

        xid = self.cur_id
        self.cur_id += 1
        if isinstance(x, str):
            if x[-1] == '@':
                add_to_place(pid, xid, x[:-1])
                add_to_place(xid, self.cur_id, '@' + x[:-1])
                self.cur_id += 1
            else:
                add_to_place(pid, xid, x)
        elif isinstance(x, dict):
            if len(x) == 1:
                key = list(x.keys())[0]
                values = list(x.values())[0]
            else:
                values = x.get('children', [])
                for instance in x['instances']:
                    self.dfs(xid, '@' + instance)
                y = {a: b for (a, b) in x.items() if b is None}
                assert len(y) == 1, str(x)
                key = list(y.keys())[0]

            add_to_place(pid, xid, key)
            for value in values:
                self.dfs(xid, value)
        else:
            assert False

    def parse_label(self, s):
        ret = {}
        if '%%' in s:
            prov = s.split('%%')[1]
            if prov != '0':
                if 'http' in prov:
                    ret['prov:wasDerivedFrom'] = prov
                else:
                    ret['prov:wasDerivedFrom'] = 'gkbsrc:' + prov
            s = s.split('%%')[0]
        if '<->' in s:
            ret['owl:equivalentClass'] = s.split('<->')[1].split(',')
            s = s.split('<->')[0]
        if '^^' in s:
            ret['rdfs:range'] = s.split('^^')[1]
            s = s.split('^^')[0]
        s = s.split('/')
        ret['_name'] = s[0]
        for x in s:
            if x[0] == '_' or x[0] == '@':
                x = x[1:]
            if 'skos:prefLabel' not in ret:
                ret['skos:prefLabel'] = x
                ret['rdfs:label'] = x
            else:
                if 'skos:altLabel' not in ret:
                    ret['skos:altLabel'] = [x]
                else:
                    ret['skos:altLabel'].append(x)
        return ret
    
    def refine_mapping(self):
        for _, x in self.properties:
            self.mapping[x]['_type'] = 'property'
        for _, x in self.classes:
            self.mapping[x]['_type'] = 'class'
        for _, x in self.instances:
            self.mapping[x]['_type'] = 'instance'
        for x, y in self.classes:
            if x is None: continue
            self.mapping[y]['rdfs:subClassOf'] = x
        for x, y in self.instances:
            self.mapping[y]['rdf:type'] = x
        for x, y in self.properties:
            if self.mapping[x]['_type'] == 'class':
                self.mapping[y]['rdfs:domain'] = x
            elif self.mapping[x]['_type'] == 'property':
                self.mapping[y]['rdfs:domain'] = self.mapping[x]['rdfs:domain']
                self.mapping[y]['rdfs:subPropertyOf'] = x
            else:
                assert False
    
    def build_link(self):
        text2id: Dict[str, int] = {}
        for i, x in self.mapping.items():
            if x['_name'] in text2id:
                print('DUP', x['_name'], text2id[x['_name']], i)
            else:
                text2id[x['_name']] = i
        def process_range(s):
            if re.match(r'^\{.*\}$', s):
                return {
                    'type': 'enum',
                    'things': list(map(str.strip, s[1:-1].split(',')))
                }
            elif re.match(r'^\(.*\)$', s):
                raise RuntimeError('error ' + s)
            elif not re.match(r'^[a-zA-Z]+:[a-zA-Z]+', s):
                if s not in text2id: print(s); exit()
                return text2id.get(s, '??')
            return s
        for i, x in self.mapping.items():
            if 'rdfs:range' in x:
                x['rdfs:range'] = process_range(x['rdfs:range'])
            if x['_name'][0] == '_': x['_name'] = 'properties/' + P('P', x['_name'][1:])
            elif x['_name'][0] == '@': x['_name'] = 'instances/' + P('I', x['_name'][1:])
            else: x['_name'] = 'classes/' + P('Q', x['_name'])

    def process(self, input_tree):
        # step 1: dfs
        for x in input_tree:
            self.dfs(None, x)
        # step 2: parse mapping to get labels
        self.mapping = {i: self.parse_label(x) for i, x in self.mapping.items()}
        # step 3: get mapping type and hierachy
        self.refine_mapping()
        # step 4: build link
        self.build_link()
        # step 5: filter mapping
        self.mapping = {i: x for i, x in self.mapping.items() if x['_type'] != 'property' or x.get('rdfs:range', '??') != '??'}
        return self

tree = Tree().process(input_tree)

from rdflib import URIRef, BNode, Literal, Graph, Namespace
from rdflib.collection import Collection
from rdflib.namespace import DC, OWL, PROV, RDF, RDFS, SKOS, TIME, XSD

GKB = Namespace(args.prefix)
GKBSRC = Namespace(args.prefix + "src/")

def init_graph():
    g = Graph()
    g.bind('owl', OWL)
    g.bind('dc', DC)
    g.bind('gkb', GKB)
    g.bind('gkbsrc', GKBSRC)
    g.bind('time', TIME)
    g.bind('xsd', XSD)
    g.bind('skos', SKOS)
    g.bind('prov', PROV)
    g.bind('dbo', Namespace("http://dbpedia.org/ontology/"))
    g.bind('clinga', Namespace("http://ws.nju.edu.cn/clinga/"))
    g.bind('geo', Namespace("http://www.w3.org/2003/01/geo/wgs84_pos#"))
    g.bind('gn', Namespace("http://www.geonames.org/ontology#"))
    g.bind('geosparql', 'http://www.opengis.net/ont/geosparql#')
    return g

g = init_graph()
g.add((GKB[""], RDF.type, OWL.Ontology))
g.add((GKB[""], RDFS.comment, Literal("CKGG: A Chinese Knowledge Graph for High-School Geography and Beyond", 'en')))
g.add((GKB[""], RDFS.label, Literal("CKGG Ontology", 'en')))
g.add((GKB[""], RDFS.comment, Literal("中文高中地理知识图谱", 'zh-cn')))
g.add((GKB[""], RDFS.label, Literal("CKGG 本体", 'zh-cn')))
g.add((GKB[""], DC.creator, URIRef("http://ws.nju.edu.cn/")))

mapping_graph = init_graph()

for _, x in tree.mapping.items():
    if 'prov:wasDerivedFrom' in x:
        if x['_type'] == 'instance' and args.ignore_explicit_instance:
            continue
        g.add((GKB[x['_name']], PROV.wasDerivedFrom, to_uriref(g, x['prov:wasDerivedFrom']) if 'http' not in x['prov:wasDerivedFrom'] else Literal(x['prov:wasDerivedFrom'], datatype=XSD.anyURI)))
    else:
        print(f"WARN {x} dont have prov")
    if x['_type'] == 'class':
        g.add((GKB[x['_name']], RDF.type, OWL.Class))
        g.add((GKB[x['_name']], SKOS.prefLabel, Literal(x['skos:prefLabel'], 'zh-cn')))
        g.add((GKB[x['_name']], RDFS.label, Literal(x['rdfs:label'], 'zh-cn')))
        for y in x.get('skos:altLabel', []):
            g.add((GKB[x['_name']], SKOS.altLabel, Literal(y, 'zh-cn')))
        if 'rdfs:subClassOf' in x:
            g.add((GKB[x['_name']], RDFS.subClassOf, GKB[tree.mapping[x['rdfs:subClassOf']]['_name']]))
        if 'owl:equivalentClass' in x:
            mapping_graph.add((GKB[x['_name']], RDF.type, OWL.Class))
            for y in x['owl:equivalentClass']:
                mapping_graph.add((GKB[x['_name']], OWL.equivalentClass, to_uriref(mapping_graph, y)))
    elif x['_type'] == 'property':
        if 'rdfs:range' not in x: continue
        if x['rdfs:range'] == '??': assert False
        objp = False
        datap = False
        if isinstance(x['rdfs:range'], int) or x['rdfs:range'] in ['time:ProperInterval'] or isinstance(x['rdfs:range'], dict):
            g.add((GKB[x['_name']], RDF.type, OWL.ObjectProperty))
            objp = True
            if isinstance(x['rdfs:range'], int):
                g.add((GKB[x['_name']], RDFS.range, GKB[tree.mapping[x['rdfs:range']]['_name']]))
            if isinstance(x['rdfs:range'], dict) and x['rdfs:range']['type'] == 'enum':
                if args.use_string_for_enum:
                    g.add((GKB[x['_name']], RDF.type, OWL.DatatypeProperty))
                    g.add((GKB[x['_name']], RDFS.range, XSD.string))
                    objp = False
                    g.remove((GKB[x['_name']], RDF.type, OWL.ObjectProperty))
                    datap = True
                else:
                    bn = BNode()
                    g.add((GKB[x['_name']], RDFS.range, bn))
                    g.add((bn, RDF.type, OWL.Class))
                    cbn = BNode()
                    g.add((bn, OWL.oneOf, cbn))
                    cn = Collection(g, cbn)

                    for y in x['rdfs:range']['things']:
                        yn = URIRef(GKB['instances/' + y])
                        g.add((yn, RDFS.label, Literal(y, 'zh-cn')))
                        g.add((yn, RDF.type, OWL.Thing))
                        cn.append(yn)
        else:
            g.add((GKB[x['_name']], RDF.type, OWL.DatatypeProperty))
            datap = True
            if x['rdfs:range'] == 'gkb:qualitative':
                datatype_node = BNode()
                list_node = BNode()
                g.add((GKB[x['_name']], RDFS.range, datatype_node))
                g.add((datatype_node, RDF.type, RDFS.Datatype))
                g.add((datatype_node, GKB.tobereplaced, list_node))
            elif isinstance(x['rdfs:range'], str) and ':' in x['rdfs:range']:
                g.add((GKB[x['_name']], RDFS.range, to_uriref(g, x['rdfs:range'])))
        if 'rdfs:subPropertyOf' in x and x['rdfs:subPropertyOf'] in tree.mapping:
            g.add((GKB[x['_name']], RDFS.subPropertyOf, GKB[tree.mapping[x['rdfs:subPropertyOf']]['_name']]))
        g.add((GKB[x['_name']], SKOS.prefLabel, Literal(x['skos:prefLabel'], 'zh-cn')))
        g.add((GKB[x['_name']], RDFS.label, Literal(x['rdfs:label'], 'zh-cn')))
        for y in x.get('skos:altLabel', []):
            g.add((GKB[x['_name']], SKOS.altLabel, Literal(y, 'zh-cn')))
        g.add((GKB[x['_name']], RDFS.domain, URIRef(GKB[tree.mapping[x['rdfs:domain']]['_name']])))
        if 'owl:equivalentClass' in x:
            if objp:
                mapping_graph.add((GKB[x['_name']], RDF.type, OWL.ObjectProperty))
            elif datap:
                mapping_graph.add((GKB[x['_name']], RDF.type, OWL.DatatypeProperty))
            for y in x['owl:equivalentClass']:
                mapping_graph.add((GKB[x['_name']], OWL.equivalentProperty, to_uriref(mapping_graph, y)))
    elif x['_type'] == 'instance':
        if args.ignore_explicit_instance:
            continue
        g.add((GKB[x['_name']], SKOS.prefLabel, Literal(x['skos:prefLabel'], 'zh-cn')))
        g.add((GKB[x['_name']], RDFS.label, Literal(x['rdfs:label'], 'zh-cn')))
        for y in x.get('skos:altLabel', []):
            g.add((GKB[x['_name']], SKOS.altLabel, Literal(y, 'zh-cn')))
        g.add((GKB[x['_name']], RDF.type, URIRef(GKB[tree.mapping[x['rdf:type']]['_name']])))
    else:
        assert False

for x, y in [l.strip().split(',') for l in open('toc.csv')]:
    g.add((GKBSRC[str(x)], SKOS.prefLabel, Literal(y, 'zh-cn')))
    g.add((GKBSRC[str(x)], RDFS.label, Literal(y, 'zh-cn')))

checker()

g.serialize('../release/ontology_pre.owl', 'pretty-xml')
mapping_graph.serialize('../release/mapping.owl', 'pretty-xml')

with open('../release/ontology.owl', 'w') as f:
    for l in open('../release/ontology_pre.owl').readlines():
        if 'gkb:tobereplaced' in l:
            f.write("""
        <owl:oneOf>
            <rdf:Description>
                <rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#List"/>
                <rdf:first rdf:datatype="http://www.w3.org/2001/XMLSchema#string">中</rdf:first>
                <rdf:rest>
                    <rdf:Description>
                        <rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#List"/>
                        <rdf:first rdf:datatype="http://www.w3.org/2001/XMLSchema#string">低</rdf:first>
                        <rdf:rest>
                            <rdf:Description>
                                <rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#List"/>
                                <rdf:first rdf:datatype="http://www.w3.org/2001/XMLSchema#string">高</rdf:first>
                                <rdf:rest>
                                    <rdf:Description>
                                        <rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#List"/>
                                        <rdf:first rdf:datatype="http://www.w3.org/2001/XMLSchema#string">非常高</rdf:first>
                                        <rdf:rest>
                                            <rdf:Description>
                                                <rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#List"/>
                                                <rdf:first rdf:datatype="http://www.w3.org/2001/XMLSchema#string">非常低</rdf:first>
                                                <rdf:rest rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
                                            </rdf:Description>
                                        </rdf:rest>
                                    </rdf:Description>
                                </rdf:rest>
                            </rdf:Description>
                        </rdf:rest>
                    </rdf:Description>
                </rdf:rest>
            </rdf:Description>
        </owl:oneOf>\n""")
        else:
            f.write(l)

# print(text2id)
