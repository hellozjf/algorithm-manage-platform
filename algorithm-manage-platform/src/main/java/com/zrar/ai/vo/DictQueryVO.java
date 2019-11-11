package com.zrar.ai.vo;

import com.zrar.ai.annotation.Query;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
@ApiModel(description = "数据字典")
public class DictQueryVO extends BaseQueryVO {

    /**
     * 根据code模糊查询
     */
    @ApiModelProperty(value = "数据字典编码")
    @Query(type = Query.Type.INNER_LIKE)
    private String code;

    /**
     * 根据name模糊查询
     */
    @ApiModelProperty(value = "数据字典名称")
    @Query(type = Query.Type.INNER_LIKE)
    private String name;

    /**
     * 根据name模糊查询
     */
    @ApiModelProperty(value = "数据字典描述")
    @Query(type = Query.Type.INNER_LIKE)
    private String description;
}
