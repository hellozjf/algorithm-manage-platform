package com.zrar.ai.service;

import com.zrar.ai.bo.AiModelBO;
import com.zrar.ai.vo.AiModelVO;
import com.zrar.ai.vo.FullNameVO;

/**
 * 容器服务
 *
 * @author Jingfeng Zhou
 */
public interface FullNameService {

    /**
     * 从AiModelVO中获取到FullNameVO
     * @param aiModelVO
     * @return
     */
    FullNameVO getByAiModel(AiModelVO aiModelVO);

    /**
     * 从AiModelBO中获取FullNameVO
     *
     * @param aiModelBO
     * @return
     */
    FullNameVO getByAiModel(AiModelBO aiModelBO);

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
    FullNameVO getByTypeNameVersion(String type, String shortName, int version);
}
