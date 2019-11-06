import pandas as pd
import sys


def get(x):
    return map_dict[x]


def main(argv):
    data = pd.read_csv(argv[1])
    map_dict = {i: j for i, j in zip(data['standard_question'], data['answer'])}
    if argv[2] in map_dict.keys():
        print(map_dict[argv[2]])
    else:
        print('')


if __name__ == "__main__":
    main(sys.argv)
