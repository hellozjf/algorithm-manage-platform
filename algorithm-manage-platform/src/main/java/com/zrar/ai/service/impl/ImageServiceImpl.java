package com.zrar.ai.service.impl;

import com.zrar.ai.config.CustomDockerConfig;
import com.zrar.ai.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 镜像服务
 *
 * @author Jingfeng Zhou
 */
@Service
public class ImageServiceImpl implements ImageService {

    @Autowired
    private CustomDockerConfig customDockerConfig;

    /**
     * 获取mleap镜像名称
     * @return
     */
    @Override
    public String getMleap() {
        return customDockerConfig.getHarborIp() + "/zrar/mleap-serving:0.9.0-SNAPSHOT";
    }

    /**
     * 获取tensorflow镜像名称
     * @return
     */
    @Override
    public String getTensorflow() {
        return customDockerConfig.getHarborIp() + "/zrar/tensorflow-serving:1.14.0";
    }
}
