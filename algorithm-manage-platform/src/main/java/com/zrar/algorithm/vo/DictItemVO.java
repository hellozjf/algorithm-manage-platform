package com.zrar.algorithm.vo;

import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
public class DictItemVO extends BaseVO {

    /**
     * 值
     */
    private String value;

    /**
     * 显示文本
     */
    private String text;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 其它值
     */
    private String other;

    /**
     * 字典编号
     */
    private String dictId;
}
