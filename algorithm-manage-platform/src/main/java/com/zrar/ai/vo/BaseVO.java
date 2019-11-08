package com.zrar.ai.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author hellozjf
 *
 */
@Data
public abstract class BaseVO implements Serializable {

    /**
     * ID
     */
    private String id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 更新时间
     */
    private Date gmtModified;
}