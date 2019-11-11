package com.zrar.ai.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
@ApiModel(description = "数据字典项目")
public class DictItemVO extends BaseVO {

    /**
     * 值
     */
    @ApiModelProperty(value = "数据字典项目值")
    private String value;

    /**
     * 显示文本
     */
    @ApiModelProperty(value = "数据字典项目显示文本")
    private String text;

    /**
     * 描述
     */
    @ApiModelProperty(value = "数据字典项目描述")
    private String description;

    /**
     * 排序
     */
    @ApiModelProperty(value = "数据字典项目排序位置", example = "0")
    private Integer sort;

    /**
     * 其它值
     */
    @ApiModelProperty(value = "数据字典项目其它参数")
    private String other;

    /**
     * 字典编号
     */
    @ApiModelProperty(value = "数据字典编号")
    private String dictId;
}
