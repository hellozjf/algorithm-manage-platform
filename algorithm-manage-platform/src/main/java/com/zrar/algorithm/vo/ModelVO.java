package com.zrar.algorithm.vo;

import lombok.Data;

import java.util.Date;

/**
 * @author Jingfeng Zhou
 */
@Data
public class ModelVO {
    private String id;
    private Date gmtCreate;
    private Date gmtModified;
    private String name;
    private String desc;
    private String md5;
    private int type;
    private String typeName;
    private int param;
    private String paramName;
}
