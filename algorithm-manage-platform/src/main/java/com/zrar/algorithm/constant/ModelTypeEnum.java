package com.zrar.algorithm.constant;

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
    COMPOSE(3, "compose"),
    ;

    int code;
    String desc;
}
