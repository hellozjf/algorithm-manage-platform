package com.zrar.ai.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据库中，模型的实体类
 *
 * @author Jingfeng Zhou
 */
@Slf4j
@Data
@ApiModel(description = "订单查询请求数据")
public class AiModelVO {

    /**
     * 模型的编号
     */
    @ApiModelProperty(value = "模型编号")
    private String id;

    /**
     * 模型的名称，例如sw、yyth、qgfx……
     * 实际的模型文件为 前缀+类型+名称+版本.zip
     */
    @ApiModelProperty(value = "模型名称")
    private String shortName;

    /**
     * 模型描述
     */
    @ApiModelProperty(value = "模型描述")
    private String description;

    /**
     * 模型类型，目前支持mleap和tensorflow
     */
    @ApiModelProperty(value = "模型类型")
    private String type;

    /**
     * 模型参数
     */
    @ApiModelProperty(value = "模型参数")
    private ModelParamVO param;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "模型版本", example = "1")
    private Integer version;

    /**
     * 模型状态
     */
    @ApiModelProperty(value = "模型状态")
    private String state;

    /**
     * 端口
     */
    @ApiModelProperty(value = "模型端口", example = "0")
    private Integer port;

    /**
     * 版本号是否增加
     */
    @ApiModelProperty(value = "模型版本是否增加")
    private Boolean newVersion;
}
