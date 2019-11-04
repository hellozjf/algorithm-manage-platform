package com.zrar.algorithm.service;

/**
 * @author Jingfeng Zhou
 */
public interface FileService {

    /**
     * 根据完整模型名称，获取模型所在开发环境的路径
     * @param fullName
     * @return
     */
    String getModelPath(String fullName);

    /**
     * 根据模型名称，获取模型所在docker主机的路径
     * @param fullName
     * @return
     */
    String getModelOutterPath(String fullName);

    /**
     * 根据模型名称，获取模型所在docker容器内的路径
     * @param fullName
     * @return
     */
    String getModelInnerPath(String fullName);
}
