package com.zrar.ai.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Jingfeng Zhou
 */
@Getter
@AllArgsConstructor
public enum CutMethodEnum {

    WORD_CUT(1, "切词"),
    WORD_CUT_VSWZYC(2, "切词——税务专有词"),
    PHRASE_LIST(3, "切短语"),
    CHAR_CUT(4, "切字")
    ;

    public static CutMethodEnum getByCode(Integer code) {
        for (CutMethodEnum cutMethodEnum : CutMethodEnum.values()) {
            if (cutMethodEnum.getCode().equals(code)) {
                return cutMethodEnum;
            }
        }
        return null;
    }

    Integer code;
    String description;
}
