package com.zrar.algorithm.service;

import com.zrar.algorithm.domain.AiModelEntity;
import com.zrar.algorithm.vo.FullNameVO;

/**
 * 容器服务
 *
 * @author Jingfeng Zhou
 */
public interface ContainerService {

    /**
     * 获取容器的名称
     *
     * 容器名称由：前缀-类型-名称-版本 组成
     *
     * @return
     */
    FullNameVO getFullName(AiModelEntity aiModelEntity);
}
