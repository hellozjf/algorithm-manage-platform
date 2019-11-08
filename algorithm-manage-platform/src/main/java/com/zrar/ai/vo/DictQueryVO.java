package com.zrar.ai.vo;

import com.zrar.ai.annotation.Query;
import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
public class DictQueryVO extends BaseQueryVO {

    /**
     * 根据code模糊查询
     */
    @Query(type = Query.Type.INNER_LIKE)
    private String code;

    /**
     * 根据name模糊查询
     */
    @Query(type = Query.Type.INNER_LIKE)
    private String name;

    /**
     * 根据name模糊查询
     */
    @Query(type = Query.Type.INNER_LIKE)
    private String description;
}
