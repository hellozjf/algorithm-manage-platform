package com.zrar.algorithm.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.zrar.algorithm.service.DictMapService;
import com.zrar.algorithm.service.StopWordService;
import com.zrar.algorithm.util.JsonUtils;
import com.zrar.algorithm.util.ResultUtils;
import com.zrar.algorithm.util.WordUtils;
import com.zrar.algorithm.vo.PredictResultVO;
import com.zrar.algorithm.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jingfeng Zhou
 */
@RestController
@Slf4j
public class ModelController {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private ModelRepository modelRepository;

    @Value("${spring.profiles.active}")
    private String active;

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private DictMapService dictMapService;

    @Autowired
    private StopWordService stopWordService;

    /**
     * 用模型预测sentence，预测分为两个步骤，第一步获取参数，第二步使用参数预测结果
     *
     * @param modelName
     * @param sentence
     * @return
     */
    @PostMapping("/{modelName}/predict")
    public ResultVO predict(@PathVariable("modelName") String modelName,
                            @RequestBody String sentence) {
        ModelEntity modelEntity = modelRepository.findByName(modelName).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        long beforeGetParams = 0L;
        long afterGetParams = 0L;
        long beforeDoPredict = 0L;
        long afterDoPredict = 0L;
        if (modelEntity.getType() == ModelTypeEnum.MLEAP.getCode()) {
            // mleap的模型

            // 首先获取参数
            String params = null;
            try {
                beforeGetParams = System.currentTimeMillis();
                params = getParams(sentence, modelEntity.getType(), modelEntity.getParam());
                afterGetParams = System.currentTimeMillis();
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
                beforeDoPredict = System.currentTimeMillis();
                ps = doPredict(params, "/mleap/" + modelName + "/transform");
                afterDoPredict = System.currentTimeMillis();
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.PREDICT_ERROR.getCode(), e.getMessage());
            }
            return ResultUtils.success(getMLeapPredictResultVO(ps, sentence, params,
                    afterGetParams - beforeGetParams,
                    afterDoPredict - beforeDoPredict));

        } else if (modelEntity.getType() == ModelTypeEnum.TENSORFLOW.getCode()) {
            // tensorflow的模型

            // 首先获取参数
            String params = null;
            try {
                beforeGetParams = System.currentTimeMillis();
                params = getParams(sentence, modelEntity.getType(), modelEntity.getParam());
                afterGetParams = System.currentTimeMillis();
            } catch (Exception e) {
                ResultUtils.error(ResultEnum.GET_PARAMS_ERROR);
            }
            if (StringUtils.isEmpty(params)) {
                ResultUtils.error(ResultEnum.GET_PARAMS_ERROR);
            }

            // 然后预测结果
            String ps = null;
            try {
                beforeDoPredict = System.currentTimeMillis();
                ps = doPredict(params, "/tensorflow/" + modelName + "/v1/models/" + modelName + ":predict");
                afterDoPredict = System.currentTimeMillis();
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.PREDICT_ERROR.getCode(), e.getMessage());
            }

            if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_DIRTY_WORD.getCode()) {
                // 脏话模型的结果
                return ResultUtils.success(getTensorflowDirtywordPredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_SENTIMENT_ANALYSIS.getCode()) {
                // 情感分析的结果
                return ResultUtils.success(getTensorflowSentimentAnalysisPredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_IS_TAX_ISSUE.getCode()) {
                // 是否税务问题模型的结果
                return ResultUtils.success(getTensorflowIsTaxIssuePredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_QA.getCode()) {
                // 是否是问答模型的结果
                return ResultUtils.success(getTensorflowQAPredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_SHEBAO.getCode()) {
                // 社保模型的结果
                return ResultUtils.success(getTensorflowSocialSecurityResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else {
                return ResultUtils.success(getTensorflowResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            }
        }

        return ResultUtils.error(ResultEnum.UNKNOWN_MODEL_TYPE);
    }

    /**
     * 获取分词数据
     * @param sentence
     * @param paramCode
     * @return
     */
    @GetMapping("/getRawMLeapParams")
    public String getRawMLeapParams(String sentence, int paramCode) {
        String wordCut = null;
        if (paramCode == ModelParamEnum.MLEAP_CUT_WORD.getCode()) {
            wordCut = WordUtils.wordCut(sentence, "");
        } else if (paramCode == ModelParamEnum.MLEAP_CUT_WORD_VSWZYC.getCode()) {
            wordCut = WordUtils.wordCut(sentence, ModelParamEnum.MLEAP_CUT_WORD_VSWZYC.getNature());
        } else if (paramCode == ModelParamEnum.MLEAP_PHRASE_LIST.getCode()) {
            wordCut = WordUtils.phraseList(sentence);
        }
        log.debug("sentence = {}, paramCode = {}, wordCut = {}", sentence, paramCode, wordCut);
        return wordCut;
    }

    private String getRawPythonTensorflowParams(String sentence, int paramCode, String other) {
        try {
            URI uri = new URIBuilder()
                    .setScheme("http")
                    .setHost(customConfig.getBridgeIp())
                    .setPort(customConfig.getBridgePort()).setPath("/tensorflow/params/transformer")
                    .build();
            HttpPost httpPost = new HttpPost(uri);
            List<NameValuePair> formparams = new ArrayList<>();
            formparams.add(new BasicNameValuePair("sentence", sentence));
            formparams.add(new BasicNameValuePair("paramCode", String.valueOf(paramCode)));
            formparams.add(new BasicNameValuePair("other", other));
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
            httpPost.setEntity(formEntity);

            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String res = EntityUtils.toString(entity);
            return res;
        } catch (Exception e) {
            log.error("e = {}", e);
            return null;
        }
    }

    /**
     * 通过Java获取问答模型Tensorflow前处理后的参数
     * @param sentence
     * @param paramCode
     * @return
     */
    private String getRawJavaQaTensorflowParams(String sentence, int paramCode) {

        // 最大词长度120
        int maxLength = 120;

        // 加载停用词
        List<String> stopWordList = stopWordService.getStopWordByPath("static/tensorflow/qa/stopwords.txt");

        // 加载词典
        Map<String, Integer> dictMap = dictMapService.getDictMapByPath("static/tensorflow/qa/vocabulary.txt");

        // 将答案“增值税发票系统升级版纳税人端税控设备包括金税盘和税控盘。”进行切词，并过滤掉其中的停用词，并将切词转向量
        String answerString = "增值税发票系统升级版纳税人端税控设备包括金税盘和税控盘。";
        List<String> answerStringList = WordUtils.getWordCutList(answerString, "").stream()
                .filter(wordCut -> !stopWordList.contains(wordCut))
                .collect(Collectors.toList());
        log.debug("{}", answerStringList);
        List<Integer> answer = answerStringList.stream().map(wordCut -> dictMap.get(wordCut) == null ? 0 : dictMap.get(wordCut))
                .collect(Collectors.toList());

        // 获取答案切词长度，并补足或截断长度
        int answerLen = answer.size();
        answer = fillLength(maxLength, answer, answerLen);

        // 将问题也进行切词，并过滤掉其中的停用词，结果以空格分隔
        List<Integer> question = WordUtils.getWordCutList(sentence, "").stream()
                .filter(wordCut -> !stopWordList.contains(wordCut))
                .map(wordCut -> dictMap.get(wordCut) == null ? 0 : dictMap.get(wordCut))
                .collect(Collectors.toList());

        // 获取问题切词长度，并补足或截断长度
        int questionLen = question.size();
        question = fillLength(maxLength, question, questionLen);

        // 返回{question:[], question_len:[], answer:[], answer_len:[]}
        Map<String, Object> map = new HashMap<>();
        map.put("question", question);
        map.put("question_len", Arrays.asList(questionLen));
        map.put("answer", answer);
        map.put("answer_len", Arrays.asList(answerLen));
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }
    }

    private List<Integer> fillLength(int maxLength, List<Integer> answer, int answerLen) {
        if (answerLen < maxLength) {
            // 不足最大长度就补零
            for (int i = answerLen; i < maxLength; i++) {
                answer.add(0);
            }
        } else if (answerLen > maxLength) {
            // 大于最大长度就截断
            answer = answer.subList(0, maxLength);
        }
        return answer;
    }

    /**
     * 通过Java获取Tensorflow的向量
     * @param sentence
     * @return
     */
    private String getRawJavaTensorflowParams(String sentence, int paramCode) {

        if (paramCode == ModelParamEnum.TENSORFLOW_SENTIMENT_ANALYSIS.getCode()) {
            // 情感分析，替换掉标点符号
            sentence.replaceAll("\\W", "");
        }

        // 这里我不知道怎么切割出单个字和完整的数字，所以我先全部切成单个，切完再处理成完整的数字
        String[] words = sentence.split("");
        List<String> wordList = new ArrayList<>();
        wordList.add("[CLS]");
        StringBuilder number = new StringBuilder();
        for (String word : words) {
            if (org.apache.commons.lang3.StringUtils.isNumeric(word)) {
                // 数字先放到缓冲区缓存起来
                number.append(word);
            } else {
                if (! StringUtils.isEmpty(number.toString())) {
                    // 将之前缓存的数字放入list中
                    wordList.add(number.toString());
                    // 清除缓存的数字
                    number.setLength(0);
                }
                wordList.add(word);
            }
        }
        if (! StringUtils.isEmpty(number.toString())) {
            // 将之前缓存的数字放入list中
            wordList.add(number.toString());
            // 清除缓存的数字
            number.setLength(0);
        }
        wordList.add("[SEP]");

        Map<String, Integer> dictMap = dictMapService.getDictMapByPath("static/tensorflow/vocab.txt");
        List<Integer> integerList = wordList.stream().map(word -> {
            Integer integer = dictMap.get(word);
            if (integer == null) {
                return dictMap.get("[UNK]");
            } else {
                return integer;
            }
        }).collect(Collectors.toList());

        int maxSeqLength = 128;
        List<Integer> inputIds = new ArrayList<>();
        List<Integer> inputMask = new ArrayList<>();
        List<Integer> segmentIds = new ArrayList<>();
        if (integerList.size() > maxSeqLength) {
            // 如果integerList长度太长，则它第128个元素置为结束符
            integerList.set(maxSeqLength - 1, integerList.get(integerList.size() - 1));
        } else if (integerList.size() < maxSeqLength) {
            // 如果integerList长度不够，则需要补零
            for (int i = integerList.size(); i < maxSeqLength; i++) {
                integerList.add(0);
            }
        }
        for (int i = 0; i < maxSeqLength; i++) {
            int t = integerList.get(i);
            inputIds.add(t);
            if (t > 0) {
                inputMask.add(1);
            } else {
                inputMask.add(0);
            }
            segmentIds.add(0);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("input_ids", inputIds);
        result.put("input_mask", inputMask);
        result.put("label_ids", 0);
        result.put("segment_ids", segmentIds);
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("e = {}", e);
            return "";
        }
    }

    /**
     * 获取tensorflow向量
     * @param sentence
     * @param paramCode
     * @return
     */
    @GetMapping("/getRawTensorflowParams")
    public String getRawTensorflowParams(String sentence, int paramCode) {
        if (paramCode == ModelParamEnum.TENSORFLOW_DIRTY_WORD.getCode() ||
            paramCode == ModelParamEnum.TENSORFLOW_IS_TAX_ISSUE.getCode() ||
            paramCode == ModelParamEnum.TENSORFLOW_SENTIMENT_ANALYSIS.getCode()) {
            return getRawJavaTensorflowParams(sentence, paramCode);
        } else if (paramCode == ModelParamEnum.TENSORFLOW_QA.getCode()) {
            // 问答模型处理很复杂，需要先通过预处理获取到参数，再用参数拿到中间结果，最后还要将中间结果经过后处理加工成最终结果
            return getRawJavaQaTensorflowParams(sentence, paramCode);
        } else if (paramCode == ModelParamEnum.TENSORFLOW_SHEBAO.getCode()) {
            return getRawPythonTensorflowParams(sentence, paramCode, "");
        }

        log.error("unknown paramCode = {}", paramCode);
        return null;
    }

    /**
     * 获取喂给mleap的数据
     * @param sentence
     * @param paramCode
     * @return
     */
    @GetMapping("/getMLeapParams")
    public String getMLeapParams(String sentence, int paramCode) {
        String res = getRawMLeapParams(sentence, paramCode);
        return "{\"schema\":{\"fields\":[{\"name\":\"word\",\"type\":\"string\"}]},\"rows\":[[\"" + res + "\"]]}";
    }

    /**
     * 获取喂给tensorflow的数据
     * @param sentence
     * @param paramCode
     * @return
     */
    @GetMapping("/getTensorflowParams")
    public String getTensorflowParams(String sentence, int paramCode) {
        String res = getRawTensorflowParams(sentence, paramCode);
        return "{\"instances\": [" + res + "]}";
    }

    /**
     * 获取参数
     * <p>
     * 如果是mleap，首先访问mleap-params-transformer获取到分词，然后拼成
     * {"schema":{"fields":[{"name":"word","type":"string"}]},"rows":[["增值税 的 税率 是 多少"]]}
     * <p>
     * 如果是tensorflow的脏话模型，首先访问tensorflow-dirtyword-params-transformer获取到向量，然后拼成
     * {"instances": [{"input_ids":[101,4318,1673,7027,1402,679,1139,6496,4280,102,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],"input_mask":[1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],"label_ids":0,"segment_ids":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]}]}
     *
     * @param sentence
     * @param modelType
     * @param modelParam
     * @return
     * @throws Exception
     */
    private String getParams(String sentence, int modelType, int modelParam) throws Exception {
        if (modelType == ModelTypeEnum.MLEAP.getCode()) {
            return getMLeapParams(sentence, modelParam);
        } else if (modelType == ModelTypeEnum.TENSORFLOW.getCode()) {
            return getTensorflowParams(sentence, modelParam);
        } else {
            log.error("unknown modelType = {}", modelType);
            throw new AlgorithmException(ResultEnum.UNKNOWN_MODEL_TYPE);
        }
    }

    /**
     * 调用tensorflow/serving或mleap-serving预测结果
     *
     * @param params
     * @param path
     * @return
     * @throws Exception
     */
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

    /**
     * 获取返回的tensorflow脏话预测结果VO
     *
     * @param ps
     * @param sentence
     * @param params
     * @param getParamsCostMs
     * @param predictCostMs
     * @return
     */
    private PredictResultVO getTensorflowDirtywordPredictResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {
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
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        return predictResultVO;
    }

    /**
     * 获取返回的tensorflow是否税务问题预测结果VO
     *
     * @param ps
     * @param sentence
     * @param params
     * @param getParamsCostMs
     * @param predictCostMs
     * @return
     */
    private PredictResultVO getTensorflowIsTaxIssuePredictResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {
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
        double isTaxIssue = arrayNode1.get(1).asDouble();

        JsonNode paramsNode = null;
        try {
            paramsNode = objectMapper.readTree(params);
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        PredictResultVO predictResultVO = new PredictResultVO();
        predictResultVO.setSentence(sentence);
        predictResultVO.setParams(paramsNode);
        predictResultVO.setPredict(isTaxIssue >= 0.5 ? 1 : 0);
        predictResultVO.setPredictString(isTaxIssue >= 0.5 ? "税务问题" : "非税务问题");
        predictResultVO.setProbability(isTaxIssue);
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        return predictResultVO;
    }

    /**
     * 获取返回的tensorflow问答模型预测结果VO
     *
     * @param ps                    喂给tensorflow模型后预测的结果
     * @param sentence              原始问题
     * @param params                原始问题的预处理结果
     * @param getParamsCostMs       预处理耗费的时间
     * @param predictCostMs         预测耗费的时间
     * @return
     */
    private Object getTensorflowQAPredictResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {

        // 首先拿着ps去查询
        long beforePost = System.currentTimeMillis();
        String result = getRawPythonTensorflowParams(sentence, ModelParamEnum.TENSORFLOW_QA.getCode(), ps);
        long afterPost = System.currentTimeMillis();

        // 先将预测结果转化为JsonNode
        JsonNode jsonNode = null;
        ArrayNode arrayNode = null;
        try {
            arrayNode = (ArrayNode) objectMapper.readTree(result);
            if (arrayNode.size() > 0) {
                jsonNode = arrayNode.get(0);
            }
        } catch (IOException e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }

        JsonNode paramsNode = null;
        try {
            paramsNode = objectMapper.readTree(params);
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        PredictResultVO predictResultVO = new PredictResultVO();
        predictResultVO.setSentence(sentence);
        predictResultVO.setParams(paramsNode);
        predictResultVO.setPredict(null);
        predictResultVO.setPredictString(jsonNode.get("a").asText());
        predictResultVO.setProbability(jsonNode.get("probs").asDouble());
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        predictResultVO.setPostCostMs(afterPost - beforePost);

        // 将arrayNode转化为List<JsonNode>
        List<JsonNode> jsonNodeList = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            jsonNodeList.add(node);
        }
        predictResultVO.setPredictList(jsonNodeList);
        return predictResultVO;
    }

    /**
     * 获取返回的tensorflow默认预测结果VO
     *
     * @param ps                    喂给tensorflow模型后预测的结果
     * @param sentence              原始问题
     * @param params                原始问题的预处理结果
     * @param getParamsCostMs       预处理耗费的时间
     * @param predictCostMs         预测耗费的时间
     * @return
     */
    private Object getTensorflowResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {

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
        double isTaxIssue = arrayNode1.get(1).asDouble();

        JsonNode paramsNode = null;
        try {
            paramsNode = objectMapper.readTree(params);
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        PredictResultVO predictResultVO = new PredictResultVO();
        predictResultVO.setSentence(sentence);
        predictResultVO.setParams(paramsNode);
        predictResultVO.setPredict(isTaxIssue >= 0.5 ? 1 : 0);
        predictResultVO.setProbability(isTaxIssue);
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        return predictResultVO;
    }

    /**
     * 获取返回的tensorflow社保预测结果VO
     *
     * @param ps                    喂给tensorflow模型后预测的结果
     * @param sentence              原始问题
     * @param params                原始问题的预处理结果
     * @param getParamsCostMs       预处理耗费的时间
     * @param predictCostMs         预测耗费的时间
     * @return
     */
    private Object getTensorflowSocialSecurityResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {

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
        int value = arrayNode.get(0).intValue();

        JsonNode paramsNode = null;
        try {
            paramsNode = objectMapper.readTree(params);
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        PredictResultVO predictResultVO = new PredictResultVO();
        predictResultVO.setSentence(sentence);
        predictResultVO.setParams(paramsNode);
        switch (value) {
            case 0:
                predictResultVO.setPredictString("养老保险");
                break;
            case 1:
                predictResultVO.setPredictString("医疗保险");
                break;
            case 2:
                predictResultVO.setPredictString("社保分类");
                break;
            case 3:
                predictResultVO.setPredictString("生育保险");
                break;
            case 4:
                predictResultVO.setPredictString("其他");
                break;
        }
        predictResultVO.setPredict(value);
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        return predictResultVO;
    }

    /**
     * 获取返回的tensorflow情感分析预测结果VO
     *
     * @param ps
     * @param sentence
     * @param params
     * @param getParamsCostMs
     * @param predictCostMs
     * @return
     */
    private PredictResultVO getTensorflowSentimentAnalysisPredictResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {
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
        double r0 = arrayNode1.get(0).asDouble();
        double r1 = arrayNode1.get(1).asDouble();

        JsonNode paramsNode = null;
        try {
            paramsNode = objectMapper.readTree(params);
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        PredictResultVO predictResultVO = new PredictResultVO();
        predictResultVO.setSentence(sentence);
        predictResultVO.setParams(paramsNode);
        predictResultVO.setPredict(r0 > 0.8 ? 0 : 1);
        predictResultVO.setPredictString("");
        predictResultVO.setProbability(r0 > 0.8 ? r0 : r1);
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        return predictResultVO;
    }

    /**
     * 获取返回的mleap预测结果VO
     *
     * @param ps
     * @param sentence
     * @param params
     * @param getParamsCostMs
     * @param predictCostMs
     * @return
     */
    private PredictResultVO getMLeapPredictResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {

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
            predictResultVO.setPreCostMs(getParamsCostMs);
            predictResultVO.setPredictCostMs(predictCostMs);
        }

        return predictResultVO;
    }
}
