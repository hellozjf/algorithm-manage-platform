import json
import sys

import pandas as pd


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


def main(argv):
    other = argv[1]
    cadidate_csv = argv[2]
    # with open(other, 'r') as load_file:
    #     jsonObject = json.load(load_file)
    #     print(jsonObject)
    # output = post_processing(jsonObject, cadidate_csv)
    output = post_processing(eval(other), cadidate_csv)
    jsonString = json.dumps(output)
    print(jsonString)


# 用法
# python reranking_post.py "../tmp/other.json" "../tmp/candidate.csv"
if __name__ == "__main__":
    main(sys.argv)
