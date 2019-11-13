package com.zrar.ai.vo;

import lombok.Data;

import java.util.Date;

/**
 * @author: chenzhangyi
 */
@Data
public class LogVO extends BaseVO {

    /**
     * 日志文本
     */
    private String content;

    /**
     * 日志描述
     */
    private String description;

    /**
     * 调用耗时
     */
    private Long cost;

    /**
     * 调用controller方法名称
     */
    private String method;

    /**
     * 调用controller方法参数
     */
    private String param;

    /**
     * 调用者IP
     */
    private String ip;

    /**
     * 日志创建时间
     */
    private Date date;
}
