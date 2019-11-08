package com.zrar.ai.service;

import com.zrar.ai.domain.AiModelEntity;
import com.zrar.ai.vo.FullNameVO;

/**
 * 容器服务
 *
 * @author Jingfeng Zhou
 */
public interface FullNameService {

    /**
     * 从AiModelEntity中获取FullNameVO
     *
     * @param aiModelEntity
     * @return
     */
    FullNameVO getByAiModelEntity(AiModelEntity aiModelEntity);

    /**
     * 通过前缀-类型-名称-版本号，获取FullNameVO
     *
     * @param fullName
     * @return
     */
    FullNameVO getByFullName(String fullName);

    /**
     * 通过类型、名称、版本号获取FullNameVO
     *
     * @param type
     * @param shortName
     * @param version
     * @return
     */
    FullNameVO getByTypeNameVersion(int type, String shortName, int version);
}
