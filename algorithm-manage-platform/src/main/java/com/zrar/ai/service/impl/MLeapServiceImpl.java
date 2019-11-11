package com.zrar.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zrar.ai.bo.AiModelBO;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.dao.AiModelDao;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.service.FileService;
import com.zrar.ai.service.FullNameService;
import com.zrar.ai.service.MLeapService;
import com.zrar.ai.vo.FullNameVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Jingfeng Zhou
 */
@Service
@Slf4j
public class MLeapServiceImpl implements MLeapService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private AiModelDao aiModelRepository;

    @Autowired
    private FullNameService fullNameService;

    @Override
    public String online(String fullName) {
        // 模型上线的时候可能没有这么快，所以我尝试20次，每次间隔5秒，这样还没有上线成功就报错
        for (int i = 0; i < 20; i++) {
            try {
                // 获取模型上线的URL
                String url = getOnlineUrl(fullName);
                // 模型的位置
                ObjectNode objectNode = objectMapper.createObjectNode();
                objectNode.put("path", fileService.getModelInnerFilePath(fullName));
                String requestBody = null;
                try {
                    requestBody = objectMapper.writeValueAsString(objectNode);
                } catch (JsonProcessingException e) {
                    log.error("e = {}", e);
                    throw new AlgorithmException(ResultEnum.JSON_ERROR);
                }
                // 构造PUT请求，上线模型
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
                // 返回上线的结果
                String responseBody = response.getBody();
                log.info("第{}次上线模型{}成功", i + 1, fullName);
                return responseBody;
            } catch (Exception e) {
                log.error("第{}次上线模型{}失败", i + 1, fullName);
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e1) {
                    log.error("e1 = {}", e1);
                }
            }
        }
        throw new AlgorithmException(ResultEnum.MODEL_ONLINE_FAILED);
    }

    @Override
    public String offline(String fullName) {
        // 获取模型上线的URL
        String url = getOfflineUrl(fullName);
        // 发送DELETE请求，删除模型
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
        // 返回删除的结果
        return response.getBody();
    }

    @Override
    public String transform(String fullName, String data) {
        // 获取模型预测的URL
        String url = getTransformUrl(fullName);
        // 获取待预测的数据
        String requestBody = data;
        // 发送POST请求，预测数据
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        // 返回预测结果
        String result = response.getBody();
        return result;
    }

    private String getOnlineUrl(String fullName) {
        return getModelUrl(fullName);
    }

    private String getOfflineUrl(String fullName) {
        return getModelUrl(fullName);
    }

    private String getModelUrl(String fullName) {
        return getUrl(fullName, "model");
    }

    private String getTransformUrl(String fullName) {
        return getUrl(fullName, "transform");
    }

    /**
     * 根据fullName，找出模型的type,shortName,version，然后找出模型对应的实体，查出模型的端口，返回URL
     * @param fullName
     * @param type
     * @return
     */
    private String getUrl(String fullName, String type) {
        FullNameVO fullNameVO = fullNameService.getByFullName(fullName);
        AiModelBO aiModelEntity = aiModelRepository.findByTypeAndShortNameAndVersion(
                fullNameVO.getType(),
                fullNameVO.getShortName(),
                fullNameVO.getVersion()).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        String url = "http://localhost:" + aiModelEntity.getPort() + "/" + type;
        return url;
    }
}
