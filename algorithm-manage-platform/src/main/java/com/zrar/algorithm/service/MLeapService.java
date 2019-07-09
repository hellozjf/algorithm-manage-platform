package com.zrar.algorithm.service;

/**
 * @author Jingfeng Zhou
 */
public interface MLeapService {

    /**
     * 上线模型
     * @param modelName
     * @return
     */
    String online(String modelName);

    /**
     * 下线模型
     * @param modelName
     * @return
     * @throws Exception
     */
    String offline(String modelName);

    /**
     * 调用某个mleap服务的transform接口，传输的内容是data，返回的是一个String
     * @param modelName
     * @param data
     * @return
     */
    String transform(String modelName, String data);

}
