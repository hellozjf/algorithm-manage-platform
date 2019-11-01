package com.zrar.algorithm.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Jingfeng Zhou
 */
@AllArgsConstructor
@Getter
public enum StateEnum {

    RUNNING("running", "运行"),
    EXITED("exited", "停止")
    ;

    String state;
    String desc;
}
