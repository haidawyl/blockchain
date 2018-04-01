#!/usr/bin/env python
# -*- coding:utf-8 -*-

import hashlib
import json
import os
import threading
from time import time
from urllib.parse import urlparse, urlencode

import requests
from flask import Flask, jsonify, request, session, render_template

data_dir = "/opt/blockchain"


class Blockchain(object):
    def __init__(self):
        self.chain = [] # 存储区块链
        self.current_transactions = [] # 存储交易
        self.nodes = set()
        self.init_chain() # 初始化区块链

    def init_chain(self):
        # 读取本地存储的区块链
        if len(self.chain) == 0:
            try:
                f = open(data_dir + "/data.txt", "r", encoding="utf-8")
                for line in f.readlines():
                    items = line.split("#")
                    self.current_transactions = []
                    transaction = {
                        'sender': items[4],
                        'recipient': items[5],
                        'amount': int(items[6]),
                    }
                    self.current_transactions.append(transaction)
                    block = {
                        'index': int(items[0]),
                        'timestamp': float(items[3]),
                        'transactions': self.current_transactions,
                        'proof': items[1],
                        'previous_hash': items[2],
                    }
                    self.chain.append(block)
                f.close()
            except:
                pass

        # 遍历全网下载链长最长且有效的区块链
        self.resolve_conflicts()

        # 创建创世快
        if len(self.chain) == 0:
            self.new_block(proof="海阔天空", previous_hash="*")
            # self.new_transaction(sender="*", recipient="*", amount=1)
            if not os.path.exists(data_dir):
                os.mkdir(data_dir)
            self.write()

    def new_block(self, proof, previous_hash=None):
        """
        创建新区块
        :param proof: 工作量
        :param previous_hash: 前一个区块的 hash 值
        :return: <dict> 新区块
        """
        block = {
            'index': len(self.chain) + 1, # 索引
            'timestamp': time(), # 时间戳
            'transactions': self.current_transactions, # 交易列表
            'proof': proof, # 工作量证明
            'previous_hash': previous_hash or self.hash(self.chain[-1]), # 前一个区块的 hash 值
        }

        self.current_transactions = [] # 清空当前的交易列表
        self.chain.append(block) # 新区块添加到链尾
        return block

    def new_transaction(self, sender, recipient, amount):
        """
        生成新的交易记录, 将其加入到下一个区块中
        :param sender: <str> 发送者
        :param recipient: <str> 接受者
        :param amount: <int> 数量
        :return: <int> 记录此交易的区块索引
        """
        self.current_transactions.append({
            'sender': sender,
            'recipient': recipient,
            'amount': amount,
        })
        return self.last_block['index'] + 1

    @staticmethod
    def hash(block):
        """
        生成块的 SHA-256 hash值
        :param block: <dict> Block
        :return: <str>
        """
        # 确保块字典是排好序的, 否则不能得到一致性hash值
        block_string = json.dumps(block, sort_keys=True).encode()
        return hashlib.sha256(block_string).hexdigest()

    @property
    def last_block(self):
        return self.chain[-1]

    @staticmethod
    def valid_proof(last_proof, proof):
        """
        验证工作量证明
        :param last_proof: <int> 前一个工作量
        :param proof: <int> 当前工作量
        :return: <bool> True:有效 , False:无效.
        """
        params = {
            'q': proof,
            't': 'ChengYu'
        }
        # 验证是否是成语
        url = "http://chengyu.t086.com/chaxun.php?" + urlencode(params, encoding="GBK")
        # print("url:%s" % (url))
        try:
            r = requests.get(url, timeout=10)
            r.raise_for_status()
            r.encoding = "GBK"
            content = r.text
        except:
            content = None

        if content is None or content.find("没有找到与您搜索相关的成语") > -1 or content.find("搜索词太长") > -1:
            return False

        # 验证是否满足成语接龙的规则
        return last_proof[-1] == proof[0]

    def register_node(self, address):
        """
        注册新的节点
        :param address: <str> 节点地址. Eg. 'http://192.168.0.5:5000'
        """
        parsed_url = urlparse(address)
        self.nodes.add(parsed_url.netloc)

    def valid_chain(self, chain):
        """
        验证区块链的有效性
        :param chain: <list> 待验证的区块链
        :return: <bool> True:有效, False:无效
        """
        last_block = chain[0]
        current_index = 1
        while current_index < len(chain):
            block = chain[current_index]
            print(f'{last_block}')
            print(f'{block}')
            print("\n-----------\n")
            # 验证块的hash值是否正确
            if block['previous_hash'] != self.hash(last_block):
                return False
            # 验证工作量是否有效
            if not self.valid_proof(last_block['proof'], block['proof']):
                return False
            last_block = block
            current_index += 1
        return True

    def resolve_conflicts(self):
        """
        一致性算法解决冲突
        使用网络中最长的链.
        :return: <bool> True:链被取代, False:链未被取代
        """
        new_chain = self.chain
        # 寻找全网链长最大的区块链
        max_length = len(self.chain)
        replaced = False
        # 遍历全网所有节点获取区块链
        # for node in self.nodes:
        for i in range(6, 10):
            try:
                # response = requests.get(f'http://{node}/gainBlockChain', timeout=30)
                response = requests.get(f'http://192.168.0.{i}:5000/gainBlockChain', timeout=30)
                if response.status_code == 200:
                    length = response.json()['length']
                    chain = response.json()['chain']
                    # 链长大于当前最大且有效的区块链
                    if length > max_length and self.valid_chain(chain):
                        max_length = length
                        new_chain = chain
                        replaced = True
                    # 链长等于当前最大且有效的区块链, 对比最后一个block的创建时间
                    elif length == max_length and self.valid_chain(chain):
                        if float(chain[-1]["timestamp"]) < float(new_chain[-1]["timestamp"]):
                            new_chain = chain
                            replaced = True
            except Exception as e:
                print(e)

        # 如果发现新的链长大于自己且有效的区块链则替换自己的
        if replaced:
            self.chain = new_chain
            self.write()
        return replaced

    def write(self):
        '''
        将区块链信息写入本地磁盘
        :return:
        '''
        f = open(data_dir + "/data.txt", "w", encoding="utf-8")
        for block in self.chain:
            f.write(str(block["index"]))
            f.write("#")
            f.write(block["proof"])
            f.write("#")
            f.write(str(block["previous_hash"]))
            f.write("#")
            f.write(str(block["timestamp"]))
            f.write("#")
            transactions = block["transactions"]
            if transactions and len(transactions) > 0:
                f.write(transactions[-1]["sender"])
                f.write("#")
                f.write(transactions[-1]["recipient"])
                f.write("#")
                f.write(str(transactions[-1]["amount"]))
            else:
                f.write("*#*#1")
            f.write("\n")
        f.close()


# 创建我们的节点
app = Flask(__name__)
app.config['SECRET_KEY'] = os.urandom(32)
blockchain = Blockchain()


@app.route('/')
def index():
    return render_template('index.html')


@app.route('/chain', methods=['POST'])
def chain():
    phone = request.form['phone']
    if phone is None or phone == "":
        return '缺少参数[phone]', 400
    session["phone"] = phone
    blockchain.resolve_conflicts()

    proofs = [block["proof"] for block in blockchain.chain]

    return render_template('chain.html', phone=phone, proofs=proofs)


@app.route('/register')
def register():
    return render_template('register.html')


@app.route('/gainBlockChain', methods=['GET'])
def full_chain():
    response = {
        'chain': blockchain.chain,
        'length': len(blockchain.chain),
    }
    return jsonify(response), 200


@app.route('/mine', methods=['POST'])
def mine():
    blockchain.resolve_conflicts()

    last_block = blockchain.last_block
    last_proof = last_block['proof']

    proof = request.form['answer'].strip()
    if last_proof[-1] != proof[0]:
        proof_chain = ""
        for block in blockchain.chain:
            proof_chain += block["proof"] + " -> "
        response = {
            'message': "非常遗憾！你晚了一步，已经有其他用户回答了这个成语",
            'proof_chain': proof_chain
        }
        return jsonify(response), 200

    if blockchain.valid_proof(last_proof, proof):
        blockchain.new_transaction(
            sender="*",  # "*" 表示新挖出的币
            recipient=session["phone"],
            amount=1,
        )
        # 在区块链的末尾加入新块
        block = blockchain.new_block(proof)
        blockchain.write()

        proof_chain = ""
        for block in blockchain.chain:
            proof_chain += block["proof"] + " -> "

        response = {
            'message': "恭喜你接龙成功！",
            'proof_chain': proof_chain
        }
    else:
        response = {
            'message': "你输入的不是成语，请重新输入..."
        }

    return jsonify(response), 200


@app.route('/transactions/new', methods=['POST'])
def new_transaction():
    values = request.get_json()
    # 检查请求中必需的属性
    required = ['sender', 'recipient', 'amount']
    if not all(k in values for k in required):
        return 'Missing values', 400
    # 创建新的交易信息
    index = blockchain.new_transaction(values['sender'], values['recipient'], values['amount'])
    response = {'message': f'Transaction will be added to Block {index}'}
    return jsonify(response), 201


@app.route('/nodes/register', methods=['POST'])
def register_nodes():
    nodes = request.form['nodes'].strip()
    if nodes is None:
        return "Error: Please supply a valid list of nodes", 400
    for node in nodes:
        blockchain.register_node(node)
    response = {
        'message': 'New nodes have been added',
        'total_nodes': list(blockchain.nodes),
    }
    return jsonify(response), 201


@app.route('/nodes/resolve', methods=['GET'])
def consensus():
    replaced = blockchain.resolve_conflicts()
    if replaced:
        response = {
            'message': 'Our chain was replaced',
            'new_chain': blockchain.chain
        }
    else:
        response = {
            'message': 'Our chain is authoritative',
            'chain': blockchain.chain
        }
    return jsonify(response), 200


class Deamon(threading.Thread):
    def __init__(self, host, port):
        threading.Thread.__init__(self)
        self.host = host
        self.port = port

    def run(self):
        app.run(host=self.host, port=self.port)


if __name__ == '__main__':
    # host = "127.0.0.1"
    # port1 = 5000
    # port2 = 5001
    #
    # thread1 = Deamon(host, port1)
    # thread1.setDaemon(True)
    # thread1.start()
    #
    # thread2 = Deamon(host, port2)
    # thread2.setDaemon(True)
    # thread2.start()
    # thread1.join()
    # thread1.join()

    app.run(host="127.0.0.1", port=5000)
