package com.zrar.algorithm.vo;

import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
public class PredictResultVO {

    /**
     * 原始字符串
     */
    private String raw;

    /**
     * 转换之后的参数
     */
    private String param;

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
