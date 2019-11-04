package com.zrar.algorithm.service.impl;

import com.zrar.algorithm.config.CustomDockerConfig;
import com.zrar.algorithm.constant.ModelTypeEnum;
import com.zrar.algorithm.domain.AiModelEntity;
import com.zrar.algorithm.service.FullNameService;
import com.zrar.algorithm.vo.FullNameVO;
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
    public FullNameVO getFullNameByAiModelEntity(AiModelEntity aiModelEntity) {
        return getByTypeNameVersion(aiModelEntity.getType(), aiModelEntity.getShortName(), aiModelEntity.getVersion());
    }

    @Override
    public FullNameVO getByFullName(String fullName) {
        FullNameVO fullNameVO = new FullNameVO();
        String[] parts = fullName.split("-");
        fullNameVO.setFullName(fullName);
        fullNameVO.setPrefix(parts[0]);
        fullNameVO.setStrType(parts[1]);
        fullNameVO.setIType(ModelTypeEnum.getCodeByDescription(fullNameVO.getStrType()));
        fullNameVO.setShortName(String.join("-", Arrays.copyOfRange(parts, 2, parts.length - 1)));
        fullNameVO.setVersion(Integer.valueOf(parts[parts.length - 1]));
        return fullNameVO;
    }

    @Override
    public FullNameVO getByTypeNameVersion(int type, String shortName, int version) {
        String fullName = String.join("-", customDockerConfig.getPrefix(), ModelTypeEnum.getDescriptionByCode(type), shortName, String.valueOf(version));
        return getByFullName(fullName);
    }
}
