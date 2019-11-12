package com.zrar.ai.bo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Jingfeng Zhou
 */
@Slf4j
@Data
@Entity
@Table(name = "log")
public class LogBO extends BaseBO {

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
}
