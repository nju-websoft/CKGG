import requests as _requests
import time
import json
import logging
from typing import Generator, List, Dict, Optional

logger = logging.getLogger(__name__)

class RequestsProxy:
    def __init__(self, interval, retry) -> None:
        self.last_time = 0
        self.interval = interval
        self.retry = retry
    def wait_for_time(self):
        while time.time() < self.last_time + self.interval:
            time.sleep(0.1)
        self.last_time = time.time()
    def get_json(self, *args, **kwargs):
        for _ in range(self.retry + 1):
            try:
                self.wait_for_time()
                logger.error('get ' + args[0])
                return _requests.get(*args, **kwargs).json()
            except Exception:
                if _ == self.retry:
                    raise
                logger.error('retry')
                
    def post_json(self, *args, **kwargs):
        for _ in range(self.retry + 1):
            try:
                self.wait_for_time()
                logger.error('post ' + args[0])
                return _requests.post(*args, **kwargs).json()
            except Exception:
                if _ == self.retry:
                    raise
                logger.error('retry')

requests = RequestsProxy(5, 5)

def get_timestamp():
    return round(time.time() * 1000)

headers = {
    'Connection': 'keep-alive',
    'sec-ch-ua': '"Chromium";v="88", "Google Chrome";v="88", ";Not A Brand";v="99"',
    'Accept': 'application/json, text/javascript, */*; q=0.01',
    'X-Requested-With': 'XMLHttpRequest',
    'sec-ch-ua-mobile': '?0',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.104 Safari/537.36',
    'Sec-Fetch-Site': 'same-origin',
    'Sec-Fetch-Mode': 'cors',
    'Sec-Fetch-Dest': 'empty',
    'Referer': 'https://data.stats.gov.cn/adv.htm?m=advquery&cn=E0103',
    'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8,zh-TW;q=0.7',
}

def get_tree_leaf(tree_id: str) -> Generator[Dict[str, str], None, None]:
    params = (
        ('m', 'findZbXl'),
        ('db', 'fsnd'),
        ('wd', 'zb'),
        ('treeId', tree_id),
    )

    response = requests.get_json('https://data.stats.gov.cn/adv.htm', headers=headers, params=params, verify=False)
    for x in response:
        yield {"name": x['name'], "id": x['id']}

def get_tree_mid(tree_id: Optional[str]=None) -> Generator[Dict[str, str], None, None]:
    """
    Get all columns tree
    """
    data = {
        'm': 'findZbXl',
        'wd': 'zb',
        'db': 'fsnd'
    }
    if tree_id is not None:
        data['treeId'] = tree_id

    response = requests.post_json('https://data.stats.gov.cn/adv.htm', headers=headers, data=data, verify=False)
    for child in response:
        if child['isParent']:
            for x in get_tree_mid(child['id']): yield x
        else:
            for x in get_tree_leaf(child['id']): yield x

def get_data_by_col(col_id: str) -> List[Dict]:
    """
    Get data by column id, return raw jsons
    @param col_id example: A020101
    """
    params = (
        ('m', 'QueryData'),
        ('dbcode', 'fsnd'),
        ('rowcode', 'reg'),
        ('colcode', 'sj'),
        ('wds', json.dumps([{"wdcode": "zb", "valuecode": col_id}])),
        ('dfwds', '[]'),
        ('k1', get_timestamp()),
        ('h', '1'),
    )
    response = requests.get_json('https://data.stats.gov.cn/easyquery.htm', headers=headers, params=params, verify=False)
    return response["returndata"]

# 1. 爬取所有字段名和字段id
columns = list(get_tree_mid())
json.dump(columns, open("columns.json", "w", encoding='utf-8'), ensure_ascii=False, indent=2)
# 2. 爬取所有字段对应的表格
columns = json.load(open("columns.json", encoding="utf-8"))
out = []
for column in columns:
    out = {
        **column,
        'data': get_data_by_col(column['id']),
    }
    json.dump(out, open(f"output/{column['id']}.json", "w", encoding='utf-8'), ensure_ascii=False, indent=2)
