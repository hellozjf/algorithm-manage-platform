package com.zrar.ai.vo;

import com.zrar.ai.annotation.Query;
import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
public class DictItemQueryVO extends BaseQueryVO {

    /**
     * 根据code模糊查询
     */
    @Query(type = Query.Type.INNER_LIKE)
    private String value;

    /**
     * 根据name模糊查询
     */
    @Query(type = Query.Type.INNER_LIKE)
    private String text;

    /**
     * 根据name模糊查询
     */
    @Query(type = Query.Type.INNER_LIKE)
    private String description;

    /**
     * 排序
     */
    @Query
    private Integer sort;

    /**
     * 其它值
     */
    @Query(type = Query.Type.INNER_LIKE)
    private String other;

    /**
     * 字典编号
     */
    @Query(joinName = "dict", propName = "id")
    private String dictId;

    /**
     * 字典代码
     */
    @Query(joinName = "dict", propName = "code")
    private String dictCode;

    /**
     * 字典名称
     */
    @Query(joinName = "dict", propName = "name", type = Query.Type.INNER_LIKE)
    private String dictName;
}
