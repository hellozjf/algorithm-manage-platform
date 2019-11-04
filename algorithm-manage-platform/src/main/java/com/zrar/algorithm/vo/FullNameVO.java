package com.zrar.algorithm.vo;

import lombok.Data;

/**
 * 容器名称VO对象
 *
 * @author Jingfeng Zhou
 */
@Data
public class FullNameVO {

    /**
     * 完成的名称，包括 前缀-类型-名称-版本
     */
    private String fullName;

    /**
     * 前缀
     */
    private String prefix;

    /**
     * 字符串类型的类型
     */
    private String strType;

    /**
     * 数字类型的类型
     */
    private int iType;

    /**
     * 名称
     */
    private String shortName;

    /**
     * 版本
     */
    private int version;
}
