package com.zrar.algorithm.service;

/**
 * 镜像服务
 *
 * @author Jingfeng Zhou
 */
public interface ImageService {

    /**
     * 获取mleap镜像名称
     * @return
     */
    String getMleap();

    /**
     * 获取tensorflow镜像名称
     * @return
     */
    String getTensorflow();
}
