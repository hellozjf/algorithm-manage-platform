package com.zrar.algorithm.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
public class PredictResultVO {

    /**
     * 原始字符串
     */
    private String sentence;

    /**
     * 转换之后的参数
     */
    private JsonNode params;

    /**
     * 获取参数花费的毫秒数
     */
    private Long getParamsCostMs;

    /**
     * 预测花费的毫秒数
     */
    private Long predictCostMs;

    /**
     * 预测的分类结果
     */
    private String predictString;

    /**
     * 预测的分类类别
     */
    private Integer predict;

    /**
     * 预测的分类概率，最大1
     */
    private Double probability;

}
