import sys
import pandas as pd
import pickle
import json
import numpy as np


def ap_post_processing(sentence, question_tensor, answer_dict_file, candidate_csv, candidate_p):
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
    candidate_df.to_csv(candidate_csv, index=False)

    candidate_dataset = collections.OrderedDict()
    for idx, row in enumerate(zip(probs, q_tensor, a_tensor)):
        candidate_dataset[idx] = row
    pickle.dump(candidate_dataset, open(candidate_p, 'wb'))


def main(argv):
    sentence = argv[1]
    other = argv[2]
    total_answer_dict_path = argv[3]
    candidate_csv_path = argv[4]
    candidate_p_path = argv[5]
    ap_post_processing(sentence, eval(other), total_answer_dict_path, candidate_csv_path, candidate_p_path)
    print('')


if __name__ == "__main__":
    main(sys.argv)
