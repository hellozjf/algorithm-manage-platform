package com.zrar.ai.service.impl;

import com.zrar.ai.bo.AiModelBO;
import com.zrar.ai.config.CustomDockerConfig;
import com.zrar.ai.service.FullNameService;
import com.zrar.ai.vo.AiModelVO;
import com.zrar.ai.vo.FullNameVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 容器服务
 *
 * @author Jingfeng Zhou
 */
@Service
public class FullNameServiceImpl implements FullNameService {

    @Autowired
    private CustomDockerConfig customDockerConfig;

    @Override
    public FullNameVO getByAiModel(AiModelVO aiModelVO) {
        return getByTypeNameVersion(aiModelVO.getType(), aiModelVO.getShortName(), aiModelVO.getVersion());
    }

    @Override
    public FullNameVO getByAiModel(AiModelBO aiModelBO) {
        return getByTypeNameVersion(aiModelBO.getType(), aiModelBO.getShortName(), aiModelBO.getVersion());
    }

    @Override
    public FullNameVO getByFullName(String fullName) {
        FullNameVO fullNameVO = new FullNameVO();
        String[] parts = fullName.split("-");
        fullNameVO.setFullName(fullName);
        fullNameVO.setPrefix(parts[0]);
        fullNameVO.setType(parts[1]);
        fullNameVO.setShortName(String.join("-", Arrays.copyOfRange(parts, 2, parts.length - 1)));
        fullNameVO.setVersion(Integer.valueOf(parts[parts.length - 1]));
        return fullNameVO;
    }

    @Override
    public FullNameVO getByTypeNameVersion(String type, String shortName, int version) {
        String fullName = String.join("-", customDockerConfig.getPrefix(), type, shortName, String.valueOf(version));
        return getByFullName(fullName);
    }
}
