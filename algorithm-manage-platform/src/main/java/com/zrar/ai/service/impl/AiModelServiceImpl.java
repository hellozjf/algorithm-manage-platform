package com.zrar.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zrar.ai.bo.AiModelBO;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.dao.AiModelDao;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.mapper.AiModelMapper;
import com.zrar.ai.service.AiModelService;
import com.zrar.ai.service.DockerService;
import com.zrar.ai.vo.AiModelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author Jingfeng Zhou
 */
@Slf4j
@Service
public class AiModelServiceImpl extends BaseServiceImpl<AiModelVO, AiModelBO, AiModelDao, AiModelMapper>
        implements AiModelService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DockerService dockerService;

    @Override
    public List<AiModelVO> getAllModels() {
        List<AiModelBO> aiModelBOList = dao.findAll();
        return mapper.toVO(aiModelBOList);
    }

    @Override
    public Page<AiModelVO> getAllModels(Pageable pageable) {
        Page<AiModelBO> aiModelBOPage = dao.findAll(pageable);
        Page<AiModelVO> aiModelVOPage = aiModelBOPage.map(mapper::toVO);
        return aiModelVOPage;
    }

    @Override
    public AiModelVO findById(String id) {
        AiModelBO aiModelBO = dao.findById(id)
                .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        return mapper.toVO(aiModelBO);
    }

    @Override
    public AiModelVO save(AiModelVO aiModelVO) {
        if (aiModelVO.getId() != null) {
            // 说明是修改
            AiModelBO oldModelBO = dao.findById(aiModelVO.getId())
                    .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
            AiModelBO aiModelBO = mapper.toBO(aiModelVO);
            aiModelBO.setGmtCreate(oldModelBO.getGmtCreate());
            aiModelBO = dao.save(aiModelBO);
            return mapper.toVO(aiModelBO);
        } else {
            // 说明是新增
            AiModelBO aiModelBO = mapper.toBO(aiModelVO);
            aiModelBO = dao.save(aiModelBO);
            return mapper.toVO(aiModelBO);
        }
    }

    @Override
    public AiModelVO updateMd5(AiModelVO aiModelVO, String md5) {
        AiModelBO oldAiModelBO = dao.findById(aiModelVO.getId())
                .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        AiModelBO aiModelBO = mapper.toBO(aiModelVO);
        aiModelBO.setGmtCreate(oldAiModelBO.getGmtCreate());
        aiModelBO.setMd5(md5);
        aiModelBO = dao.save(aiModelBO);
        return mapper.toVO(aiModelBO);
    }

    @Override
    public AiModelVO getAiModelVO(AiModelVO aiModelVO, String shortName, String type, int version, boolean bRenewVersion) {
        AiModelBO aiModelBO;
        if (bRenewVersion) {
            // 如果要更新版本号，那么根据shortName和type去数据库查找最新的记录
            Optional<AiModelBO> optionalAiModelEntity = dao.findTopByTypeAndShortNameOrderByVersionDesc(type, shortName);
            if (optionalAiModelEntity.isPresent()) {
                AiModelBO oldAiModelEntity = optionalAiModelEntity.get();

                // 用新版本号创建一个AiModelEntity
                aiModelBO = new AiModelBO();
                BeanUtil.copyProperties(aiModelVO, aiModelBO);
                aiModelBO.setId(null);
                aiModelBO.setPort(dockerService.getRandomPort());
                aiModelBO.setVersion(oldAiModelEntity.getVersion() + 1);
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
                aiModelBO.setId(null);
                aiModelBO.setPort(dockerService.getRandomPort());
                aiModelBO.setVersion(1);
                try {
                    aiModelBO.setParam(objectMapper.writeValueAsString(aiModelVO.getParam()));
                } catch (JsonProcessingException e) {
                    log.error("e = ", e);
                    throw new AlgorithmException(ResultEnum.JSON_ERROR);
                }
            }
        } else {
            Optional<AiModelBO> optionalAiModelEntity = dao.findByTypeAndShortNameAndVersion(type, shortName, version);
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
                try {
                    aiModelBO.setParam(objectMapper.writeValueAsString(aiModelVO.getParam()));
                } catch (JsonProcessingException e) {
                    log.error("e = ", e);
                    throw new AlgorithmException(ResultEnum.JSON_ERROR);
                }
                aiModelBO.setVersion(1);
            }
        }
        return mapper.toVO(aiModelBO);
    }

    @Override
    public void delete(AiModelVO aiModelVO) {
        dao.deleteById(aiModelVO.getId());
    }

    @Override
    public AiModelVO findByTypeAndShortNameAndVersion(String type, String shortName, int version) {
        AiModelBO aiModelBO = dao.findByTypeAndShortNameAndVersion(type, shortName, version)
                .orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        return mapper.toVO(aiModelBO);
    }

    @Override
    public List<AiModelVO> findByShortName(String shortName) {
        List<AiModelBO> aiModelBOList = dao.findByShortName(shortName);
        return mapper.toVO(aiModelBOList);
    }
}
