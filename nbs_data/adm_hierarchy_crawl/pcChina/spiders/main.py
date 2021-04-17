import json
import scrapy
import re
from typing import List
from scrapy.shell import inspect_response

def get_path(url: str) -> List[str]:
    url = url.replace('.html', '')
    urls = url.split('/')
    index = urls.index('2020')
    path = urls[index + 1:]
    return path

class MainSpider(scrapy.Spider):
    name = 'main'
    allowed_domains = ['stats.gov.cn']
    start_urls = ['http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2020/index.html']
    
    def parse(self, response):
        for nxt in response.css('td a').getall():
            node = nxt
            name = re.search('^<a(.+)>(.+)</a>$', node).group(2).strip().replace('<br>', '')
            if not re.search('[\u4e00-\u9fff]', name):
                continue
            href = re.search('href="([^"]+)"', node).group(1)
            yield scrapy.Request(response.urljoin(href), callback=self.parse, cookies={'SF_cookie_1': '15502425'})
        good = False
        for row in response.xpath('//tr[@class="provincetr"]'):
            good = True
            for node in row.xpath('td/a'):
                code = node.xpath('@href').extract_first().split('.')[0]
                name = node.xpath('text()').extract_first()
                yield {'type': 'PROVINCE', 'code': code, 'name': name, 'path': get_path(response.url)}
        for row in response.xpath('//tr[@class="citytr"]'):
            code = row.xpath('td[1]/*/text()').extract_first()
            name = row.xpath('td[2]/*/text()').extract_first()
            good = True
            yield {'type': 'CITY', 'code': code, 'name': name, 'path': get_path(response.url)}
        for row in response.xpath('//tr[@class="countytr"]'):
            code = row.xpath('td[1]/*/text()').extract_first()
            name = row.xpath('td[2]/*/text()').extract_first()
            good = True
            yield {'type': 'COUNTY', 'code': code, 'name': name, 'path': get_path(response.url)}
        for row in response.xpath('//tr[@class="towntr"]'):
            code = row.xpath('td[1]/*/text()').extract_first()
            name = row.xpath('td[2]/*/text()').extract_first()
            good = True
            yield {'type': 'TOWN', 'code': code, 'name': name, 'path': get_path(response.url)}
        for row in response.xpath('//tr[@class="villagetr"]'):
            code = row.xpath('td[1]/text()').extract_first()
            code2 = row.xpath('td[2]/text()').extract_first()
            name = row.xpath('td[3]/text()').extract_first()
            good = True
            yield {'type': 'VILLAGE', 'code': code, 'code2': code2, 'name': name, 'path': get_path(response.url)}
        if not good:
            print('FAILED')
            yield {'type': 'FAILED', 'url': response.url}