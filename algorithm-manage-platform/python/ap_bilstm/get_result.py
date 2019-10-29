import pandas as pd
import sys


def get(x):
    return map_dict[x]


def main(argv):
    data = pd.read_csv(argv[1])
    map_dict = {i: j for i, j in zip(data['standard_question'], data['answer'])}
    print(map_dict[argv[2]])


if __name__ == "__main__":
    main(sys.argv)
