package com.zrar.algorithm.service;

/**
 * @author Jingfeng Zhou
 */
public interface MLeapService {

    /**
     * 上线模型
     * @param fullName
     * @return
     */
    String online(String fullName);

    /**
     * 下线模型
     * @param fullName
     * @return
     * @throws Exception
     */
    String offline(String fullName);

    /**
     * 调用某个mleap服务的transform接口，传输的内容是data，返回的是一个String
     * @param fullName
     * @param data
     * @return
     */
    String transform(String fullName, String data);

}
