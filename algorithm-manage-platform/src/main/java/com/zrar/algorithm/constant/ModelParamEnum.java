package com.zrar.algorithm.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Jingfeng Zhou
 */
@Getter
@AllArgsConstructor
public enum ModelParamEnum {

    // TODO 每增加一个模型，需要增加一个获取方式
    // compose用的切词方式
    COMPOSE(-1, "compose", "", ModelTypeEnum.COMPOSE.getCode()),

    // mleap相关的切词方式
    MLEAP_CUT_WORD(1, "切词", "", ModelTypeEnum.MLEAP.getCode()),
    MLEAP_CUT_WORD_VSWZYC(2, "切词——税务专有词", "vswzyc", ModelTypeEnum.MLEAP.getCode()),
    MLEAP_PHRASE_LIST(3, "切短语", "", ModelTypeEnum.MLEAP.getCode()),

    // tensorflow的参数获取方式
    TENSORFLOW_DIRTY_WORD(101, "脏话模型", "", ModelTypeEnum.TENSORFLOW.getCode()),
    TENSORFLOW_SENTIMENT_ANALYSIS(102, "情感分析模型", "", ModelTypeEnum.TENSORFLOW.getCode()),
    TENSORFLOW_IS_TAX_ISSUE(103, "智能咨询-问题过滤", "", ModelTypeEnum.TENSORFLOW.getCode()),
    TENSORFLOW_AP_BILSTM(104, "ap-bilstm模型", "", ModelTypeEnum.TENSORFLOW.getCode()),
    TENSORFLOW_SHEBAO(105, "社保模型", "", ModelTypeEnum.TENSORFLOW.getCode()),
    TENSORFLOW_FIRSTALL(106, "三分类模型", "", ModelTypeEnum.TENSORFLOW.getCode()),
    TENSORFLOW_SYNTHESIS(107, "综合模型", "", ModelTypeEnum.TENSORFLOW.getCode()),
    TENSORFLOW_CITY_MANAGEMENT(108, "城管模型", "", ModelTypeEnum.TENSORFLOW.getCode()),
    TENSORFLOW_ZNZX(109, "智能咨询-场景分类", "", ModelTypeEnum.TENSORFLOW.getCode()),
    TENSORFLOW_RERANKING(110, "reranking模型", "", ModelTypeEnum.TENSORFLOW.getCode()),
    TENSORFLOW_BERT_MATCH(111, "bert_match模型", "", ModelTypeEnum.TENSORFLOW.getCode()),
    ;

    int code;
    String desc;
    String nature;
    int modelTypeCode;
}
