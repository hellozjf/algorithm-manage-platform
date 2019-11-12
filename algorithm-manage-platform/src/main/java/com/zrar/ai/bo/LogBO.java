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

    private String content;
    private String description;
    private Long cost;
    private String method;
    private String param;
    private String ip;
}
