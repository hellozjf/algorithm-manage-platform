package com.zrar.algorithm.util;

import com.zrar.algorithm.constant.ModelParamEnum;

/**
 * @author Jingfeng Zhou
 */
public class ModelParamUtils {

    /**
     * 通过模型类型的code获取描述信息
     * @param code
     * @return
     */
    public static String getDescByCode(int code) {
        for (ModelParamEnum modelParamEnum : ModelParamEnum.values()) {
            if (modelParamEnum.getCode() == code) {
                return modelParamEnum.getDesc();
            }
        }
        return null;
    }
}
