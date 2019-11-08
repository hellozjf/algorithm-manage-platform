package com.zrar.ai.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

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
     * 问答匹配模型中的原始问题
     */
    private String rawQuestion;

    /**
     * 转换之后的参数
     */
    private JsonNode params;

    /**
     * 预测前处理花费的毫秒数（获取向量）
     */
    private Long preCostMs;

    /**
     * 预测花费的毫秒数
     */
    private Long predictCostMs;

    /**
     * 预测后处理花费的毫秒数
     */
    private Long postCostMs;

    /**
     * 预测的分类结果
     */
    private String predictString;

    /**
     * 预测的分类结果的编号
     */
    private String predictId;

    /**
     * 预测的分类结果的节点代码
     */
    private String predictNodeCode;

    /**
     * 预测的分类类别
     */
    private Integer predict;

    /**
     * 预测的分类概率，最大1
     */
    private Double probability;

    /**
     * 预测的结果列表
     */
    private List<JsonNode> predictList;

    /**
     * docker容器返回的结果
     */
    private String dockerResult;

    /**
     * 如果是级联容器，这里返回下一个容器的输入
     */
    private String nextInput;
}
