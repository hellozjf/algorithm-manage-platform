package com.zrar.mleap.transformer.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Jingfeng Zhou
 */
@Getter
@AllArgsConstructor
public enum ModelTypeEnum {
    MLEAP(1, "mleap"),
    TENSORFLOW(2, "tensorflow"),
    ;

    int code;
    String desc;
}
