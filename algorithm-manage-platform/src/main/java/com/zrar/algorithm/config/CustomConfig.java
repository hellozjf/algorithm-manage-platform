package com.zrar.algorithm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义配置
 *
 * @author Jingfeng Zhou
 */
@Data
@Component
@ConfigurationProperties("custom")
public class CustomConfig {

    /**
     * platform-path，所有数据库、docker-compose.yml、models都在该目录下面
     */
    private String platformPath;

    /**
     * algorithm-bridge的IP地址
     */
    private String bridgeIp;

    /**
     * algorithm-bridge的端口
     */
    private Integer bridgePort;

    /**
     * 模型放置在宿主机里面的路径地址
     */
    private String modelOuterPath;

    /**
     * 模型放置在docker容器里面的路径地址
     */
    private String modelInnerPath;

    /**
     * docker-compose.yml文件所在的路径地址
     */
    private String dockerComposePath;

    /**
     * harbor的地址，生成docker-compose.yml时需要用到
     */
    private String harborIp;

}
