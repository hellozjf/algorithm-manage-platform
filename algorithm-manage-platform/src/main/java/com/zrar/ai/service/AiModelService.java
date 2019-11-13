package com.zrar.ai.service;

import com.zrar.ai.bo.AiModelBO;
import com.zrar.ai.vo.AiModelVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * @author Jingfeng Zhou
 */
public interface AiModelService extends BaseService<AiModelVO> {

    List<AiModelVO> getAllModels();
    Page<AiModelVO> getAllModels(Pageable pageable);
    AiModelVO findById(String id);
    AiModelVO save(AiModelVO aiModelVO);
    AiModelVO updateMd5(AiModelVO aiModelVO, String md5);
    AiModelVO getAiModelVO(AiModelVO aiModelVO, String shortName, String type, int version, boolean bRenewVersion);
    AiModelVO findByTypeAndShortNameAndVersion(String type, String shortName, int version);
    List<AiModelVO> findByShortName(String shortName);
}
