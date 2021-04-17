import re
import json
import itertools

data = (json.loads(x) for x in open('items.jsonl').readlines())
data = (x for x in data if x['type'] != 'VILLAGE' and x['name'] is not None)
data = ({**x, 'hide': re.search('^市辖区$|级(单位|行政区划)$|直属|^街道$|^县$', x['name']) is not None} for x in data)
data = ({**x, 'code': x['code'] if x['type'] != 'PROVINCE' else x['code'] + '0' * 10} for x in data)
data = ({**x, 'path': x['path'] if x['path'] != ['index'] else []} for x in data)
data = ({**x, 'path': x['path'] if len(x['path']) <= 1 or len(x['path'][1]) != 2 else [x['path'][0], x['path'][0] + x['path'][1], x['path'][2]]} for x in data)
data = ({**x, 'path': [y + '0' * (12 - len(y)) for y in x['path']]} for x in data)
data = itertools.chain(data, [
    {'type': 'PROVINCE', 'code': '710000000000', 'name': '台湾省', 'path': []},
    {'type': 'PROVINCE', 'code': '810000000000', 'name': '香港特别行政区', 'path': []},
    {'type': 'PROVINCE', 'code': '820000000000', 'name': '澳门特别行政区', 'path': []},
])
data = (json.dumps(x, ensure_ascii=False) for x in data)
file = open('processed.jsonl', 'w')
for x in data:
    file.write(x + '\n')
