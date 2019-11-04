package com.zrar.algorithm.util;

import com.zrar.algorithm.constant.ModelTypeEnum;

/**
 * @author Jingfeng Zhou
 */
public class ModelTypeUtils {

    /**
     * 通过模型类型的code获取描述信息
     * @param code
     * @return
     */
    public static String getDescByCode(int code) {
        for (ModelTypeEnum modelTypeEnum : ModelTypeEnum.values()) {
            if (modelTypeEnum.getCode() == code) {
                return modelTypeEnum.getDescription();
            }
        }
        return null;
    }
}
