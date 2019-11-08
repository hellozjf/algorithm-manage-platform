package com.zrar.ai.service;

import com.spotify.docker.client.messages.ContainerCreation;

import java.io.IOException;

/**
 * docker服务
 *
 * @author Jingfeng Zhou
 */
public interface DockerService {

    /**
     * 重新初始化docker相关服务
     * @throws Exception
     */
    void init();

    /**
     * 判断docker服务是否已经正常运行
     * @return
     */
    boolean isStarted();

    /**
     * 解压模型
     * @param fullName
     */
    void unpackModel(String fullName);

    /**
     * 重启docker容器
     * @param fullName
     */
    void restartDocker(String fullName) throws Exception;

    /**
     * 重新创建docker容器
     * @param fullName
     * @return
     * @throws Exception
     */
    ContainerCreation recreateDocker(String fullName) throws Exception;

    /**
     * 从minPort ~ maxPort之间寻找一个未被使用的端口
     *
     * @return
     */
    int getRandomPort();

    /**
     * 创建docker容器
     * @param fullName
     * @throws IOException
     */
    ContainerCreation createDocker(String fullName) throws Exception;

    /**
     * 开启docker容器
     * @param fullName
     */
    void startDocker(String fullName) throws Exception;

    /**
     * 关闭docker容器
     * @param fullName
     */
    void stopDocker(String fullName) throws Exception;

    /**
     * 删除docker容器
     * @param fullName
     * @throws IOException
     */
    void deleteDocker(String fullName) throws Exception;
}
