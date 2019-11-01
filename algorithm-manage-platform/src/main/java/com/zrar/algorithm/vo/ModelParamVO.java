package com.zrar.algorithm.vo;

import lombok.Data;

/**
 * {
 *   "removePunctuation": false,
 *   "removeStopWord": true,
 *   "length": 128,
 *   "paramCode": 102,
 *   "compose": "ap,bi",
 *   "haveLabelIds": true
 * }
 *
 * @author Jingfeng Zhou
 */
@Data
public class ModelParamVO {

    /**
     * 是否要移除标点
     */
    private Boolean removePunctuation;

    /**
     * 是否要去停词
     */
    private Boolean removeStopWord;

    /**
     * 位数
     */
    private Integer length;

    /**
     * 参数代码
     * @see com.zrar.algorithm.constant.ModelParamEnum
     */
    private Integer paramCode;

    /**
     * 组合类型，例如：ap,bi
     */
    private String compose;

    /**
     * 是否有label_ids
     */
    private Boolean haveLabelIds;
}
