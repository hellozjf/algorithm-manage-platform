package com.zrar.algorithm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.zrar.algorithm.config.CustomConfig;
import com.zrar.algorithm.constant.ModelParamEnum;
import com.zrar.algorithm.constant.ModelTypeEnum;
import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.domain.ModelEntity;
import com.zrar.algorithm.dto.Indexes;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.repository.ModelRepository;
import com.zrar.algorithm.service.FileService;
import com.zrar.algorithm.service.MLeapService;
import com.zrar.algorithm.service.RemoteService;
import com.zrar.algorithm.util.JsonUtils;
import com.zrar.algorithm.util.ResultUtils;
import com.zrar.algorithm.vo.PredictResultVO;
import com.zrar.algorithm.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jingfeng Zhou
 */
@RestController
@Slf4j
public class ModelController {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MLeapService mLeapService;

    @Autowired
    private ModelRepository mLeapRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private RemoteService remoteService;

    @Autowired
    private ModelRepository modelRepository;

    @Value("${spring.profiles.active}")
    private String active;

    @Autowired
    private Runtime runtime;

    @Autowired
    private CloseableHttpClient httpClient;

    /**
     * 预测某句话的分类，以及这个分类的可信度
     * predict实际上会发送
     * {"schema":{"fields":[{"name":"word","type":"string"}]},"rows":[["增值税 的 税率 是 多少"]]}
     *
     * @param modelName
     * @param sentence
     * @return
     */
    @PostMapping("/{modelName}/predict")
    public ResultVO predict(@PathVariable("modelName") String modelName,
                            @RequestBody String sentence) {
        ModelEntity modelEntity = modelRepository.findByName(modelName);
        if (modelEntity.getType() == ModelTypeEnum.MLEAP.getCode()) {
            // mleap的模型

            // 首先获取参数
            String params = null;
            try {
                params = getParams(sentence, modelEntity.getType(), modelEntity.getParam());
            } catch (Exception e) {
                log.error("e = {}", e);
                ResultUtils.error(ResultEnum.GET_PARAMS_ERROR.getCode(), e.getMessage());
            }
            if (StringUtils.isEmpty(params)) {
                ResultUtils.error(ResultEnum.GET_PARAMS_ERROR);
            }

            // 然后预测结果
            String ps = null;
            try {
                ps = doPredict(params, "/mleap/" + modelName + "/transform");
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.PREDICT_ERROR.getCode(), e.getMessage());
            }
            return ResultUtils.success(getMLeapPredictResultVO(ps, sentence, params));

        } else if (modelEntity.getType() == ModelTypeEnum.TENSORFLOW.getCode() &&
                    modelEntity.getParam() == ModelParamEnum.TENSORFLOW_DIRTY_WORD.getCode()) {
            // tensorflow的模型

            // 首先获取参数
            String params = null;
            try {
                params = getParams(sentence, modelEntity.getType(), modelEntity.getParam());
            } catch (Exception e) {
                ResultUtils.error(ResultEnum.GET_PARAMS_ERROR);
            }
            if (StringUtils.isEmpty(params)) {
                ResultUtils.error(ResultEnum.GET_PARAMS_ERROR);
            }

            // 然后预测结果
            String ps = null;
            try {
                ps = doPredict(params, "/tensorflow/" + modelName + "/v1/models/" + modelName + ":predict");
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.PREDICT_ERROR.getCode(), e.getMessage());
            }
            return ResultUtils.success(getTensorflowDirtywordPredictResultVO(ps, sentence, params));
        }

        return ResultUtils.error(ResultEnum.UNKNOWN_MODEL_TYPE);
    }

    private String getParams(String sentence, int modelType, int modelParam) throws Exception {
        if (modelType == ModelTypeEnum.MLEAP.getCode()) {
            URI uri = new URIBuilder()
                    .setScheme("http")
                    .setHost(customConfig.getBridgeIp())
                    .setPort(customConfig.getBridgePort())
                    .setPath("/mleap/params/transformer")
                    .build();
            HttpPost httpPost = new HttpPost(uri);
            List<NameValuePair> formparams = new ArrayList<>();
            formparams.add(new BasicNameValuePair("sentence", sentence));
            formparams.add(new BasicNameValuePair("paramCode", String.valueOf(modelParam)));
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
            httpPost.setEntity(formEntity);

            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String res = EntityUtils.toString(entity);
            return "{\"schema\":{\"fields\":[{\"name\":\"word\",\"type\":\"string\"}]},\"rows\":[[\"" + res + "\"]]}";
        } else if (modelType == ModelTypeEnum.TENSORFLOW.getCode()) {
            URI uri = new URIBuilder()
                    .setScheme("http")
                    .setHost(customConfig.getBridgeIp())
                    .setPort(customConfig.getBridgePort()).setPath("/tensorflow/dirtyword/params/transformer")
                    .setParameter("sentence", sentence)
                    .build();
            HttpPost httpPost = new HttpPost(uri);
            List<NameValuePair> formparams = new ArrayList<>();
            formparams.add(new BasicNameValuePair("sentence", sentence));
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
            httpPost.setEntity(formEntity);

            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String res = EntityUtils.toString(entity);
            return "{\"instances\": [" + res + "]}";
        } else {
            log.error("unknown modelType = {}", modelType);
            throw new AlgorithmException(ResultEnum.UNKNOWN_MODEL_TYPE);
        }
    }

    private String doPredict(String params, String path) throws Exception {
        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(customConfig.getBridgeIp())
                .setPort(customConfig.getBridgePort())
                .setPath(path)
                .build();
        HttpPost httpPost = new HttpPost(uri);
        StringEntity stringEntity = new StringEntity(params, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
    }

    private PredictResultVO getTensorflowDirtywordPredictResultVO(String ps, String sentence, String params) {
        // 先将预测结果转化为JsonNode
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(ps);
        } catch (IOException e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }

        JsonNode predictions = jsonNode.get("predictions");
        ArrayNode arrayNode = (ArrayNode) predictions;
        ArrayNode arrayNode1 = (ArrayNode) arrayNode.get(0);
        double isDirtyword = arrayNode1.get(1).asDouble();

        JsonNode paramsNode = null;
        try {
            paramsNode = objectMapper.readTree(params);
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        PredictResultVO predictResultVO = new PredictResultVO();
        predictResultVO.setSentence(sentence);
        predictResultVO.setParams(paramsNode);
        predictResultVO.setPredict(isDirtyword >= 0.5 ? 1 : 0);
        predictResultVO.setPredictString(isDirtyword >= 0.5 ? "脏话" : "非脏话");
        predictResultVO.setProbability(isDirtyword);
        return predictResultVO;
    }

    private PredictResultVO getMLeapPredictResultVO(String ps, String sentence, String params) {

        // 先将预测结果转化为JsonNode
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(ps);
        } catch (IOException e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }

        // 获取mlType和probability的index
        Indexes indexes = JsonUtils.getIndexes(objectMapper, jsonNode);
        if (indexes == null || indexes.getIndexPredict() == null || indexes.getIndexProbability() == null) {
            // 如果概率，预测项目为空，返回预测错误
            // 注意预测字符串可能为空，因为晓曦的模型返回的mlType确实不存在
            throw new AlgorithmException(ResultEnum.PREDICT_ERROR);
        }
        log.debug("indexMlType={}, indexPredict={}, indexProbability={}",
                indexes.getIndexMlType(), indexes.getIndexPredict(), indexes.getIndexProbability());

        // 取出预测字符串和预测的概率
        PredictResultVO predictResultVO = new PredictResultVO();
        JsonNode rows = jsonNode.get("rows");
        if (rows.isArray()) {
            ArrayNode arrayNode = (ArrayNode) rows;
            JsonNode row = arrayNode.get(0);

            Integer predict = null;
            String predictString = null;
            Double predictProbability = null;
            if (row.isArray()) {
                arrayNode = (ArrayNode) row;
                if (indexes.getIndexMlType() != null) {
                    predictString = arrayNode.get(indexes.getIndexMlType()).asText();
                }
                log.debug("values = {}", arrayNode.get(indexes.getIndexProbability()).get("values"));
                predict = arrayNode.get(indexes.getIndexPredict()).intValue();
                predictProbability = arrayNode.get(indexes.getIndexProbability()).get("values").get(predict).asDouble();
                // 判断是否有错误
                if (predictProbability == null) {
                    // 如果取不到预测的概率，返回预测错误
                    // 注意，字符串确实有可能取不到
                    throw new AlgorithmException(ResultEnum.PREDICT_ERROR);
                }
            }

            JsonNode paramsNode = null;
            try {
                paramsNode = objectMapper.readTree(params);
            } catch (Exception e) {
                log.error("e = {}", e);
            }
            predictResultVO.setSentence(sentence);
            predictResultVO.setParams(paramsNode);
            predictResultVO.setPredict(predict);
            predictResultVO.setPredictString(predictString);
            predictResultVO.setProbability(predictProbability);
        }

        return predictResultVO;
    }
}
