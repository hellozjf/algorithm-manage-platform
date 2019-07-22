package com.zrar.algorithm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zrar.algorithm.config.CustomConfig;
import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.repository.ModelRepository;
import com.zrar.algorithm.service.FileService;
import com.zrar.algorithm.service.MLeapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
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
    private ModelRepository modelRepository;

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private FileService fileService;

    @Override
    public String online(String modelName) {
        // 模型上线的时候可能没有这么快，所以我尝试10次，每次间隔5秒
        for (int i = 0; ; i++) {
            try {
                // 获取模型上线的URL
                String url = getOnlineUrl(modelName);
                // 模型的位置
                ObjectNode objectNode = objectMapper.createObjectNode();
                objectNode.put("path", fileService.getModelInnerPath(modelName));
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
                log.info("第{}次上线模型{}成功", i + 1, modelName);
                return responseBody;
            } catch (Exception e) {
                log.error("第{}次上线模型{}失败", i + 1, modelName);
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e1) {
                    log.error("e1 = {}", e1);
                }
            }
        }
    }

    @Override
    public String offline(String modelName) {
        // 获取模型上线的URL
        String url = getOfflineUrl(modelName);
        // 发送DELETE请求，删除模型
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
        // 返回删除的结果
        return response.getBody();
    }

    @Override
    public String transform(String modelName, String data) {
        // 获取模型预测的URL
        String url = getTransformUrl(modelName);
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

    private String getOnlineUrl(String modelName) {
        return getModelUrl(modelName);
    }

    private String getOfflineUrl(String modelName) {
        return getModelUrl(modelName);
    }

    private String getModelUrl(String modelName) {
        return getUrl(modelName, "model");
    }

    private String getTransformUrl(String modelName) {
        return getUrl(modelName, "transform");
    }

    private String getUrl(String modelName, String type) {
        // 构造mleap-bridge能够接收的地址
        String url = "http://" + customConfig.getBridgeIp() + ":" + customConfig.getBridgePort() + "/mleap/" + modelName + "/" + type;
        return url;
    }
}
