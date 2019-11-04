package com.zrar.algorithm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工作目录相关的自定义配置
 *
 * @author Jingfeng Zhou
 */
@Data
@Component
@ConfigurationProperties("custom.workdir")
public class CustomWorkdirConfig {
    private String root;
    private boolean needCopy;
    private String model;
}
