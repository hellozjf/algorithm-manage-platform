package com.zrar.ai.controller;

import cn.hutool.core.util.RuntimeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.zrar.ai.bo.AiModelBO;
import com.zrar.ai.config.CustomDockerConfig;
import com.zrar.ai.constant.DictItem;
import com.zrar.ai.constant.ResultEnum;
import com.zrar.ai.dao.AiModelDao;
import com.zrar.ai.exception.AlgorithmException;
import com.zrar.ai.service.CutService;
import com.zrar.ai.service.FullNameService;
import com.zrar.ai.service.StopWordService;
import com.zrar.ai.service.VocabMapService;
import com.zrar.ai.util.JsonUtils;
import com.zrar.ai.util.ResultUtils;
import com.zrar.ai.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jingfeng Zhou
 */
@RestController
@Slf4j
public class ModelController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${server.port}")
    private String port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private VocabMapService vocabMapService;

    @Autowired
    private StopWordService stopWordService;

    @Autowired
    private AiModelDao aiModelRepository;

    @Autowired
    private CustomDockerConfig customDockerConfig;

    @Autowired
    private FullNameService fullNameService;

    @Autowired
    private CutService cutService;

    @PostMapping("/{shortName}/metadata")
    public ResultVO metadata(@PathVariable("shortName") String shortName,
                             @RequestBody String sentence) {
        // 将sentence打包成PredictVO
        PredictVO predictVO = unpackSentence(shortName, sentence);
        // 获取对应的实例
        AiModelBO aiModelEntity = aiModelRepository.findByTypeAndShortNameAndVersion(
                predictVO.getType(),
                predictVO.getShortName(),
                predictVO.getVersion()).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        FullNameVO fullNameVO = fullNameService.getByAiModel(aiModelEntity);

        if (aiModelEntity.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_MLEAP)) {
            return ResultUtils.error(ResultEnum.METADATA_DOES_NOT_SUPPORT_MLEAP);
        } else {
            try {
                String result = doGetMetadata(aiModelEntity.getPort(), fullNameVO.getFullName());
                JsonNode jsonNode = objectMapper.readTree(result);
                return ResultUtils.success(jsonNode);
            } catch (Exception e) {
                log.error("e = ", e);
                return ResultUtils.error(ResultEnum.GET_METADATA_ERROR);
            }
        }
    }

    /**
     * 用模型预测sentence，预测分为两个步骤，第一步获取参数，第二步使用参数预测结果
     *
     * @param shortName
     * @param sentence
     * @return
     */
    @PostMapping("/{shortName}/predict")
    public ResultVO predict(@PathVariable("shortName") String shortName,
                            @RequestBody String sentence) {

        // 将sentence打包成PredictVO
        PredictVO predictVO = unpackSentence(shortName, sentence);
        // 获取对应的实例
        AiModelBO aiModelEntity = aiModelRepository.findByTypeAndShortNameAndVersion(
                predictVO.getType(),
                predictVO.getShortName(),
                predictVO.getVersion()).orElseThrow(() -> new AlgorithmException(ResultEnum.CAN_NOT_FIND_MODEL_ERROR));
        FullNameVO fullNameVO = fullNameService.getByAiModel(aiModelEntity);
        // 将modelParam转化为ModelParamVO
        ModelParamVO modelParamVO = null;
        try {
            modelParamVO = objectMapper.readValue(aiModelEntity.getParam(), ModelParamVO.class);
        } catch (IOException e) {
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }

        // 要记录getParam和doPredict所需要花费的时间
        long beforeGetParams = 0L;
        long afterGetParams = 0L;
        long beforeDoPredict = 0L;
        long afterDoPredict = 0L;

        if (aiModelEntity.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_MLEAP)) {
            // mleap的模型

            // 首先获取参数
            String params = null;
            try {
                beforeGetParams = System.currentTimeMillis();
                params = getMLeapParams(predictVO.getSentence(), modelParamVO.getCutMethod());
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
                ps = doMLeapPredict(aiModelEntity.getPort(), params);
//                ps = doPredict(params, "/mleap/" + shortName + "/transform");
                afterDoPredict = System.currentTimeMillis();
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.PREDICT_ERROR.getCode(), e.getMessage());
            }
            return ResultUtils.success(getMLeapPredictResultVO(ps, sentence, params,
                    afterGetParams - beforeGetParams,
                    afterDoPredict - beforeDoPredict));

        } else if (aiModelEntity.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_TENSORFLOW)) {
            // tensorflow的模型

            // 首先获取参数
            String params = null;
            try {
                beforeGetParams = System.currentTimeMillis();
                params = getTensorflowParams(predictVO.getSentence(), modelParamVO);
//                params = getParams(predictVO.getSentence(), aiModelEntity.getType(), modelParamVO);
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
//                ps = doPredict(params, "/tensorflow/" + shortName + "/v1/models/" + shortName + ":predict");
                ps = doTensorflowPredict(aiModelEntity.getPort(), fullNameVO.getFullName(), params);
                afterDoPredict = System.currentTimeMillis();
            } catch (Exception e) {
                log.error("e = {}", e);
                throw new AlgorithmException(ResultEnum.PREDICT_ERROR.getCode(), e.getMessage());
            }

            // TODO 每增加一个模型，需要增加一个分支
            if (modelParamVO.getModelName().equalsIgnoreCase(DictItem.MODEL_NAME_DIRTY_WORD)) {
                // 脏话模型的结果
                return ResultUtils.success(getTensorflowDirtywordPredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelParamVO.getModelName().equalsIgnoreCase(DictItem.MODEL_NAME_SENTIMENT_ANALYSIS)) {
                // 情感分析的结果
                return ResultUtils.success(getTensorflowSentimentAnalysisPredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelParamVO.getModelName().equalsIgnoreCase(DictItem.MODEL_NAME_IS_TAX_ISSUE)) {
                // 是否税务问题模型的结果
                return ResultUtils.success(getTensorflowIsTaxIssuePredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelParamVO.getModelName().equalsIgnoreCase(DictItem.MODEL_NAME_AP_BILSTM)) {
                // 是否是ap_bilstm模型的结果
                return ResultUtils.success(getTensorflowApBilstmPredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelParamVO.getModelName().equalsIgnoreCase(DictItem.MODEL_NAME_RERANKING)) {
                // 是否是reranking模型的结果
                return ResultUtils.success(getTensorflowRerankingPredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelParamVO.getModelName().equalsIgnoreCase(DictItem.MODEL_NAME_SHEBAO)) {
                // 社保模型的结果
                return ResultUtils.success(getTensorflowSocialSecurityResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelParamVO.getModelName().equalsIgnoreCase(DictItem.MODEL_NAME_FIRSTALL)) {
                // 三分类模型的结果
                return ResultUtils.success(getTensorflowFirstAllResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelParamVO.getModelName().equalsIgnoreCase(DictItem.MODEL_NAME_SYNTHESIS)) {
                // 综合模型
                return ResultUtils.success(getTensorflowSynthesisResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelParamVO.getModelName().equalsIgnoreCase(DictItem.MODEL_NAME_CITY_MANAGEMENT)) {
                // 城管模型
                return ResultUtils.success(getTensorflowCityManagementResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelParamVO.getModelName().equalsIgnoreCase(DictItem.MODEL_NAME_ZNZX)) {
                // 智能咨询-场景分类模型
                return ResultUtils.success(getTensorflowZnzxParams(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelParamVO.getModelName().equalsIgnoreCase(DictItem.MODEL_NAME_BERT_MATCH)) {
                // 是否是bert_match模型的结果
                return ResultUtils.success(getTensorflowBertMatchPredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else {
                return ResultUtils.success(getTensorflowResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            }
        } else if (aiModelEntity.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_COMPOSE)) {
            String[] modelsArray = modelParamVO.getCompose().split(",");
            String result = sentence;
            // 依次调用模型
            ResultVO resultVO = null;
            synchronized (this) {
                long totalPreCostMs = 0L;
                long totalPredictCostMs = 0L;
                long totalPostCostMs = 0L;
                for (String model : modelsArray) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.valueOf("application/json;UTF-8"));
                    org.springframework.http.HttpEntity<String> stringEntity = new org.springframework.http.HttpEntity<>(result, headers);
                    String url = "http://localhost:" + port + contextPath + "/" + model + "/predict";
                    resultVO = restTemplate.postForObject(url, stringEntity, ResultVO.class);
                    Map map = (Map) resultVO.getData();
                    Object nextInput = map.get("nextInput");
                    Object preCostMs = map.get("preCostMs");
                    Object predictCostMs = map.get("predictCostMs");
                    Object postCostMs = map.get("postCostMs");
                    result = nextInput == null ? "" : nextInput.toString();
                    totalPreCostMs += preCostMs == null ? 0 : (int) preCostMs;
                    totalPredictCostMs += predictCostMs == null ? 0 : (int) predictCostMs;
                    totalPostCostMs += postCostMs == null ? 0 : (int) postCostMs;
                }

                // 更新调用时间
                Map map = (Map) resultVO.getData();
                map.put("preCostMs", totalPreCostMs);
                map.put("predictCostMs", totalPredictCostMs);
                map.put("postCostMs", totalPostCostMs);
            }
            return resultVO;
        }

        return ResultUtils.error(ResultEnum.UNKNOWN_MODEL_TYPE);
    }

    /**
     * 获取分词数据
     *
     * @param sentence
     * @param cutMethod
     * @return
     */
    public String getRawMLeapParams(String sentence, String cutMethod) {
        return cutService.getStringByMethod(sentence, cutMethod);
    }

//    /**
//     * 通过Java获取问答模型Tensorflow前处理后的参数
//     *
//     * @param sentence
//     * @param paramCode
//     * @return
//     */
//    private String getRawJavaQaTensorflowParams(String sentence, int paramCode) {
//
//        // 最大词长度120
//        int maxLength = 120;
//
//        // 加载停用词
//        List<String> stopWordList = stopWordService.getStopWordByPath("static/tensorflow/qa/stopwords.txt");
//
//        // 加载词典
//        Map<String, Integer> dictMap = vocabMapService.getVocabMapByPath("static/tensorflow/qa/vocabulary.txt");
//
//        // 将答案“增值税发票系统升级版纳税人端税控设备包括金税盘和税控盘。”进行切词，并过滤掉其中的停用词，并将切词转向量
//        String answerString = "增值税发票系统升级版纳税人端税控设备包括金税盘和税控盘。";
//        List<String> answerStringList = WordUtils.getWordCutList(answerString, "").stream()
//                .filter(wordCut -> !stopWordList.contains(wordCut))
//                .collect(Collectors.toList());
//        log.debug("{}", answerStringList);
//        List<Integer> answer = answerStringList.stream().map(wordCut -> dictMap.get(wordCut) == null ? 0 : dictMap.get(wordCut))
//                .collect(Collectors.toList());
//
//        // 获取答案切词长度，并补足或截断长度
//        int answerLen = answer.size();
//        answer = fillLength(maxLength, answer, answerLen);
//
//        // 将问题也进行切词，并过滤掉其中的停用词，结果以空格分隔
//        List<Integer> question = WordUtils.getWordCutList(sentence, "").stream()
//                .filter(wordCut -> !stopWordList.contains(wordCut))
//                .map(wordCut -> dictMap.get(wordCut) == null ? 0 : dictMap.get(wordCut))
//                .collect(Collectors.toList());
//
//        // 获取问题切词长度，并补足或截断长度
//        int questionLen = question.size();
//        question = fillLength(maxLength, question, questionLen);
//
//        // 返回{question:[], question_len:[], answer:[], answer_len:[]}
//        Map<String, Object> map = new HashMap<>();
//        map.put("question", question);
//        map.put("question_len", Arrays.asList(questionLen));
//        map.put("answer", answer);
//        map.put("answer_len", Arrays.asList(answerLen));
//        try {
//            return objectMapper.writeValueAsString(map);
//        } catch (JsonProcessingException e) {
//            log.error("e = {}", e);
//            throw new AlgorithmException(ResultEnum.JSON_ERROR);
//        }
//    }
//
//    private List<Integer> fillLength(int maxLength, List<Integer> answer, int answerLen) {
//        if (answerLen < maxLength) {
//            // 不足最大长度就补零
//            for (int i = answerLen; i < maxLength; i++) {
//                answer.add(0);
//            }
//        } else if (answerLen > maxLength) {
//            // 大于最大长度就截断
//            answer = answer.subList(0, maxLength);
//        }
//        return answer;
//    }

    /**
     * 通过Java获取tensorflow的参数向量
     *
     * @param sentence
     * @param modelParamVO
     * @return
     * @throws JsonProcessingException
     */
    private String getRawJavaTensorflowParams(String sentence, ModelParamVO modelParamVO) {

        if (modelParamVO.getRemovePunctuation() != null && modelParamVO.getRemovePunctuation().booleanValue()) {
            // 替换标点符号
            sentence.replaceAll("\\W", "");
        }
        if (modelParamVO.getRemoveStopWord() != null && modelParamVO.getRemoveStopWord().booleanValue()) {
            // 去停词
            sentence = removeStopWord(sentence);
        }

        // 切字
        List<String> wordList = cutService.getListByMethod(sentence, modelParamVO.getCutMethod());

        // 加开始和结束符
        wordList.add(0, "[CLS]");
        wordList.add("[SEP]");

        // 将文字转化为坐标值
        Map<String, Integer> dictMap = vocabMapService.getVocabMap();
        List<Integer> integerList = wordList2IntegerList(wordList, dictMap);

        // 构造inputIds, inputMask, segmentIds
        List<Integer> inputIds = new ArrayList<>();
        List<Integer> inputMask = new ArrayList<>();
        List<Integer> segmentIds = new ArrayList<>();
        getInputIdsInputMaskSegmentIds(integerList, inputIds, inputMask, segmentIds, modelParamVO.getLength());

        // 返回JSON化的结果
        try {
            return getJsonResult(modelParamVO, inputIds, inputMask, segmentIds);
        } catch (JsonProcessingException e) {
            log.error("e = ", e);
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }
    }

    /**
     * 获取JSON化的结果
     *
     * @param modelParamVO
     * @param inputIds
     * @param inputMask
     * @param segmentIds
     * @return
     * @throws JsonProcessingException
     */
    private String getJsonResult(ModelParamVO modelParamVO, List<Integer> inputIds, List<Integer> inputMask, List<Integer> segmentIds) throws JsonProcessingException {
        Map<String, Object> result = new HashMap<>();
        result.put("input_ids", inputIds);
        result.put("input_mask", inputMask);
        if (modelParamVO.getHaveLabelIds() != null && modelParamVO.getHaveLabelIds().booleanValue()) {
            result.put("label_ids", 0);
        }
        result.put("segment_ids", segmentIds);
        return objectMapper.writeValueAsString(result);
    }

    /**
     * 去停词
     *
     * @param sentence
     * @return
     */
    private String removeStopWord(String sentence) {
        // 1. 切词
        List<String> wordList = cutService.getListByMethod(sentence, DictItem.CUT_METHOD_WORD_CUT);
        // 2. 读取停词
        List<String> stopWordList = stopWordService.getStopWord();
        // 3. 过滤停词，拼接字符串，返回结果
        return wordList.stream()
                .filter(wordCut -> !stopWordList.contains(wordCut))
                .collect(Collectors.joining());
    }

    /**
     * 获取tensorflow向量
     *
     * @param sentence
     * @param modelParamVO
     * @return
     */
    public String getRawTensorflowParams(String sentence, ModelParamVO modelParamVO) {
        return getRawJavaTensorflowParams(sentence, modelParamVO);

        // TODO 每增加一个模型，需要添加一段代码逻辑
//        if (paramCode == ModelParamEnum.TENSORFLOW_DIRTY_WORD.getCode() ||
//                paramCode == ModelParamEnum.TENSORFLOW_IS_TAX_ISSUE.getCode() ||
//                paramCode == ModelParamEnum.TENSORFLOW_SENTIMENT_ANALYSIS.getCode()) {
//            return getRawJavaTensorflowParams(sentence, paramCode, 128);
//        } else if (paramCode == ModelParamEnum.TENSORFLOW_AP_BILSTM.getCode()) {
//            // 问答模型处理很复杂，需要先通过预处理获取到参数，再用参数拿到中间结果，最后还要将中间结果经过后处理加工成最终结果
//            return getRawPythonTensorflowParams(sentence, paramCode, "", 120);
//        } else if (paramCode == ModelParamEnum.TENSORFLOW_BERT_MATCH.getCode()) {
//            return getRawJavaTensorflowParams(sentence, paramCode, 84);
//        } else if (paramCode == ModelParamEnum.TENSORFLOW_RERANKING.getCode()) {
//            // 问答模型处理很复杂
//            String result = getRawPythonTensorflowParams(sentence, paramCode, "", 120);
//            try {
//                JsonNode jsonNode = objectMapper.readTree(result);
//                ArrayNode answer_ = (ArrayNode) jsonNode.get("answer_");
//                ArrayNode probability = (ArrayNode) jsonNode.get("probability");
//                ArrayNode question_ = (ArrayNode) jsonNode.get("question_");
//
//                StringBuilder stringBuilder = new StringBuilder();
//
//                int size = answer_.size();
//                for (int i = 0; i < size; i++) {
//                    stringBuilder.append("{");
//                    stringBuilder.append("\"answer\":");
//                    stringBuilder.append(objectMapper.writeValueAsString(answer_.get(i)));
//                    stringBuilder.append(",\"question\":");
//                    stringBuilder.append(objectMapper.writeValueAsString(question_.get(i)));
//                    stringBuilder.append(",\"probability\":[");
//                    stringBuilder.append(probability.get(i).asText());
//                    stringBuilder.append("]},");
//                }
//                return stringBuilder.substring(0, stringBuilder.length() - 1);
//            } catch (IOException e) {
//                log.error("e = {}", e);
//            }
//        } else if (paramCode == ModelParamEnum.TENSORFLOW_SHEBAO.getCode()) {
//            // 社保是512长度
//            return getRawPythonTensorflowParams(sentence, paramCode, "", 512);
//        } else if (paramCode == ModelParamEnum.TENSORFLOW_FIRSTALL.getCode() ||
//                paramCode == ModelParamEnum.TENSORFLOW_SYNTHESIS.getCode() ||
//                paramCode == ModelParamEnum.TENSORFLOW_CITY_MANAGEMENT.getCode()) {
//            // 三分类和综合模型都是300长度
//            return getRawPythonTensorflowParams(sentence, paramCode, "", 300);
//        } else if (paramCode == ModelParamEnum.TENSORFLOW_ZNZX.getCode()) {
//            // 智能咨询模型长度是128
//            return getRawPythonTensorflowParams(sentence, paramCode, "", 128);
//        }
//
//        log.error("unknown paramCode = {}", paramCode);
//        return null;
    }

    /**
     * 获取喂给mleap的数据
     *
     * @param sentence
     * @param cutMethod
     * @return
     */
    public String getMLeapParams(String sentence, String cutMethod) {
        String res = getRawMLeapParams(sentence, cutMethod);
        return "{\"schema\":{\"fields\":[{\"shortName\":\"word\",\"type\":\"string\"}]},\"rows\":[[\"" + res + "\"]]}";
    }

    /**
     * 获取喂给tensorflow的数据
     *
     * @param sentence
     * @param modelParamVO
     * @return
     */
    public String getTensorflowParams(String sentence, ModelParamVO modelParamVO) {
        String res = getRawTensorflowParams(sentence, modelParamVO);
        return "{\"instances\": [" + res + "]}";
    }

    /**
     * 获取参数
     * <p>
     * 如果是mleap，首先访问mleap-params-transformer获取到分词，然后拼成
     * {"schema":{"fields":[{"shortName":"word","type":"string"}]},"rows":[["增值税 的 税率 是 多少"]]}
     * <p>
     * 如果是tensorflow的脏话模型，首先访问tensorflow-dirtyword-params-transformer获取到向量，然后拼成
     * {"instances": [{"input_ids":[101,4318,1673,7027,1402,679,1139,6496,4280,102,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],"input_mask":[1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],"label_ids":0,"segment_ids":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]}]}
     *
     * @param sentence
     * @param modelType
     * @param modelParamVO
     * @return
     * @throws Exception
     */
    private String getParams(String sentence, String modelType, ModelParamVO modelParamVO) {
        // 获取参数
        if (modelType.equalsIgnoreCase(DictItem.MODEL_TYPE_MLEAP)) {
            return getMLeapParams(sentence, modelParamVO.getCutMethod());
        } else if (modelType.equalsIgnoreCase(DictItem.MODEL_TYPE_TENSORFLOW)) {
            return getTensorflowParams(sentence, modelParamVO);
        } else {
            log.error("unknown modelType = {}", modelType);
            throw new AlgorithmException(ResultEnum.UNKNOWN_MODEL_TYPE);
        }
    }

//    /**
//     * 调用tensorflow/serving或mleap-serving预测结果
//     *
//     * @param params
//     * @param path
//     * @return
//     * @throws Exception
//     */
//    private String doPredict(String params, String path) throws Exception {
//
//        URI uri = new URIBuilder()
//                .setScheme("http")
//                .setHost("localhost")
//                .setPort(customConfig.getBridgePort())
//                .setPath(path)
//                .build();
//        HttpPost httpPost = new HttpPost(uri);
//        StringEntity stringEntity = new StringEntity(params, ContentType.APPLICATION_JSON);
//        httpPost.setEntity(stringEntity);
//        CloseableHttpResponse response = httpClient.execute(httpPost);
//        HttpEntity entity = response.getEntity();
//        return EntityUtils.toString(entity);
//    }

    /**
     * 调用mleap-serving获取结果
     *
     * @param port
     * @param params
     * @return
     * @throws Exception
     */
    private String doMLeapPredict(int port, String params) throws Exception {
        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(customDockerConfig.getRestIp())
                .setPort(port)
                .setPath("/transform")
                .build();
        HttpPost httpPost = new HttpPost(uri);
        StringEntity stringEntity = new StringEntity(params, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
    }

    /**
     * 调用tensorflow-serving获取结果
     *
     * @param port
     * @param fullName
     * @param params
     * @return
     * @throws Exception
     */
    private String doTensorflowPredict(int port, String fullName, String params) throws Exception {
        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(customDockerConfig.getRestIp())
                .setPort(port)
                .setPath("/v1/models/" + fullName + ":predict")
                .build();
        HttpPost httpPost = new HttpPost(uri);
        StringEntity stringEntity = new StringEntity(params, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
    }

    /**
     * 获取元数据
     * @param port
     * @param fullName
     * @return
     * @throws Exception
     */
    private String doGetMetadata(int port, String fullName) throws Exception {
        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(customDockerConfig.getRestIp())
                .setPort(port)
                .setPath("/v1/models/" + fullName + "/metadata")
                .build();
        HttpGet httpGet = new HttpGet(uri);
        CloseableHttpResponse response = httpClient.execute(httpGet);
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
        predictResultVO.setDockerResult(ps);
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
     * 获取返回的AP_BILSTM模型预测结果VO
     *
     * @param ps              喂给tensorflow模型后预测的结果
     * @param sentence        原始问题
     * @param params          原始问题的预处理结果
     * @param getParamsCostMs 预处理耗费的时间
     * @param predictCostMs   预测耗费的时间
     * @return
     */
    private Object getTensorflowApBilstmPredictResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {

        // 首先拿着ps去查询
        long beforePost = System.currentTimeMillis();
        // 这次调用主要是在tensorflow-param中生成一个文件
        // todo 这里不知道怎么改
//        getRawPythonTensorflowParams(sentence, ModelParamEnum.TENSORFLOW_AP_BILSTM.getCode(), ps, 120);
        long afterPost = System.currentTimeMillis();

        // 预测结果无返回

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
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        predictResultVO.setPostCostMs(afterPost - beforePost);
        predictResultVO.setDockerResult(ps);
        predictResultVO.setNextInput(sentence);

        return predictResultVO;
    }

    /**
     * 获取返回的AP_BILSTM模型预测结果VO
     *
     * @param ps              喂给tensorflow模型后预测的结果
     * @param sentence        原始问题
     * @param params          原始问题的预处理结果
     * @param getParamsCostMs 预处理耗费的时间
     * @param predictCostMs   预测耗费的时间
     * @return
     */
    private Object getTensorflowBertMatchPredictResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {

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

        String rawQuestion = null;
        String predictString = null;
        try {
            String text = objectMapper.writeValueAsString(arrayNode1);
            rawQuestion = RuntimeUtil.execForStr("python",
                    "python/bert_match/deployment.py",
                    "python/bert_match/stand_em_.pk",
                    text);
            if (!StringUtils.isEmpty(rawQuestion)) {
                rawQuestion = rawQuestion.trim();
            }
            predictString = RuntimeUtil.execForStr("python",
                    "python/bert_match/get_result.py",
                    "python/bert_match/train_set.csv",
                    rawQuestion);
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        log.debug("str = {}", rawQuestion);

        // 首先拿着ps去查询
        long beforePost = System.currentTimeMillis();
        // 先将预测结果转化为JsonNode
        long afterPost = System.currentTimeMillis();

        // 预测结果无返回

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
        predictResultVO.setPredictString(predictString);
        predictResultVO.setRawQuestion(rawQuestion);
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        predictResultVO.setPostCostMs(afterPost - beforePost);
        predictResultVO.setDockerResult(ps);
        predictResultVO.setNextInput(sentence);

        return predictResultVO;
    }

    /**
     * 获取返回的AP_BILSTM模型预测结果VO
     *
     * @param ps              喂给tensorflow模型后预测的结果
     * @param sentence        原始问题
     * @param params          原始问题的预处理结果
     * @param getParamsCostMs 预处理耗费的时间
     * @param predictCostMs   预测耗费的时间
     * @return
     */
    private Object getTensorflowRerankingPredictResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {

        // 首先拿着ps去查询
        long beforePost = System.currentTimeMillis();
        // 这次调用主要是在tensorflow-param中生成一个文件
        // todo 这里不知道怎么改
//        String result = getRawPythonTensorflowParams(sentence, ModelParamEnum.TENSORFLOW_RERANKING.getCode(), ps, 120);
        long afterPost = System.currentTimeMillis();

        // 预测结果类似
        // [
        //    [
        //        0.9984029023653523,
        //        "8ef5d5397fc24f618661132f61444291",
        //        "2.2.4990",
        //        "小规模纳税人逾期补申报流程？",
        //        "您好！一般纳税人增值税逾期未申报的需要到主管税局进行补申报，小规模纳税人增值税逾期未申报的可以在电子税务局逾期申报模块补申报或者去主管税局进行补申报；并应按照规定办理相关处罚事宜。"
        //    ]
        // ]
        ArrayNode arrayNode = null;
//        try {
        // todo 这里不知道怎么改
//            ArrayNode a1 = (ArrayNode) objectMapper.readTree(result);
//            if (a1 != null && a1.size() == 1) {
//                arrayNode = (ArrayNode) a1.get(0);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

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
        predictResultVO.setProbability(arrayNode.get(0).asDouble());
        predictResultVO.setPredictId(arrayNode.get(1).asText());
        predictResultVO.setPredictNodeCode(arrayNode.get(2).asText());
        predictResultVO.setRawQuestion(arrayNode.get(3).asText());
        predictResultVO.setPredictString(arrayNode.get(4).asText());
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        predictResultVO.setPostCostMs(afterPost - beforePost);
        predictResultVO.setDockerResult(ps);

        return predictResultVO;
    }

    /**
     * 获取返回的tensorflow默认预测结果VO
     *
     * @param ps              喂给tensorflow模型后预测的结果
     * @param sentence        原始问题
     * @param params          原始问题的预处理结果
     * @param getParamsCostMs 预处理耗费的时间
     * @param predictCostMs   预测耗费的时间
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

        JsonNode paramsNode = null;
        try {
            paramsNode = objectMapper.readTree(params);
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        PredictResultVO predictResultVO = new PredictResultVO();
        predictResultVO.setSentence(sentence);
        predictResultVO.setParams(paramsNode);
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        predictResultVO.setDockerResult(ps);
        return predictResultVO;
    }

    /**
     * 获取返回的tensorflow社保预测结果VO
     *
     * @param ps              喂给tensorflow模型后预测的结果
     * @param sentence        原始问题
     * @param params          原始问题的预处理结果
     * @param getParamsCostMs 预处理耗费的时间
     * @param predictCostMs   预测耗费的时间
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
     * 获取返回的三分类城管社保综合预测结果VO
     *
     * @param ps              喂给tensorflow模型后预测的结果
     * @param sentence        原始问题
     * @param params          原始问题的预处理结果
     * @param getParamsCostMs 预处理耗费的时间
     * @param predictCostMs   预测耗费的时间
     * @return
     */
    private Object getTensorflowFirstAllResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {

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
                predictResultVO.setPredictString("城管");
                break;
            case 1:
                predictResultVO.setPredictString("社保");
                break;
            case 2:
                predictResultVO.setPredictString("综合");
                break;
        }
        predictResultVO.setPredict(value);
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        return predictResultVO;
    }

    /**
     * 获取返回的综合模型预测结果VO
     *
     * @param ps              喂给tensorflow模型后预测的结果
     * @param sentence        原始问题
     * @param params          原始问题的预处理结果
     * @param getParamsCostMs 预处理耗费的时间
     * @param predictCostMs   预测耗费的时间
     * @return
     */
    private Object getTensorflowSynthesisResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {

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
                predictResultVO.setPredictString("工商管理类");
                break;
            case 1:
                predictResultVO.setPredictString("城建土地规划类");
                break;
            case 2:
                predictResultVO.setPredictString("当前热点类");
                break;
            case 3:
                predictResultVO.setPredictString("劳动人事类");
                break;
            case 4:
                predictResultVO.setPredictString("城市管理类");
                break;
            case 5:
                predictResultVO.setPredictString("物价财税类");
                break;
            case 6:
                predictResultVO.setPredictString("旅游园林类");
                break;
            case 7:
                predictResultVO.setPredictString("环境保护类");
                break;
            case 8:
                predictResultVO.setPredictString("房产管理类");
                break;
            case 9:
                predictResultVO.setPredictString("交通秩序类");
                break;
            case 10:
                predictResultVO.setPredictString("农林水利类");
                break;
        }
        predictResultVO.setPredict(value);
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        return predictResultVO;
    }

    /**
     * 获取返回的城管模型预测结果VO
     *
     * @param ps              喂给tensorflow模型后预测的结果
     * @param sentence        原始问题
     * @param params          原始问题的预处理结果
     * @param getParamsCostMs 预处理耗费的时间
     * @param predictCostMs   预测耗费的时间
     * @return
     */
    private Object getTensorflowCityManagementResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {

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
                predictResultVO.setPredictString("施工");
                break;
            case 1:
                predictResultVO.setPredictString("城市规划");
                break;
            case 2:
                predictResultVO.setPredictString("垃圾");
                break;
            case 3:
                predictResultVO.setPredictString("绿化");
                break;
            case 4:
                predictResultVO.setPredictString("犬类安全");
                break;
            case 5:
                predictResultVO.setPredictString("停车收费");
                break;
        }
        predictResultVO.setPredict(value);
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        return predictResultVO;
    }

    /**
     * 获取返回的场景模型预测结果VO
     *
     * @param ps              喂给tensorflow模型后预测的结果
     * @param sentence        原始问题
     * @param params          原始问题的预处理结果
     * @param getParamsCostMs 预处理耗费的时间
     * @param predictCostMs   预测耗费的时间
     * @return
     */
    private Object getTensorflowZnzxParams(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {

        // 先将预测结果转化为JsonNode
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(ps);
        } catch (IOException e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.JSON_ERROR.getCode(), ps);
        }

        if (jsonNode.get("error") != null) {
            throw new AlgorithmException(ResultEnum.PREDICT_ERROR.getCode(), jsonNode.get("error").asText());
        }

        JsonNode predictions = jsonNode.get("predictions");
        ArrayNode arrayNode = (ArrayNode) predictions;
        arrayNode = (ArrayNode) arrayNode.get(0);
        double p0 = arrayNode.get(0) == null ? 0 : arrayNode.get(0).doubleValue();
        double p1 = arrayNode.get(1) == null ? 0 : arrayNode.get(1).doubleValue();
        double p2 = arrayNode.get(2) == null ? 0 : arrayNode.get(2).doubleValue();
        double p3 = arrayNode.get(3) == null ? 0 : arrayNode.get(3).doubleValue();
        double p4 = arrayNode.get(4) == null ? 0 : arrayNode.get(4).doubleValue();
        double p5 = arrayNode.get(5) == null ? 0 : arrayNode.get(5).doubleValue();
        double p6 = arrayNode.get(6) == null ? 0 : arrayNode.get(6).doubleValue();
        double p7 = arrayNode.get(7) == null ? 0 : arrayNode.get(7).doubleValue();
        int value = 0;
        double p = p0;
        if (p1 > p) {
            value = 1;
            p = p1;
        }
        if (p2 > p) {
            value = 2;
            p = p2;
        }
        if (p3 > p) {
            value = 3;
            p = p3;
        }
        if (p4 > p) {
            value = 4;
            p = p4;
        }
        if (p5 > p) {
            value = 5;
            p = p5;
        }
        if (p6 > p) {
            value = 6;
            p = p6;
        }
        if (p7 > p) {
            value = 7;
            p = p7;
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
        switch (value) {
            case 0:
                predictResultVO.setPredictString("发票查询");
                break;
            case 1:
                predictResultVO.setPredictString("纳税证明");
                break;
            case 2:
                predictResultVO.setPredictString("人工客服");
                break;
            case 3:
                predictResultVO.setPredictString("询问地址");
                break;
            case 4:
                predictResultVO.setPredictString("其他");
                break;
            case 5:
                predictResultVO.setPredictString("社保类别");
                break;
            case 6:
                predictResultVO.setPredictString("文件查询");
                break;
            case 7:
                predictResultVO.setPredictString("征期查询");
                break;
        }
        predictResultVO.setPredict(value);
        predictResultVO.setProbability(p);
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
        IndexesVO indexes = JsonUtils.getIndexes(objectMapper, jsonNode);
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

    /**
     * 通过查询vocat.txt文件，把单词列表转化为数字列表
     *
     * @param wordList
     * @param dictMap
     * @return
     */
    private List<Integer> wordList2IntegerList(List<String> wordList, Map<String, Integer> dictMap) {
        List<Integer> integerList = wordList.stream().map(word -> {
            Integer integer = dictMap.get(word);
            if (integer == null) {
                return dictMap.get("[UNK]");
            } else {
                return integer;
            }
        }).collect(Collectors.toList());
        return integerList;
    }

    /**
     * 构造inputIds, inputMask, segmentIds
     *
     * @param integerList
     * @param inputIds
     * @param inputMask
     * @param segmentIds
     * @param length
     */
    private void getInputIdsInputMaskSegmentIds(List<Integer> integerList, List<Integer> inputIds, List<Integer> inputMask, List<Integer> segmentIds, Integer length) {
        if (integerList.size() > length) {
            // 如果integerList长度太长，则它第128个元素置为结束符
            integerList.set(length - 1, integerList.get(integerList.size() - 1));
        } else if (integerList.size() < length) {
            // 如果integerList长度不够，则需要补零
            for (int i = integerList.size(); i < length; i++) {
                integerList.add(0);
            }
        }
        for (int i = 0; i < length; i++) {
            int t = integerList.get(i);
            inputIds.add(t);
            if (t > 0) {
                inputMask.add(1);
            } else {
                inputMask.add(0);
            }
            segmentIds.add(0);
        }
    }

    /**
     * 将body体里面的文本进行解析，解析成PredictVO对象
     *
     * @param sentence
     * @return
     */
    private PredictVO unpackSentence(String shortName, String sentence) {
        try {
            if (sentence.trim().startsWith("{")) {
                // 说明sentence就是一个JSON对象，直接解析
                PredictVO predictVO = objectMapper.readValue(sentence, PredictVO.class);
                predictVO.setShortName(shortName);
                return predictVO;
            } else {
                // 说明sentence是单独一句话，将其包装一下
                PredictVO predictVO = new PredictVO();
                predictVO.setShortName(shortName);
                predictVO.setSentence(sentence);

                // 从所有相同名称的模型中，找出版本号最大的tensorflow模型，如果没有tensorflow模型那就找版本号最大的mleap模型
                List<AiModelBO> aiModelEntityList = aiModelRepository.findByShortName(shortName);
                AiModelBO wanted = null;
                for (AiModelBO aiModelEntity : aiModelEntityList) {
                    if (wanted == null) {
                        wanted = aiModelEntity;
                    } else if (wanted.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_MLEAP) &&
                            aiModelEntity.getType().equalsIgnoreCase(DictItem.MODEL_TYPE_TENSORFLOW)) {
                        wanted = aiModelEntity;
                    } else if (aiModelEntity.getVersion() > wanted.getVersion()) {
                        wanted = aiModelEntity;
                    }
                }
                predictVO.setType(wanted.getType());
                predictVO.setVersion(wanted.getVersion());
                return predictVO;
            }
        } catch (IOException e) {
            log.error("e = ", e);
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }
    }
}
