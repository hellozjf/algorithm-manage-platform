package com.zrar.algorithm.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Jingfeng Zhou
 */
@Getter
@AllArgsConstructor
public enum ModelParamEnum {

    // mleap相关的切词方式
    MLEAP_CUT_WORD(1, "切词", "", ModelTypeEnum.MLEAP.getCode()),
    MLEAP_CUT_WORD_VSWZYC(2, "切词——税务专有词", "vswzyc", ModelTypeEnum.MLEAP.getCode()),
    MLEAP_PHRASE_LIST(3, "切短语", "", ModelTypeEnum.MLEAP.getCode()),

    // tensorflow的参数获取方式
    TENSORFLOW_NORMAL(101, "普通", "", ModelTypeEnum.TENSORFLOW.getCode()),
    TENSORFLOW_REMOVE_PUNCTUATION(102, "去掉标点", "", ModelTypeEnum.TENSORFLOW.getCode()),
    ;

    int code;
    String desc;
    String nature;
    int modelTypeCode;
}
