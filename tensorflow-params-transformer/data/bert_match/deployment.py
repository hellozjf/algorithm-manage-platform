import pickle
import numpy as np
import sys
import json

def get_sim(v1,v2):
    return np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2))


def get_result(extend_em,stand_em_dict):
    k_values={key:get_sim(extend_em,v) for key,v in stand_em_dict.items()}
    result=sorted(k_values.items(),key=lambda x:x[1],reverse=True)[0][0]
    return result


def main(argv):
    with open(argv[1],'rb') as f:
        stand_em_dict=pickle.load(f)
    array = json.loads(argv[2])
    print(get_result(array,stand_em_dict))


if __name__ == "__main__":
    main(sys.argv)

