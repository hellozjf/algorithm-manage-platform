package com.zrar.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zrar.ai.bo.AiModelBO;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.dao.AiModelDao;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.service.DockerService;
import com.zrar.ai.service.WebService;
import com.zrar.ai.vo.AiModelVO;
import com.zrar.ai.vo.ModelParamVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Jingfeng Zhou
 */
@Slf4j
@Service
public class WebServiceImpl implements WebService {

    @Autowired
    private AiModelDao aiModelDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DockerService dockerService;

    @Override
    public List<AiModelVO> getAllModels() {
        List<AiModelBO> aiModelBOPage = aiModelDao.findAll();
        List<AiModelVO> aiModelVOPage = aiModelBOPage.stream()
                .map(aiModelBO -> convert(aiModelBO))
                .collect(Collectors.toList());
        return aiModelVOPage;
    }

    @Override
    public Page<AiModelVO> getAllModels(Pageable pageable) {
        Page<AiModelBO> aiModelBOPage = aiModelDao.findAll(pageable);
        Page<AiModelVO> aiModelVOPage = aiModelBOPage.map(aiModelBO -> convert(aiModelBO));
        return aiModelVOPage;
    }

    @Override
    public AiModelVO findById(String id) {
        AiModelBO aiModelBO = aiModelDao.findById(id).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        return convert(aiModelBO);
    }

    @Override
    public AiModelVO save(AiModelVO aiModelVO) {
        if (aiModelVO.getId() != null) {
            // 说明是修改
            AiModelBO oldModelBO = aiModelDao.findById(aiModelVO.getId()).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
            AiModelBO aiModelBO = convert(aiModelVO);
            aiModelBO.setGmtCreate(oldModelBO.getGmtCreate());
            aiModelBO = aiModelDao.save(aiModelBO);
            return convert(aiModelBO);
        } else {
            // 说明是新增
            AiModelBO aiModelBO = convert(aiModelVO);
            aiModelBO = aiModelDao.save(aiModelBO);
            return convert(aiModelBO);
        }
    }

    @Override
    public AiModelVO updateMd5(AiModelVO aiModelVO, String md5) {
        AiModelBO aiModelBO = convert(aiModelVO);
        aiModelBO.setMd5(md5);
        aiModelBO = aiModelDao.save(aiModelBO);
        return convert(aiModelBO);
    }

    /**
     * 将AiModelVO转换为AiModelBO
     * @param aiModelVO
     * @return
     */
    private AiModelBO convert(AiModelVO aiModelVO) {
        AiModelBO aiModelBO = new AiModelBO();
        BeanUtil.copyProperties(aiModelVO, aiModelBO);

        ModelParamVO modelParamVO = aiModelVO.getParam();
        try {
            aiModelBO.setParam(objectMapper.writeValueAsString(modelParamVO));
        } catch (JsonProcessingException e) {
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }
        return aiModelBO;
    }

    @Override
    public AiModelVO getAiModelVO(AiModelVO aiModelVO, String shortName, String type, int version, boolean bRenewVersion) {
        AiModelBO aiModelBO;
        if (bRenewVersion) {
            // 如果要更新版本号，那么根据shortName和type去数据库查找最新的记录
            Optional<AiModelBO> optionalAiModelEntity = aiModelDao.findTopByTypeAndShortNameOrderByVersionDesc(type, shortName);
            if (optionalAiModelEntity.isPresent()) {
                AiModelBO oldAiModelEntity = optionalAiModelEntity.get();

                // 用新版本号创建一个AiModelEntity
                aiModelBO = new AiModelBO();
                BeanUtil.copyProperties(aiModelVO, aiModelBO);
                aiModelBO.setId(null);
                aiModelBO.setPort(dockerService.getRandomPort());
                aiModelBO.setVersion(oldAiModelEntity.getVersion() + 1);
            } else {
                // 新建一个
                aiModelBO = new AiModelBO();
                BeanUtils.copyProperties(aiModelVO, aiModelBO);
                aiModelBO.setId(null);
                aiModelBO.setPort(dockerService.getRandomPort());
                aiModelBO.setVersion(1);
            }
        } else {
            Optional<AiModelBO> optionalAiModelEntity = aiModelDao.findByTypeAndShortNameAndVersion(type, shortName, version);
            if (optionalAiModelEntity.isPresent()) {
                // 获取
                aiModelBO = optionalAiModelEntity.get();
                // 还要额外修改描述和参数
                aiModelBO.setDescription(aiModelVO.getDescription());
                try {
                    aiModelBO.setParam(objectMapper.writeValueAsString(aiModelVO.getParam()));
                } catch (JsonProcessingException e) {
                    log.error("e = ", e);
                    throw new AlgorithmException(ResultEnum.JSON_ERROR);
                }
            } else {
                // 新建一个
                aiModelBO = new AiModelBO();
                BeanUtils.copyProperties(aiModelVO, aiModelBO);
                aiModelBO.setVersion(1);
            }
        }
        return convert(aiModelBO);
    }

    @Override
    public void delete(AiModelVO aiModelVO) {
        aiModelDao.deleteById(aiModelVO.getId());
    }

    @Override
    public AiModelVO findByTypeAndShortNameAndVersion(String type, String shortName, int version) {
        AiModelBO aiModelBO = aiModelDao.findByTypeAndShortNameAndVersion(type, shortName, version)
                .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        return convert(aiModelBO);
    }

    /**
     * 将AiModelBO转换为AiModelVO
     * @param aiModelBO
     * @return
     */
    private AiModelVO convert(AiModelBO aiModelBO) {
        AiModelVO aiModelVO = new AiModelVO();
        BeanUtil.copyProperties(aiModelBO, aiModelVO);

        String param = aiModelBO.getParam();
        if (StringUtils.isEmpty(param)) {
            param = "{}";
        }
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(param);
        } catch (IOException e) {
            log.error("e = ", e);
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }
        JsonNode removePunctuation = jsonNode.get("removePunctuation");
        JsonNode removeStopWord = jsonNode.get("removeStopWord");
        JsonNode cutMethod = jsonNode.get("cutMethod");
        JsonNode length = jsonNode.get("length");
        JsonNode modelName = jsonNode.get("modelName");
        JsonNode compose = jsonNode.get("compose");
        JsonNode haveLabelIds = jsonNode.get("haveLabelIds");

        ModelParamVO modelParamVO = new ModelParamVO();
        modelParamVO.setRemovePunctuation(removePunctuation == null ? false : removePunctuation.booleanValue());
        modelParamVO.setRemoveStopWord(removeStopWord == null ? false : removePunctuation.booleanValue());
        modelParamVO.setCutMethod(cutMethod == null ? "" : cutMethod.asText());
        modelParamVO.setLength(length == null ? 0 : length.intValue());
        modelParamVO.setModelName(modelName == null ? "" : modelName.asText());
        modelParamVO.setCompose(compose == null ? "" : compose.asText());
        modelParamVO.setHaveLabelIds(haveLabelIds == null ? false : haveLabelIds.booleanValue());
        aiModelVO.setParam(modelParamVO);
        return aiModelVO;
    }
}
