import os
from flask import *
import sys
import json
import re
import requests
from flask_cors import CORS, cross_origin
from ltp import LTP

import threading
mu = threading.Lock()

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
os.environ['FLASK_DEBUG'] = '1'

app = Flask(__name__)
app.config['CORS_HEADERS'] = 'Content-Type'
cors = CORS(app)

api = Blueprint('api', __name__, url_prefix='/api')
static = Blueprint('static', __name__, static_folder='static', static_url_path='/', url_prefix='/')

SPARQL_HOST = "http://DELETED_HOST:DELETED_PORT/"
SEARCH_HOST = "http://DELETED_HOST:DELETED_PORT/"

class Ner:
    def __init__(self):
        self.ltp = LTP()
    
    def preprocess(self, sent):
        return re.sub('\s+', '', sent)

    def ner(self, sents):
        assert not any(re.search(r'\s', x) for x in sents), "no space is allowed"
        psents = [x for x in sents if x != '']
        if len(psents) == 0:
            return [[] for x in sents]
        segment, hidden = self.ltp.seg(psents)
        ne = self.ltp.ner(hidden)
        anes = []
        for sseg, sne in zip(segment, ne):
            nes = []
            slens = [0] + [len(x) for x in sseg]
            for i in range(1, len(slens)):
                slens[i] += slens[i - 1]
            for t, x, y in sne:
                if t == 'Ns':
                    nes.append([slens[x], slens[y + 1]])
            anes.append(nes)
        fnes = []
        cur = 0
        for s in sents:
            if s == '':
                fnes.append([])
            else:
                fnes.append(anes[cur])
                cur += 1
        return fnes

@api.route('/search_problem', methods=['GET'])
@cross_origin()
def search_problem():
    q = request.args.get('q')
    ret = requests.post(SEARCH_HOST + 'search',data={'query':q,'model':'全体'}).json()

    return jsonify(ret)

@api.route('/problem/<int:problem_id>', methods=['GET'])
@cross_origin()
def get_problem(problem_id):
    global ner
    if ner == None:
        try:
            mu.acquire()
            if ner == None:
                ner = Ner()
        finally:
            mu.release()
    
    ret = requests.post(SEARCH_HOST + 'detail',data={'id': problem_id}).json()

    ret['background'] = ner.preprocess(ret['background'])
    keys = ['question', 'choice_A', 'choice_B', 'choice_C', 'choice_D', 'explains']
    for x in ret['multiplechoice']:
        for k in keys:
            x[k] = ner.preprocess(x[k])
    ret['background_ner'] = ner.ner([ret['background']])[0]
    for x in ret['multiplechoice']:
        for k, v in zip([k + '_ner' for k in keys], ner.ner([x[k] for k in keys])):
            x[k] = v
    return jsonify(ret)

@api.route('/spatialQuery', methods=['GET'])
@cross_origin()
def spatial_query():
    lat = request.args.get('lat')
    lng = request.args.get('lng')
    dist = request.args.get('dist')

    sparql = """
PREFIX  geo: <http://www.opengis.net/ont/geosparql#>
PREFIX geof: <http://www.opengis.net/def/function/geosparql/>

SELECT ?s AS ?entity, SAMPLE(?n) AS ?name, SAMPLE(?o) AS ?coord, SAMPLE(?lng) AS ?lng, SAMPLE(?lat) AS ?lat, MIN(?dist) AS ?dist WHERE {
    GRAPH <temp:coord> {
        SELECT ?s ?o ?dist ?lng ?lat {
            ?s ckgp:P23 ?o.
            BIND (bif:st_distance(?o, bif:st_geomfromtext("POINT(${lng} ${lat})")) AS ?dist).
            BIND (bif:st_x(?o) AS ?lng).
            BIND (bif:st_y(?o) AS ?lat).
            FILTER (bif:st_within(bif:st_geomfromtext("POINT(${lng} ${lat})"), ?o, ${dist})).
            FILTER (?dist < ${dist}).
        }
    }
    GRAPH <http://w3id.org/ckgg/1.0/> {
        ?s rdfs:label ?n.
        ?s ckgp:P7 ?w.
        FILTER (langMatches(lang(?n), 'zh')).
    }
    BIND((?w * 1e9 - ?dist) AS ?wv) 
} GROUP BY ?s ORDER BY DESC(MAX(?wv)) ?dist LIMIT 100
    """

    sparql = sparql.replace("${lng}", str(lng))
    sparql = sparql.replace("${lat}", str(lat))
    sparql = sparql.replace("${dist}", str(dist))

    resp = requests.post(SPARQL_HOST + 'sparql', {'query': sparql, 'format': 'json', 'timeout': 30000})

    excluded_headers = ['content-encoding', 'content-length', 'transfer-encoding', 'connection']
    headers = [(name, value) for (name, value) in resp.raw.headers.items()
               if name.lower() not in excluded_headers]

    response = Response(resp.content, resp.status_code, headers)
    return response

@api.route('/keywordQuery', methods=['GET'])
@cross_origin()
def keyword_query():
    keyword = request.args.get('keyword')
    sparql = """
PREFIX wgs84_pos: <http://www.w3.org/2003/01/geo/wgs84_pos#>

SELECT ?entity, SAMPLE(?name) AS ?name, SAMPLE(?bcl) AS ?bcl, SAMPLE(?b1l) AS ?b1l, SAMPLE(?lat) AS ?lat, SAMPLE(?lng) AS ?lng WHERE {
    GRAPH <http://w3id.org/ckgg/1.0/> {
        {
            SELECT (?s AS ?entity) (str(?n) AS ?name) ?wboost {
                ?s a ckgc:Q5 .
                ?s rdfs:label ?n .
                FILTER (
                    (?n = "${keyword}") OR
                    (?n = "${keyword}"@zh) OR
                    (?n = "${keyword}"@en) OR
                    (?n = "${keyword}"@zh-cn) OR
                    (?n = "${keyword}"@zh-hans) OR
                    (?n = "${keyword}"@zh-hk) OR
                    (?n = "${keyword}"@zh-hant) OR
                    (?n = "${keyword}"@zh-sg) OR
                    (?n = "${keyword}"@zh-mo) OR
                    (?n = "${keyword}"@zh-my)
                )
                BIND(1e9 AS ?wboost)
            }
        } UNION {
            SELECT (?s AS ?entity) (str(?n) AS ?name) ?wboost {
                ?s a ckgc:Q5 .
                ?s rdfs:label ?n .
                ?n bif:contains '"${keyword}"' .
                BIND(0 AS ?wboost)
            }
        } UNION {
            SELECT (?s AS ?entity) (SAMPLE(?n) AS ?name) ?wboost {
                ?s a ckgc:Q5 .
                ?s rdfs:label ?n .
                FILTER (?s = <http://w3id.org/ckgg/1.0/instances/location/${keyword.replaceAll(' ', '_')}>)
                BIND(1e10 AS ?wboost)
            }
        } UNION {
            SELECT (?s AS ?entity) (SAMPLE(?n) AS ?name) ?wboost {
                ?s a ckgc:Q5 .
                ?s rdfs:label ?n .
                ?s ckgp:P762 "${keyword}".
                BIND(1e10 AS ?wboost)
            }
        } UNION {
            SELECT (?s AS ?entity) (SAMPLE(?sn) AS ?name) {
                ?s a ckgc:Q5 .
                ?s rdfs:label ?sn .
                ?s ckgp:P86 ?n .
                ?n rdfs:label ?nl .
                FILTER (?nl = "${keyword}"@zh-cn)
                ?s ckgp:P7 ?cw.
                FILTER (?cw > 0).
                FILTER (langMatches(lang(?sn), 'zh')).
            } ORDER BY DESC(?cw) LIMIT 100
        } UNION {
            SELECT (?s AS ?entity) (SAMPLE(?sn) AS ?name) {
                ?s a ckgc:Q5 .
                ?s rdfs:label ?sn .
                ?s ckgp:P31 ?n .
                ?n rdfs:label ?nl .
                FILTER (?nl = "${keyword}"@zh-cn)
                ?s ckgp:P7 ?cw.
                FILTER (?cw > 0).
                FILTER (langMatches(lang(?sn), 'zh')).
            } ORDER BY DESC(?cw) LIMIT 100
        }
        ?entity ckgp:P7 ?w.
        ?entity wgs84_pos:lat ?lat .
        ?entity wgs84_pos:lon ?lng .
        OPTIONAL {
            ?entity ckgp:P12 ?bc .
            ?bc rdfs:label ?bcl .
            FILTER (langMatches(lang(?bcl), "zh")) .
        }
        OPTIONAL {
            ?entity ckgp:P13 ?b1 .
            ?b1 rdfs:label ?b1l .
            FILTER (langMatches(lang(?b1l), "zh")) .
        }
        BIND((?w + COALESCE(?wboost, 0)) AS ?wv)
    }
} GROUP BY ?entity ORDER BY DESC(MAX(?wv)) LIMIT 100
    """
    sparql = sparql.replace("${keyword}", str(keyword))
    sparql = sparql.replace("${keyword.replaceAll(' ', '_')}", str(keyword).replace(' ', '_'))

    resp = requests.post(SPARQL_HOST + 'sparql', {'query': sparql, 'format': 'json', 'timeout': 30000})

    excluded_headers = ['content-encoding', 'content-length', 'transfer-encoding', 'connection']
    headers = [(name, value) for (name, value) in resp.raw.headers.items()
               if name.lower() not in excluded_headers]

    response = Response(resp.content, resp.status_code, headers)
    return response

@api.route('/describe', methods=['GET'])
@cross_origin()
def describe():
    uri = request.args.get('uri')
    sparql = """
PREFIX wgs84_pos: <http://www.w3.org/2003/01/geo/wgs84_pos#>

SELECT
    ?name
    ?allupper
    ?allupperl
    ?alltype
    ?allname
    ?keyword
    ?lon
    ?lat
    ?poly
    ?alt
    ?code
    ?gncls
    ?wikilink
    ?sameAs
    ?rankscore
    ?pop
    ?toocean
    ?bc
    ?bcl
    ?b1
    ?b1l
    ?b2
    ?b2l
    ?b3
    ?b3l
    ?b4
    ?b4l
    ?tzv
    ?solar
    ?oce
    ?oc
    ?cle
    ?cl
    ?tm
    ?tv
    ?pm
    ?pv
    ?P755
    ?P175
    ?P128
    ?P101
    ?P158
    ?P750
    ?P749
    ?P118
    ?P751
    ?P117
    ?all_coord # for debugging
WHERE { GRAPH <http://w3id.org/ckgg/1.0/> {
    OPTIONAL { SELECT ?name WHERE {
        <${uri}> rdfs:label ?name .
        BIND(
        IF(langMatches(lang(?name), "zh"), IF(langMatches(lang(?name), "zh-cn"), -2, -1), 0)
        AS ?zhn)
    } ORDER BY ?zhn LIMIT 1 }
    OPTIONAL { SELECT GROUP_CONCAT(?typename, "\t") AS ?alltype WHERE {
        <${uri}> a ?type .
        GRAPH <http://tempuri/src/ontology_manual> {
            ?type skos:prefLabel ?typename .
        }
    } }
    OPTIONAL { SELECT GROUP_CONCAT(?cname, "\t") AS ?allname WHERE {
        <${uri}> rdfs:label ?name .
        BIND(CONCAT(?name, "@", lang(?name)) AS ?cname).
    } }
    OPTIONAL { SELECT GROUP_CONCAT(str(?cname), " ") AS ?keyword WHERE {
        SELECT DISTINCT ?cname WHERE {
            <${uri}> rdfs:label ?name .
            FILTER langMatches(lang(?name), "zh")
            BIND(str(?name) AS ?cname)
        }
    } }
    OPTIONAL { SELECT GROUP_CONCAT(str(?u), "\t") AS ?allupper GROUP_CONCAT(str(?ul), "\t") AS ?allupperl WHERE {
        SELECT ?u SAMPLE(?ul) AS ?ul WHERE {
            <${uri}> ckgp:P10 ?u .
            ?u rdfs:label ?ul.
            FILTER (langMatches(lang(?ul), "zh")) .
        }
    } }
    OPTIONAL { SELECT * WHERE {
        <${uri}> wgs84_pos:lon ?lon
    } LIMIT 1 }
    OPTIONAL { SELECT * WHERE {
        <${uri}> wgs84_pos:lat ?lat
    } LIMIT 1 }
    OPTIONAL { SELECT * WHERE {
        <${uri}> ckgp:P24 ?poly
    } LIMIT 1 }
    OPTIONAL { SELECT * WHERE {
        <${uri}> ckgp:P22 ?alt
    } LIMIT 1 }
    OPTIONAL { SELECT GROUP_CONCAT(?code, "\\t") AS ?code WHERE {
        <${uri}> ckgp:P762 ?code .
    } }
    OPTIONAL { SELECT GROUP_CONCAT(?gncls, "\\t") AS ?gncls WHERE {
        <${uri}> ckgp:P8 ?gncls .
    } }
    OPTIONAL { SELECT GROUP_CONCAT(?wikilink, "\\t") AS ?wikilink WHERE {
        <${uri}> ckgp:P6 ?wikilink .
    } }
    OPTIONAL { SELECT GROUP_CONCAT(?sameAs, "\\t") AS ?sameAs WHERE {
        <${uri}> owl:sameAs ?sameAs .
    } }
    OPTIONAL{ SELECT * WHERE {<${uri}> ckgp:P7 ?rankscore.} LIMIT 1}
    OPTIONAL { SELECT * WHERE {
        <${uri}> ckgp:P177 ?pop
    } LIMIT 1 }
    OPTIONAL { SELECT * WHERE {
        <${uri}> ckgp:P26 ?toocean
    } LIMIT 1 }
    OPTIONAL { SELECT ?bc ?bcl WHERE {
        <${uri}> ckgp:P12 ?bc .
        ?bc rdfs:label ?bcl .
        BIND (IF(langMatches(lang(?bcl), "zh"), 1, 0) AS ?lw) .
    } ORDER BY DESC(?lw) LIMIT 1 }
    OPTIONAL { SELECT ?b1 ?b1l WHERE {
        <${uri}> ckgp:P13 ?b1 .
        ?b1 rdfs:label ?b1l .
        BIND (IF(langMatches(lang(?b1l), "zh"), 1, 0) AS ?lw) .
    } ORDER BY DESC(?lw) LIMIT 1 }
    OPTIONAL { SELECT ?b2 ?b2l WHERE {
        <${uri}> ckgp:P14 ?b2 .
        ?b2 rdfs:label ?b2l .
        BIND (IF(langMatches(lang(?b2l), "zh"), 1, 0) AS ?lw) .
    } ORDER BY DESC(?lw) LIMIT 1 }
    OPTIONAL { SELECT ?b3 ?b3l WHERE {
        <${uri}> ckgp:P15 ?b3 .
        ?b3 rdfs:label ?b3l .
        BIND (IF(langMatches(lang(?b3l), "zh"), 1, 0) AS ?lw) .
    } ORDER BY DESC(?lw) LIMIT 1 }
    OPTIONAL { SELECT ?b4 ?b4l WHERE {
        <${uri}> ckgp:P16 ?b4 .
        ?b4 rdfs:label ?b4l .
        BIND (IF(langMatches(lang(?b4l), "zh"), 1, 0) AS ?lw) .
    } ORDER BY DESC(?lw) LIMIT 1 }
    OPTIONAL { SELECT * WHERE {
        <${uri}> ckgp:P48 ?tz .
        ?tz ckgp:P1623 ?tzv .
    } LIMIT 1 }
    OPTIONAL { SELECT * WHERE {
        <${uri}> ckgp:P39 ?solar
    } LIMIT 1 }
    OPTIONAL { SELECT GROUP_CONCAT(?oce, "\t") AS ?oce GROUP_CONCAT(?oc, "\t") AS ?oc WHERE {
        <${uri}> ckgp:P31 ?oce .
        ?oce rdfs:label ?oc .
    } }
    OPTIONAL { SELECT GROUP_CONCAT(?cle, "\t") AS ?cle GROUP_CONCAT(?cl, "\t") AS ?cl WHERE {
        <${uri}> ckgp:P86 ?cle .
        ?cle rdfs:label ?cl .
    } }
    OPTIONAL { SELECT GROUP_CONCAT(?tm, "\\t") AS ?tm GROUP_CONCAT(?tv, "\\t") AS ?tv WHERE {
        <${uri}> ckgp:P75 ?tn .
        ?tn ckgp:P1634 ?tv .
        ?tn ckgp:P1633 ?tm .
    } }
    OPTIONAL { SELECT GROUP_CONCAT(?pm, "\\t") AS ?pm GROUP_CONCAT(?pv, "\\t") AS ?pv WHERE {
        <${uri}> ckgp:P90 ?pn .
        ?pn ckgp:P1631 ?pv .
        ?pn ckgp:P1630 ?pm .
    } }
    OPTIONAL{ SELECT * WHERE {<${uri}> ckgp:P755 ?P755.} LIMIT 1}
    OPTIONAL{ SELECT * WHERE {<${uri}> ckgp:P175 ?P175.} LIMIT 1}
    OPTIONAL{ SELECT * WHERE {<${uri}> ckgp:P128 ?P128.} LIMIT 1}
    OPTIONAL{ SELECT * WHERE {<${uri}> ckgp:P101 ?P101.} LIMIT 1}
    OPTIONAL{ SELECT * WHERE {<${uri}> ckgp:P158 ?P158.} LIMIT 1}
    OPTIONAL{ SELECT * WHERE {<${uri}> ckgp:P750 ?P750.} LIMIT 1}
    OPTIONAL{ SELECT * WHERE {<${uri}> ckgp:P749 ?P749.} LIMIT 1}
    OPTIONAL{ SELECT * WHERE {<${uri}> ckgp:P118 ?P118.} LIMIT 1}
    OPTIONAL{ SELECT * WHERE {<${uri}> ckgp:P751 ?P751.} LIMIT 1}
    OPTIONAL{ SELECT * WHERE {<${uri}> ckgp:P117 ?P117.} LIMIT 1}
}
{
    SELECT GROUP_CONCAT(?all_coord, "\t") AS ?all_coord {
        SELECT DISTINCT ?all_coord {
             <${uri}> ckgp:P23 ?all_coord.
        }
    }
}
} LIMIT 1
    """

    sparql = sparql.replace("${uri}", str(uri))
    
    resp = requests.post(SPARQL_HOST + 'sparql', {'query': sparql, 'format': 'json', 'timeout': 30000})

    excluded_headers = ['content-encoding', 'content-length', 'transfer-encoding', 'connection']
    headers = [(name, value) for (name, value) in resp.raw.headers.items()
               if name.lower() not in excluded_headers]

    response = Response(resp.content, resp.status_code, headers)
    return response

@api.route('/getAllClimates', methods=['GET'])
@cross_origin()
def get_all_climates():
    sparql = """
SELECT ?name {
    {GRAPH <http://w3id.org/ckgg/1.0/> {?s a ckgc:Q86. ?s rdfs:label ?name.}}
}
    """

    resp = requests.post(SPARQL_HOST + 'sparql', {'query': sparql, 'format': 'json', 'timeout': 30000})

    excluded_headers = ['content-encoding', 'content-length', 'transfer-encoding', 'connection']
    headers = [(name, value) for (name, value) in resp.raw.headers.items()
               if name.lower() not in excluded_headers]

    response = Response(resp.content, resp.status_code, headers)
    return response

@api.route('/getAllOceanCurrents', methods=['GET'])
@cross_origin()
def get_all_ocean_currents():
    sparql = """
SELECT ?name {
    {GRAPH <http://w3id.org/ckgg/1.0/> {?s a ckgc:Q703. ?s rdfs:label ?name.}}
}
    """

    resp = requests.post(SPARQL_HOST + 'sparql', {'query': sparql, 'format': 'json', 'timeout': 30000})

    excluded_headers = ['content-encoding', 'content-length', 'transfer-encoding', 'connection']
    headers = [(name, value) for (name, value) in resp.raw.headers.items()
               if name.lower() not in excluded_headers]

    response = Response(resp.content, resp.status_code, headers)
    return response

@api.route('/auxPolygonQuery', methods=['GET'])
@cross_origin()
def aux_polygon_query():
    text = request.args.get('text')
    sparql = """
SELECT ?poly ?name ?description {
    {GRAPH <http://w3id.org/ckgg/1.0/> {?n rdfs:label "${text}"@zh-cn; ckgp:P1228 ?poly. OPTIONAL{?n skos:definition ?desc.}}} UNION
    {GRAPH <http://w3id.org/ckgg/1.0/> {?n rdfs:label "${text}"@zh-cn; ckgp:P705 ?poly. OPTIONAL{?n skos:definition ?desc.}}}
    BIND("${text}"@zh-cn AS ?name)
    BIND(COALESCE(?desc, "") AS ?description)
}
    """

    sparql = sparql.replace('${text}', str(text))
    
    resp = requests.post(SPARQL_HOST + 'sparql', {'query': sparql, 'format': 'json', 'timeout': 30000})

    excluded_headers = ['content-encoding', 'content-length', 'transfer-encoding', 'connection']
    headers = [(name, value) for (name, value) in resp.raw.headers.items()
               if name.lower() not in excluded_headers]

    response = Response(resp.content, resp.status_code, headers)
    return response

@static.route('/', methods=['GET'])
@cross_origin()
def get_index():
    return static.send_static_file('index.html')

ner = None

app.register_blueprint(api)
app.register_blueprint(static)
print(app.url_map)

if __name__ == "__main__":
    app.run('0.0.0.0', 9000)