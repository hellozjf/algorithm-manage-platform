package com.zrar.algorithm.vo;

import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
public class DictVO extends BaseVO {

    /**
     * 字典编码
     */
    private String code;

    /**
     * 字典名称
     */
    private String name;

    /**
     * 字典描述
     */
    private String description;
}
