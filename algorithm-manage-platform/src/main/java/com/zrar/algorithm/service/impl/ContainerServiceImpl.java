package com.zrar.algorithm.service.impl;

import com.zrar.algorithm.domain.AiModelEntity;
import com.zrar.algorithm.service.ContainerService;
import com.zrar.algorithm.vo.FullNameVO;
import org.springframework.stereotype.Service;

/**
 * 容器服务
 *
 * @author Jingfeng Zhou
 */
@Service
public class ContainerServiceImpl implements ContainerService {

    /**
     * 获取容器的名称
     *
     * 容器名称由：前缀-类型-名称-版本 组成
     *
     * @return
     */
    @Override
    public FullNameVO getFullName(AiModelEntity aiModelEntity) {
        return FullNameVO.getByTypeNameVersion(aiModelEntity.getType(), aiModelEntity.getShortName(), aiModelEntity.getVersion());
    }
}
