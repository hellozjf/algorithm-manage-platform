package com.zrar.ai.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.zrar.ai.bo.AiModelBO;
import com.zrar.ai.bo.DictItemBO;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.vo.AiModelVO;
import com.zrar.ai.vo.DictItemVO;
import com.zrar.ai.vo.ModelParamVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.io.IOException;

/**
 * VO与BO互相转换工具类
 * 参考链接：https://stackoverflow.com/questions/48492207/how-can-i-convert-string-to-map-using-mapstruct/48510197#48510197
 * @author hellozjf
 * @date 2019-11-08
 */
@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AiModelMapper extends BaseMapper<AiModelVO, AiModelBO> {

    default String map(ModelParamVO value) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }
    }

    default ModelParamVO map(String value) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(value, new TypeReference<ModelParamVO>(){});
        } catch (IOException e) {
            e.printStackTrace();
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }
    }
}