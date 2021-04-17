import json
import random
from collections import defaultdict

random.seed(19260817)

items_by_type = defaultdict(list)
code_to_name = {}

for l in open('processed.jsonl'):
    item = json.loads(l)
    code_to_name[item['code']] = item['name']


for l in open('processed.jsonl'):
    item = json.loads(l)
    if item['type'] == 'PROVINCE' and any(x in item['name'] for x in ['台湾', '香港', '澳门']): 
        continue
    if item['hide']:
        continue
    item['path'] = [code_to_name[x] for x in item['path']]
    items_by_type[item['type']].append(item)
    

assert len(items_by_type['PROVINCE']) == 31
assert len(items_by_type['CITY']) == 333
assert len(items_by_type['COUNTY']) == 2989
assert len(items_by_type['TOWN']) == 41602

with open('provinces.jsonl', 'w') as f:
    for x in items_by_type['PROVINCE']:
        f.write(json.dumps(x, ensure_ascii=False) + '\n')

with open('citys.jsonl', 'w') as f:
    for x in random.sample(items_by_type['CITY'], 100):
        f.write(json.dumps(x, ensure_ascii=False) + '\n')

with open('countys.jsonl', 'w') as f:
    for x in random.sample(items_by_type['COUNTY'], 100):
        f.write(json.dumps(x, ensure_ascii=False) + '\n')

with open('towns.jsonl', 'w') as f:
    for x in random.sample(items_by_type['TOWN'], 100):
        f.write(json.dumps(x, ensure_ascii=False) + '\n')