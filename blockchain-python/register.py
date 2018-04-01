#!/usr/bin/env python
# -*- coding:utf-8 -*-
import json
from time import time

import requests

if __name__ == "__main__":
    host = "127.0.0.1"
    port1 = 5000
    port2 = 5001

    url1 = 'http://%s:%s/nodes/register' % (host, port1)
    data = {"nodes": "http://%s:%s" % ((host, port1))}
    response = requests.post(url1, data=json.dumps(data))
    print(response.text)

    url2 = 'http://%s:%s/nodes/register' % (host, port2)
    response = requests.post(url2, data=json.dumps(data))
    print(response.text)

    print(type(time()))
