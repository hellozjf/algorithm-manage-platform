import numpy as np
import sys


def split_seq_list(x):
    """
    将[1,2,3,5,6,9]拆分成[[1,2,3],[5,6],[9]]
    :param x:
    :return:
    """
    return [list(i) for i in np.split(x, np.where(np.diff(x) != 1)[0] + 1)]


def sw_result(pre_label,test_data):
    """
    提取结果
    :param pre_label: 预测标签
    :param test_data: 实际文本数据
    :return:
    """
    pre_entity=[]
    for s_label,sentence in zip(pre_label,test_data):
        temp_index = []
        single_entity=[]
        for index,ssl in enumerate(s_label):
            if ssl==0 or ssl==1:#取出标记为0和1的
                temp_index.append(index-1)
        if len(temp_index)!=0:
            try:
                for ti in split_seq_list(temp_index):#按照index切分
                    single_entity.append(''.join([sentence[tti] for tti in ti]))
                single_entity_1=list(set(single_entity))#去重
                single_entity_1.sort(key=single_entity.index)
                pre_entity.append(' '.join(single_entity_1))
            except:
                #print(temp_index)
                pre_entity.append(None)
        if len(temp_index)==0:
            pre_entity.append(None)
    return pre_entity


def main(argv):
    # 先把输入向量按逗号隔开，成为字符串数组
    t = argv[1].split(',')
    # 再转换成int数组
    t = list(map(int, t))
    pre_label=[t]
    #增值税的税率是多少
    test_data=[argv[2]]
    print(sw_result(pre_label,test_data))


if __name__=="__main__":
    main(sys.argv)