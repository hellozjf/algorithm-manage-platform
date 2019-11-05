package com.zrar.algorithm.vo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据库中，模型的实体类
 *
 * @author Jingfeng Zhou
 */
@Slf4j
@Data
public class AiModelVO {

    /**
     * 模型的编号
     */
    private String id;

    /**
     * 模型的名称，例如sw、yyth、qgfx……
     * 实际的模型文件为 前缀+类型+名称+版本.zip
     */
    private String shortName;

    /**
     * 模型描述
     */
    private String description;

    /**
     * 模型类型，目前支持mleap和tensorflow
     * @see com.zrar.algorithm.constant.ModelTypeEnum
     */
    private Integer type;

    /**
     * 模型参数
     */
    private ModelParamVO param;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 模型状态
     */
    private String state;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 版本号是否增加
     */
    private Boolean newVersion;
}
