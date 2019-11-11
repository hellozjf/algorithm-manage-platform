package com.zrar.ai.vo;

import lombok.Data;

/**
 * {
 *   "removePunctuation": false,
 *   "removeStopWord": true,
 *   "cutMethod": "char_cut",
 *   "length": 128,
 *   "modelName": "bert_match",
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
     * 切词方式
     * mleap：切词、切词——税务专有词、切短语
     * tensorflow：切字
     */
    private String cutMethod;

    /**
     * 位数
     */
    private Integer length;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 组合类型，例如：ap,bi
     */
    private String compose;

    /**
     * 是否有label_ids
     */
    private Boolean haveLabelIds;
}
