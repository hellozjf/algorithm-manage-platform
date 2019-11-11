package com.zrar.ai.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
@ApiModel(description = "数据字典")
public class DictVO extends BaseVO {

    /**
     * 字典编码
     */
    @ApiModelProperty(value = "数据字典编码")
    private String code;

    /**
     * 字典名称
     */
    @ApiModelProperty(value = "数据字典名称")
    private String name;

    /**
     * 字典描述
     */
    @ApiModelProperty(value = "数据字典描述")
    private String description;
}
