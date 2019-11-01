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

    /**
     * 通过类型描述获取类型代码
     * @param code
     * @return
     */
    public static String getDescByCode(int code) {
        for (ModelTypeEnum modelTypeEnum : ModelTypeEnum.values()) {
            if (modelTypeEnum.getCode() == code) {
                return modelTypeEnum.getDesc();
            }
        }
        return "";
    }

    /**
     * 通过类型代码获取类型描述
     * @param desc
     * @return
     */
    public static int getCodeByDesc(String desc) {
        for (ModelTypeEnum modelTypeEnum : ModelTypeEnum.values()) {
            if (modelTypeEnum.getDesc().equalsIgnoreCase(desc)) {
                return modelTypeEnum.getCode();
            }
        }
        return -1;
    }

    int code;
    String desc;
}
