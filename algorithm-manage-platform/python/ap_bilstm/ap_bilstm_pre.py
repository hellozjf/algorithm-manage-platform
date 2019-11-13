import pandas as pd
import sys
import jieba
import json


# 中文分词
def chinese_tokenizer(seq, stop_words):
    """中文分词。"""
    seq = jieba.lcut(seq)
    seq = filter(lambda x: x not in stop_words, seq)
    seq = " ".join(seq)
    return seq


def word2id(vocab_dict, token, max_length=120):
    """将词转换为ID表示。"""
    ids = [vocab_dict.get(i, 0) for i in token.split()]

    while len(ids) < max_length:
        ids.append(0)

    if len(ids) > max_length:
        ids = ids[:max_length]

    assert len(ids) == max_length
    return ids


def ap_pre_processing(stop_words_file, vocab_file, input_question, input_answer):
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
    answer_token = chinese_tokenizer(input_answer, stopwords)
    answer_len = [len(answer_token.split(" "))]
    answer = word2id(vocab_dict, answer_token)

    question_token = chinese_tokenizer(input_question, stopwords)
    question_len = [len(question_token.split(" "))]
    question = word2id(vocab_dict, question_token)

    return question, question_len, answer, answer_len


def main(argv):
    sentence = argv[1]
    stopword_path = argv[2]
    total_vocabulary_path = argv[3]
    question, question_len, answer, answer_len = ap_pre_processing(
        stopword_path,
        total_vocabulary_path,
        sentence,
        '增值税发票系统升级版纳税人端税控设备包括金税盘和税控盘。'
    )
    result = {
        'question': question,
        'question_len': question_len,
        'answer': answer,
        'answer_len': answer_len
    }
    jsonString = json.dumps(result)
    print(jsonString)


# 用法
# python ap_bilstm_pre.py 增值税的税率是多少 stopWord.txt total_vocabulary.txt
if __name__ == "__main__":
    main(sys.argv)

