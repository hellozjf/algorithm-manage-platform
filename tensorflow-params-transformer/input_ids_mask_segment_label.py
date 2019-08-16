import csv
import os
import pickle
import re

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
    seq = jieba.lcut(seq)
    seq = filter(lambda x: x not in stop_words, seq)
    seq = " ".join(seq)
    return seq


# 英文分词
def tokenizer_fn(iterator):
    return (x.split(" ") for x in iterator)


# 余弦相似度
def cosine(x, y):
    """x, y shape (batch_size, vector_size)"""
    sum_xy = np.matmul(x, y.T)
    normalize_x = np.expand_dims(np.sqrt(np.sum(x * x, 1)), 1)
    normalize_y = np.expand_dims(np.sqrt(np.sum(y * y, 1)), 1).T

    cosine_score = np.divide(sum_xy, np.matmul(normalize_x, normalize_y) + 1e-8)

    return cosine_score


def pre_processing(stop_words_file, vocab_processor_file):

    global INPUT_QUESTION
    global INPUT_ANSWER

    # 加载停用词
    stopwords = pd.read_csv(stop_words_file,
                            index_col=False,
                            quoting=3,
                            sep="\t",
                            names=['stopword'],
                            encoding='utf-8')
    stopwords = stopwords['stopword'].values

    # 加载词典
    vp = tf.contrib.learn.preprocessing.VocabularyProcessor.restore(
        vocab_processor_file)

    input_questions = INPUT_QUESTION.split(' ')

    # 构造输入数据
    answer_token = chinese_tokenizer(INPUT_ANSWER, stopwords)
    answer_len = [len(answer_token.split(" "))]
    answer = list(vp.transform([answer_token]))[0].tolist()

    question_token = chinese_tokenizer(input_questions[0], stopwords)
    question_len = [len(question_token.split(" "))]
    question = list(vp.transform([question_token]))[0].tolist()

    return question, question_len, answer, answer_len


def post_processing(question_tensor, answer_dict_file, answer_matrics_file):
    input_questions = INPUT_QUESTION.split(' ')

    # 加载答案字典与答案矩阵
    answer_dict = pickle.load(open(answer_dict_file, mode='rb'))
    answer_matrics = pickle.load(open(answer_matrics_file, mode='rb'))

    # 处理模型输出
    question_matrics = np.asarray(question_tensor['predictions'])
    shape = question_matrics[0].shape[0]
    question_matrics = np.array(question_matrics).reshape(-1, shape)

    # 计算余弦相似度
    prob = cosine(question_matrics, answer_matrics)

    # 输出置信度，HDID, 语音编号，问题，答案
    probs, hdid, voice_number, q, a = [], [], [], [], []
    for idx, i in enumerate(prob):
        result = [(x, y) for x, y in zip(i, answer_dict.values())]
        for p, r in sorted(result, reverse=True)[:100]:
            probs.append(p)
            hdid.append(r[0])
            voice_number.append(r[1])
            q.append(input_questions[idx])
            a.append(r[2])

    return probs, hdid, voice_number, q, a


@app.route('/getParams', methods=['POST'])
def getParams():
    sentence = request.form['sentence']
    paramCode = request.form['paramCode']
    other = request.form['other']

    if int(paramCode) == 104:
        # 参考ModelParamEnum.java，104是问答分析模型，是另一个独立的处理方式
        global INPUT_QUESTION
        global INPUT_ANSWER
        if other:
            # 说明是后处理
            INPUT_QUESTION = sentence
            probs, hdid, voice_number, q, a = post_processing(eval(other), './data/answer_dict.p', './data/answer_matrics.p')
            result = []
            for i in zip(probs, hdid, voice_number, q, a):
                result.append({
                    'probs': i[0],
                    'hdid': i[1],
                    'voice_number': i[2],
                    'q': i[3],
                    'a': i[4]
                })
            return jsonify(result)
        else:
            # 说明是数据预处理
            INPUT_QUESTION = sentence
            INPUT_ANSWER = '增值税发票系统升级版纳税人端税控设备包括金税盘和税控盘。'
            question, question_len, answer, answer_len = pre_processing(
                './data/stopwords.txt', './data/vocab_processor.bin')
            result = {
                'question': question,
                'question_len': question_len,
                'answer': answer,
                'answer_len': answer_len
            }
            return jsonify(result)

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

                if int(paramCode) == 105:
                    max_seq_length = 512
                elif int(paramCode) == 106 or int(paramCode) == 107 or int(paramCode) == 108:
                    max_seq_length = 300
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

        max_seq_length = 128
        vocab_file = "vocab.txt"
        tokenizer = tokenization.FullTokenizer(vocab_file=vocab_file, do_lower_case=True)
        label_list = ['0']
        result = convert_single_example2('0', example, label_list, max_seq_length, tokenizer)

        return jsonify(result)


if __name__ == "__main__":
    app.run(host='0.0.0.0')
    # processor = myprocess()
    # predict_examples = processor.get_dev_examples('data')
    # max_seq_length=128
    # vocab_file="vocab.txt"
    # tokenizer = tokenization.FullTokenizer(vocab_file=vocab_file, do_lower_case=True)
    # label_list=['0','1']
    # for (ex_index, example) in enumerate(predict_examples):
    #     feature = convert_single_example(ex_index, example, label_list, max_seq_length, tokenizer)
