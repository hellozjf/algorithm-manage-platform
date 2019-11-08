package com.zrar.ai.service.impl;

import com.zrar.ai.config.CustomDockerConfig;
import com.zrar.ai.config.CustomWorkdirConfig;
import com.zrar.ai.service.FileService;
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

    @Override
    public String getModelWorkFolderPath(String fullName) {
        return customWorkdirConfig.getModel() + "/" + fullName;
    }

    /**
     * 根据完整模型名称，获取模型所在开发环境的路径
     * @param fullName
     * @return
     */
    @Override
    public String getModelWorkFilePath(String fullName) {
        return customWorkdirConfig.getModel() + "/" + fullName + ".zip";
    }

    /**
     * 根据完整模型名称，获取模型所在开发环境的路径
     * @param fullName
     * @return
     */
    @Override
    public String getModelOutterFolderPath(String fullName) {
        return customDockerConfig.getModelOutter() + "/" + fullName;
    }

    /**
     * 根据模型名称，获取模型所在docker主机的路径
     * @param fullName
     * @return
     */
    @Override
    public String getModelOutterFilePath(String fullName) {
        return customDockerConfig.getModelOutter() + "/" + fullName + ".zip";
    }

    /**
     * 根据模型名称，获取模型所在docker容器内的路径
     * @param fullName
     * @return
     */
    @Override
    public String getModelInnerFolderPath(String fullName) {
        return customDockerConfig.getModelInner() + "/" + fullName;
    }

    /**
     * 根据模型名称，获取模型所在docker容器内的路径
     * @param fullName
     * @return
     */
    @Override
    public String getModelInnerFilePath(String fullName) {
        return customDockerConfig.getModelInner() + "/" + fullName + ".zip";
    }
}
