package com.zrar.ai.service;

/**
 * @author Jingfeng Zhou
 */
public interface FileService {

    /**
     * 根据完整模型文件夹，获取模型所在开发环境的路径
     * @param fullName
     * @return
     */
    String getModelWorkFolderPath(String fullName);

    /**
     * 根据完整模型名称，获取模型所在开发环境的路径
     * @param fullName
     * @return
     */
    String getModelWorkFilePath(String fullName);

    /**
     * 根据模型名称，获取模型所在docker主机的路径
     * @param fullName
     * @return
     */
    String getModelOutterFolderPath(String fullName);

    /**
     * 根据模型名称，获取模型所在docker主机的路径
     * @param fullName
     * @return
     */
    String getModelOutterFilePath(String fullName);

    /**
     * 根据模型名称，获取模型所在docker容器内的路径
     * @param fullName
     * @return
     */
    String getModelInnerFolderPath(String fullName);

    /**
     * 根据模型名称，获取模型所在docker容器内的路径
     * @param fullName
     * @return
     */
    String getModelInnerFilePath(String fullName);
}
