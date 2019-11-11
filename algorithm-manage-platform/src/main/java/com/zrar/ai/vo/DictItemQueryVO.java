package com.zrar.ai.vo;

import com.zrar.ai.annotation.Query;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
@ApiModel(description = "数据字典项目")
public class DictItemQueryVO extends BaseQueryVO {

    /**
     * 根据code模糊查询
     */
    @ApiModelProperty(value = "数据字典项目值")
    @Query(type = Query.Type.INNER_LIKE)
    private String value;

    /**
     * 根据name模糊查询
     */
    @ApiModelProperty(value = "数据字典项目显示文本")
    @Query(type = Query.Type.INNER_LIKE)
    private String text;

    /**
     * 根据name模糊查询
     */
    @ApiModelProperty(value = "数据字典项目描述")
    @Query(type = Query.Type.INNER_LIKE)
    private String description;

    /**
     * 排序
     */
    @ApiModelProperty(value = "数据字典项目排序位置", example = "0")
    @Query
    private Integer sort;

    /**
     * 其它值
     */
    @ApiModelProperty(value = "数据字典项目其它参数")
    @Query(type = Query.Type.INNER_LIKE)
    private String other;

    /**
     * 字典编号
     */
    @ApiModelProperty(value = "数据字典编号")
    @Query(joinName = "dict", propName = "id")
    private String dictId;

    /**
     * 字典代码
     */
    @ApiModelProperty(value = "数据字典代码")
    @Query(joinName = "dict", propName = "code")
    private String dictCode;

    /**
     * 字典名称
     */
    @ApiModelProperty(value = "数据字典名称")
    @Query(joinName = "dict", propName = "name", type = Query.Type.INNER_LIKE)
    private String dictName;
}
