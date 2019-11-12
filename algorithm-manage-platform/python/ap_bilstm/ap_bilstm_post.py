import sys
import pandas as pd
import pickle
import json
import numpy as np
import collections


def cosine(x, y):
    """计算余弦相似度。
    x, y shape (batch_size, vector_size).
    """
    sum_xy = np.matmul(x, y.T)
    normalize_x = np.expand_dims(np.sqrt(np.sum(x * x, 1)), 1)
    normalize_y = np.expand_dims(np.sqrt(np.sum(y * y, 1)), 1).T

    cosine_score = np.divide(sum_xy, np.matmul(normalize_x, normalize_y) + 1e-8)

    return cosine_score


def ap_post_processing(question_tensor, answer_dict_file, candidate_csv, candidate_p):
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
    other = argv[1]
    total_answer_dict_path = argv[2]
    candidate_csv_path = argv[3]
    candidate_p_path = argv[4]
    ap_post_processing(eval(other), total_answer_dict_path, candidate_csv_path, candidate_p_path)
    print('')


# 用法
# python ap_bilstm_post.py "{\"predictions\":[[-0.018027493140054581,0.011430047246020017,0.015589819428145073,0.016141098309636621,-0.0045847881323493149,-0.0033866025987932371,0.001934203097181412,0.0040374653329167658,0.0086101912513408436,0.0098123263228180966,0.0021058646881854802,0.01604723046149268,0.02230368513847809,0.019539453906694291,0.0094500153634804983,-0.021138867347236374,-0.02106869202563532,0.0050107171828010786,-0.0015104639428981583,-0.022800606100337222,-0.0034744518182706322,-0.01133925702868813,-0.019287969015861519,0.024435494369421749,-0.0019435927144390542,-0.009263981896568383,-0.011108875973695783,0.0047397867734437936,-0.016738272019997662,-0.013719851034970554,0.031594844756039506,-0.0047065240581902791,0.015641861827086833,-0.005062544438478242,-0.018315368531692085,-0.012226158423392773,-0.0029643573676705766,0.002856165885108934,-0.0087866650212266809,0.015586421834528821,0.022552019088528805,-0.0064300651122712327,8.0404623952070198e-05,-0.031934118707885852,0.003614389936639551,0.004308384550904678,0.0058965315669729356,0.0006324496807921169,-0.0034168175342143593,0.0068523306352136944,0.026157590005891337,0.0050716233697766441,0.0094546617171242346,0.014195649409833288,0.011149551083097044,0.027628148588372287,-0.0084339446996860353,0.0043953589085453902,0.0114451299943466,-0.006889282381063051,0.0081736070724112066,-0.022917420237414252,0.0099995500316730629,0.011936800126039523,0.011665926833415245,-0.010745069138769783,-0.013910607140909031,0.0085890351597392115,0.011927868543491114,-0.0011960814079635747,-0.011752923635890075,-0.021199859040625938,0.0043874746879665636,0.0063459110078029988,0.0091206911172596328,-0.0060583534520572153,-0.0062835950814511543,-0.01070358412009758,-0.013459100852486158,-0.00032242399212837258,0.0083534706504458066,-0.0050845367369290887,-0.0085620578236266769,-0.0059667177076073857,-0.002741207563414126,0.024893422841976938,0.026262548274520115,-0.0025990221282213661,-0.0081542163115645456,0.0066220194405790835,0.012337165014751688,-0.0031482997982167425,-0.019290370481929275,-0.0022928014366378415,0.0049714928925861466,0.009898635058381372,-0.0012031395969110325,-0.0022216959441507885,-0.019020845807088273,-0.008131068845044857,0.012060045080085385,-0.023346074974255317,0.010326409929086761,0.0045400192346873201,0.023508936660041398,0.017593433559310059,0.0050320658546276643,-0.006535683537568616,0.0014504326847231625,-0.0027781206608125707,0.03235378688293479,0.012301601774252467,-0.0023683472535658938,-0.0095374720130106897,-0.011431350649886566,0.0070722891085993325,0.014297301995403801,0.010712353057387547,0.01221841952921018,-0.011733230772131844,-0.022422068905291269,0.0013256782401828587,-0.022932371813699188,0.0079658416274202299,0.0060909619261193104,0.022082877915778391,-0.013779324934015317,-0.0070555299028719869,0.050243253962606815,0.0059056768131588236,0.0010722768922086462,0.019725513441253659,-0.01739685120813968,0.046686705031168306,-0.0011264668884706415,0.013161853342717824,0.016134233412137673,-0.023260002251184676,-0.01772942355349192,-0.012472476737288459,-0.021039512109708072,-0.026928662073656344,-0.023577379371884231,0.01433251943112949,-0.024709687454713209,-0.0072054888074051778,0.035813262032507072,0.030618164272077038,0.0022744509502565669,-0.020614867734277353,-0.00051883059471636022,-0.01945229476537317,-0.00034247419967775285,0.025111339091626238,0.015671010094255641,-0.0070284851699056955,-0.0018317408522285188,-0.023206368553104491,0.060444414976950189,-0.019322439250291326,0.017033902028773563,-0.0019086745468024952,-0.027259933770757604,-0.023489316620546239,0.039400824873046275,-0.029849834854131181,-0.057356948161511989,-0.026765389280282426,0.054747780422537883,-0.024397870841220973,-0.0086481821323898221,-0.0072065912313718141,-0.028833498274069273,0.018174280121003628,0.044327423123565607,0.026730171199540301,0.039402602135826192,-0.0013244545239283036,-0.035759359245486566,0.012150739819246869,0.028014981513842303,0.018853198953349665,0.04656311612754066,0.035804355534157145,-0.021874193847017429,-0.012241539893117551,-0.014598894166962764,-0.019713056051391842,-0.040160992548362279,0.012984469363699243,0.0070105568649531918,-0.060434685450133113,0.066729568046555804,0.045497568567336666,-0.046072301234689819,-0.018155237245743076,0.0023520261248008789,-0.055994602074954783,-0.01161402719852171,0.020715907624807454,0.0059909473305839733,-0.024626707195886392,0.017066663475524966,-0.00050333767511561822,0.048349969229478096,0.023928351106007138,-0.042390795199620232,0.066523085910370927,0.033026957865997228,-0.027025661164086921,-0.025223577459388226,0.021120683683431875,-0.023669370352199397,-0.023777841770808394,0.023175046440686381,-0.049177134522999918,0.018061175585786789,-0.031692658357453968,0.013311773928376977,-0.0044946027362668074,0.046356828592482406,-0.0181823210054659,0.04612253991075535,-0.021590698435369773,0.009522932617176065,-0.025953071319977008,-0.013706081353680644,-0.018655532345387162,0.040677118436116377,0.028110792892039729,-0.043293454977042477,0.071895587973599046,0.00082869978862417217,-0.011932353379925875,0.012863298639546512,-0.014000866068283654,0.0098657428465495026,0.018375460956041985,-0.033074581404591269,-0.02363324493581925,-0.01779141846950839,-0.018887587139245009,-0.027113717843255537,0.0021635262686050456,0.069254437393949264,0.023363095623306834,-0.013292974819016172,-0.01234556015740439,0.0049994788960076465,0.030614965933554769,-0.015883355244693111,-0.0035324889408903997,0.0063096207580046713,0.030397175465495176,-0.00076255986536567896,-0.013460949711298239]]}" total_answer_dict.p "../tmp/candidate.csv" "../tmp/candidate.p"
if __name__ == "__main__":
    main(sys.argv)
