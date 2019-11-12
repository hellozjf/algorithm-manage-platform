import pickle
import json
import sys


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


def main(argv):
    candidate_p_path = argv[1]
    probability, question_, answer_ = pre_processing(candidate_p_path)
    result = {
        'probability': probability,
        'question_': question_,
        'answer_': answer_,
    }
    jsonString = json.dumps(result)
    print(jsonString)


# 用法
# python reranking_pre.py "../tmp/candidate.p"
if __name__ == "__main__":
    main(sys.argv)
