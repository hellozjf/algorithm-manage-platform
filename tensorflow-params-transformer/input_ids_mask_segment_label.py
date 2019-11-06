import csv
import os
import pickle
import re
import json

import collections
import jieba
import numpy as np
import pandas as pd
import tensorflow as tf
from flask import Flask
from flask import jsonify
from flask import request

import tokenization

app = Flask(__name__)

global INPUT_QUESTION
global INPUT_ANSWER
CANDIDATE_CSV = './data/tmp/candidate.csv'
CANDIDATE_P = './data/tmp/candidate.p'


class DataProcessor(object):
    """Base class for data converters for sequence classification data sets."""

    def get_train_examples(self, data_dir):
        """Gets a collection of `InputExample`s for the train set."""
        raise NotImplementedError()

    def get_dev_examples(self, data_dir):
        """Gets a collection of `InputExample`s for the dev set."""
        raise NotImplementedError()

    def get_test_examples(self, data_dir):
        """Gets a collection of `InputExample`s for prediction."""
        raise NotImplementedError()

    def get_labels(self):
        """Gets the list of labels for this data set."""
        raise NotImplementedError()

    @classmethod
    def _read_tsv(cls, input_file, quotechar=None):
        """Reads a tab separated value file."""
        with tf.gfile.Open(input_file, "r") as f:
            reader = csv.reader(f, delimiter="\t", quotechar=quotechar)
            lines = []
            for line in reader:
                lines.append(line)
            return lines


class InputExample(object):
    """A single training/test example for simple sequence classification."""

    def __init__(self, guid, text_a, text_b=None, label=None):
        """Constructs a InputExample.

        Args:
          guid: Unique id for the example.
          text_a: string. The untokenized text of the first sequence. For single
            sequence tasks, only this sequence must be specified.
          text_b: (Optional) string. The untokenized text of the second sequence.
            Only must be specified for sequence pair tasks.
          label: (Optional) string. The label of the example. This should be
            specified for train and dev examples, but not for test examples.
        """
        self.guid = guid
        self.text_a = text_a
        self.text_b = text_b
        self.label = label


class myprocess(DataProcessor):
    def get_train_examples(self, data_dir):
        """Gets a collection of `InputExample`s for the train set."""
        return self.create_examples(
            self._read_tsv(os.path.join(data_dir, "train.tsv")), "train")

    def get_dev_examples(self, data_dir):
        """Gets a collection of `InputExample`s for the dev set."""
        return self.create_examples(
            self._read_tsv(os.path.join(data_dir, "dev.tsv")), "dev")

    def get_test_examples(self, data_dir):
        """Gets a collection of `InputExample`s for prediction."""
        return self.create_examples(
            self._read_tsv(os.path.join(data_dir, "test.tsv")), "test")

    def get_labels(self):
        """See base class."""
        return ["0", "1"]

    def create_examples(self, lines, set_type, file_base=True):
        """Creates examples for the training and dev sets. each line is label+\t+text"""
        examples = []
        print(lines)
        for i, line in enumerate(lines):
            if file_base:
                if i == 0:
                    continue
            guid = "%s-%s" % (set_type, i)
            text = tokenization.convert_to_unicode(line[1])
            if set_type == "test" or set_type == "pred":
                label = "0"
            else:
                label = tokenization.convert_to_unicode(line[0])
            examples.append(
                InputExample(guid=guid, text_a=text, label=label))
        return examples


def convert_single_example(ex_index, example, label_list, max_seq_length, tokenizer):
    """Converts a single `InputExample` into a single `InputFeatures`."""

    label_map = {}
    for (i, label) in enumerate(label_list):
        label_map[label] = i

    tokens_a = tokenizer.tokenize(example.text_a)
    tokens_b = None
    if example.text_b:
        tokens_b = tokenizer.tokenize(example.text_b)

    if len(tokens_a) > max_seq_length - 2:
        tokens_a = tokens_a[0:(max_seq_length - 2)]

    tokens = []
    segment_ids = []
    tokens.append("[CLS]")
    segment_ids.append(0)
    for token in tokens_a:
        tokens.append(token)
        segment_ids.append(0)
    tokens.append("[SEP]")
    segment_ids.append(0)

    if tokens_b:
        for token in tokens_b:
            tokens.append(token)
            segment_ids.append(1)
        tokens.append("[SEP]")
        segment_ids.append(1)

    input_ids = tokenizer.convert_tokens_to_ids(tokens)

    # The mask has 1 for real tokens and 0 for padding tokens. Only real
    # tokens are attended to.
    input_mask = [1] * len(input_ids)

    # Zero-pad up to the sequence length.
    while len(input_ids) < max_seq_length:
        input_ids.append(0)
        input_mask.append(0)
        segment_ids.append(0)

    assert len(input_ids) == max_seq_length
    assert len(input_mask) == max_seq_length
    assert len(segment_ids) == max_seq_length

    label_id = label_map[example.label]
    print("input_ids", input_ids)
    print("input_mask", input_mask)
    print('segment_ids', segment_ids)
    print('label_id', label_id)

    return {
        'input_ids': input_ids,
        'input_mask': input_mask,
        'segment_ids': segment_ids,
        'label_id': label_id
    }


def convert_single_example2(ex_index, example, label_list, max_seq_length, tokenizer):
    """Converts a single `InputExample` into a single `InputFeatures`."""

    label_map = {}
    for (i, label) in enumerate(label_list):
        label_map[label] = i

    tokens_a = tokenizer.tokenize(example['text_a'])

    if len(tokens_a) > max_seq_length - 2:
        tokens_a = tokens_a[0:(max_seq_length - 2)]

    tokens = []
    segment_ids = []
    tokens.append("[CLS]")
    segment_ids.append(0)
    for token in tokens_a:
        tokens.append(token)
        segment_ids.append(0)
    tokens.append("[SEP]")
    segment_ids.append(0)

    input_ids = tokenizer.convert_tokens_to_ids(tokens)

    # The mask has 1 for real tokens and 0 for padding tokens. Only real
    # tokens are attended to.
    input_mask = [1] * len(input_ids)

    # Zero-pad up to the sequence length.
    while len(input_ids) < max_seq_length:
        input_ids.append(0)
        input_mask.append(0)
        segment_ids.append(0)

    assert len(input_ids) == max_seq_length
    assert len(input_mask) == max_seq_length
    assert len(segment_ids) == max_seq_length

    label_id = label_map[example['label']]
    print("input_ids", input_ids)
    print("input_mask", input_mask)
    print('segment_ids', segment_ids)
    print('label_ids', label_id)

    return {
        'input_ids': input_ids,
        'input_mask': input_mask,
        'segment_ids': segment_ids,
        'label_ids': label_id
    }


# 中文分词
def chinese_tokenizer(seq, stop_words):
    """中文分词。"""
    seq = jieba.lcut(seq)
    seq = filter(lambda x: x not in stop_words, seq)
    seq = " ".join(seq)
    return seq


# 英文分词
def tokenizer_fn(iterator):
    return (x.split(" ") for x in iterator)


def cosine(x, y):
    """计算余弦相似度。
    x, y shape (batch_size, vector_size).
    """
    sum_xy = np.matmul(x, y.T)
    normalize_x = np.expand_dims(np.sqrt(np.sum(x * x, 1)), 1)
    normalize_y = np.expand_dims(np.sqrt(np.sum(y * y, 1)), 1).T

    cosine_score = np.divide(sum_xy, np.matmul(normalize_x, normalize_y) + 1e-8)

    return cosine_score


def word2id(vocab_dict, token, max_length=120):
    """将词转换为ID表示。"""
    ids = [vocab_dict.get(i, 0) for i in token.split()]

    while len(ids) < max_length:
        ids.append(0)

    if len(ids) > max_length:
        ids = ids[:max_length]

    assert len(ids) == max_length
    return ids


def ap_pre_processing(stop_words_file, vocab_file):
    """ap-bilstm数据预处理。"""
    # 加载停用词
    stopwords = pd.read_csv(stop_words_file,
                            index_col=False,
                            quoting=3,
                            sep="\t",
                            names=["stopword"],
                            encoding="utf-8")
    stopwords = stopwords["stopword"].values

    # 加载词典
    with open(vocab_file, encoding="utf-8") as f:
        vocab_list = list(map(lambda x: x.strip(), f.readlines()))
    vocab_dict = {w: i for i, w in enumerate(vocab_list)}

    # 构造输入数据
    answer_token = chinese_tokenizer(INPUT_ANSWER, stopwords)
    answer_len = [len(answer_token.split(" "))]
    answer = word2id(vocab_dict, answer_token)

    question_token = chinese_tokenizer(INPUT_QUESTION, stopwords)
    question_len = [len(question_token.split(" "))]
    question = word2id(vocab_dict, question_token)

    return question, question_len, answer, answer_len


def ap_post_processing(question_tensor, answer_dict_file):
    """ap-bilstm数据后处理。"""

    # 加载答案字典
    answer_dict = pickle.load(open(answer_dict_file, mode="rb"))

    # 答案矩阵
    answer_tensor = [v[4] for v in answer_dict.values()]
    shape = answer_tensor[0].shape[0]
    answer_matrics = np.array(answer_tensor).reshape(-1, shape)

    # 处理模型输出
    question_tensor = np.asarray(question_tensor["predictions"])
    shape = question_tensor[0].shape[0]
    question_matrics = np.array(question_tensor).reshape(-1, shape)

    # 计算余弦相似度
    prob = cosine(question_matrics, answer_matrics)

    # 输出候选答案集
    probs, q, a, hdid, voice_number = [], [], [], [], []
    q_tensor, a_tensor = [], []
    result = [(x, y) for x, y in zip(prob[0], answer_dict.values())]
    for p, r in sorted(result, reverse=True)[:10]:
        probs.append(p)
        hdid.append(r[0])
        voice_number.append(r[1])
        q.append(r[2])
        q_tensor.append(question_tensor[0])
        a.append(r[3])
        a_tensor.append(r[4])

    candidate_df = pd.DataFrame({
        "HDID": hdid,
        "voice_number": voice_number,
        "question": q,
        "answer": a})
    candidate_df.to_csv(CANDIDATE_CSV, index=False)

    candidate_dataset = collections.OrderedDict()
    for idx, row in enumerate(zip(probs, q_tensor, a_tensor)):
        candidate_dataset[idx] = row
    pickle.dump(candidate_dataset, open(CANDIDATE_P, 'wb'))


def pre_processing(pickle_file):
    """reranking数据预处理。"""
    # 候选答案集字典
    dataset_dict = pickle.load(open(pickle_file, mode="rb"))

    # 构造输入数据
    probability_list, question_list, answer_list = [], [], []
    for row in dataset_dict.values():
        probability_list.append(row[0])
        question_list.append(row[1].tolist())
        answer_list.append(row[2].tolist())

    return probability_list, question_list, answer_list


def post_processing(probs, csv_file):
    """reranking数据后处理。"""

    # 候选答案的DataFrame
    dataset_df = pd.read_csv(csv_file)
    hdid = dataset_df['HDID'].values.tolist()
    voice_number = dataset_df['voice_number'].values.tolist()
    question = dataset_df['question'].values.tolist()
    answer = dataset_df['answer'].values.tolist()

    # 处理模型输出
    prob = [x[0] for x in probs["predictions"]]
    result = [x for x in zip(prob, hdid, voice_number, question, answer)]
    output = sorted(result, reverse=True)[:1]

    return output


def get_sim(v1,v2):
    return np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2))


def get_result(extend_em,stand_em_dict):
    k_values={key:get_sim(extend_em,v) for key,v in stand_em_dict.items()}
    result=sorted(k_values.items(),key=lambda x:x[1],reverse=True)[0][0]
    return result


def deployment(pk, text):
    with open(pk,'rb') as f:
        stand_em_dict=pickle.load(f)
    array = json.loads(text)
    return get_result(array,stand_em_dict)


def get(x):
    return map_dict[x]


def predictString(csv, text):
    data = pd.read_csv(csv)
    map_dict = {i: j for i, j in zip(data['standard_question'], data['answer'])}
    if text in map_dict.keys():
        return map_dict[text]
    else:
        return ''


# 生成参数
@app.route('/getParams', methods=['POST'])
def getParams():
    sentence = request.form['sentence']
    paramCode = request.form['paramCode']
    other = request.form['other']
    maxLength = request.form['maxLength']

    if int(paramCode) == 104:
        # 参考ModelParamEnum.java，104是ap-bilstm，是另一个独立的处理方式
        global INPUT_QUESTION
        global INPUT_ANSWER
        if not other:
            # 说明是数据预处理
            INPUT_QUESTION = sentence
            INPUT_ANSWER = '增值税发票系统升级版纳税人端税控设备包括金税盘和税控盘。'
            # ap-bilstm数据预处理
            question, question_len, answer, answer_len = ap_pre_processing(
                "./data/stopwords.txt", "./data/total_vocabulary.txt")
            result = {
                'question': question,
                'question_len': question_len,
                'answer': answer,
                'answer_len': answer_len
            }
            return jsonify(result)
        else:
            # 说明是后处理
            INPUT_QUESTION = sentence
            # ap-bilstm数据后处理，得到候选答案
            ap_post_processing(eval(other), "./data/total_answer_dict.p")
            return ''
    elif int(paramCode) == 110:
        # 参考ModelParamEnum.java，110是reranking，是另一个独立的处理方式
        if not other:
            # 说明是数据预处理
            # reranking数据预处理
            probability, question_, answer_ = pre_processing(CANDIDATE_P)
            result = {
                'probability': probability,
                'question_': question_,
                'answer_': answer_,
            }
            return jsonify(result)
        else:
            # 说明是后处理
            # reranking数据后处理
            output = post_processing(eval(other), CANDIDATE_CSV)
            return jsonify(output)
    elif int(paramCode) == 111:
        if other == 'rawQuestion':
            return deployment('data/bert_match/stand_em_.pk', sentence)
        elif other == 'predictString':
            return predictString('data/bert_match/train_set.csv', sentence)
    else:
        if int(paramCode) == 102:
            # 参考ModelParamEnum.java，102是情感分析模型，需要去除标点
            sentence = re.sub('\W', '', sentence)
        elif int(paramCode) == 105 or int(paramCode) == 106 or int(paramCode) == 107 or int(paramCode) == 108:
            # 参考ModelParamEnum.java
            # 105是社保模型，要去停词
            # 106是三分类模型，要去停词
            with open('./data/stopword/stopWord.txt', 'r', encoding='utf_8_sig') as f:
                stopword = [i.strip() for i in f.readlines()]
                word = []
                for i in jieba.lcut(sentence):
                    if i in stopword:
                        continue
                    word.append(i)
                sentence = ''.join(word)

                print('sentence:', sentence)
                example = {
                    'label': '0',
                    'text_a': sentence
                }

                max_seq_length = int(maxLength);
                vocab_file = "vocab.txt"
                tokenizer = tokenization.FullTokenizer(vocab_file=vocab_file, do_lower_case=True)
                label_list = ['0']
                result = convert_single_example2('0', example, label_list, max_seq_length, tokenizer)

                return jsonify(result)

        print('sentence:', sentence)
        example = {
            'label': '0',
            'text_a': sentence
        }

        max_seq_length = int(maxLength)
        vocab_file = "vocab.txt"
        tokenizer = tokenization.FullTokenizer(vocab_file=vocab_file, do_lower_case=True)
        label_list = ['0']
        result = convert_single_example2('0', example, label_list, max_seq_length, tokenizer)

        return jsonify(result)


# 开启restful服务
if __name__ == "__main__":
    app.run(host='0.0.0.0')
