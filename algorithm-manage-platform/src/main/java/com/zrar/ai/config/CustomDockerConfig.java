package com.zrar.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * docker相关的自定义配置
 *
 * @author Jingfeng Zhou
 */
@Data
@Component
@ConfigurationProperties("custom.docker")
public class CustomDockerConfig {

    private String restIp;
    private int restPort;
    private String prefix;
    private int portRangeMin;
    private int portRangeMax;
    private String harborIp;
    private String modelOutter;
    private String modelInner;

}

