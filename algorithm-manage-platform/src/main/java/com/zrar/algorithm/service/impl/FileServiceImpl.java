package com.zrar.algorithm.service.impl;

import com.zrar.algorithm.config.CustomDockerConfig;
import com.zrar.algorithm.config.CustomWorkdirConfig;
import com.zrar.algorithm.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Jingfeng Zhou
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private CustomDockerConfig customDockerConfig;

    @Autowired
    private CustomWorkdirConfig customWorkdirConfig;

    /**
     * 根据完整模型名称，获取模型所在开发环境的路径
     * @param fullName
     * @return
     */
    @Override
    public String getModelPath(String fullName) {
        return customWorkdirConfig.getModel() + "/" + fullName + ".zip";
    }

    /**
     * 根据模型名称，获取模型所在docker主机的路径
     * @param fullName
     * @return
     */
    @Override
    public String getModelOutterPath(String fullName) {
        return customDockerConfig.getModelOutter() + "/" + fullName + ".zip";
    }

    /**
     * 根据模型名称，获取模型所在docker容器内的路径
     * @param fullName
     * @return
     */
    @Override
    public String getModelInnerPath(String fullName) {
        return customDockerConfig.getModelInner() + "/" + fullName + ".zip";
    }
}
