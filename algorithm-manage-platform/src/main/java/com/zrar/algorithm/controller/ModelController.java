package com.zrar.algorithm.controller;

import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.extra.tokenizer.Result;
import cn.hutool.extra.tokenizer.TokenizerEngine;
import cn.hutool.extra.tokenizer.TokenizerUtil;
import cn.hutool.extra.tokenizer.Word;
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
import com.zrar.algorithm.util.JiebaUtils;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
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
    private RestTemplate restTemplate;

    @Value("${server.port}")
    private String port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

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

            // TODO 每增加一个模型，需要增加一个分支
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
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_AP_BILSTM.getCode()) {
                // 是否是ap_bilstm模型的结果
                return ResultUtils.success(getTensorflowApBilstmPredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_RERANKING.getCode()) {
                // 是否是reranking模型的结果
                return ResultUtils.success(getTensorflowRerankingPredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_SHEBAO.getCode()) {
                // 社保模型的结果
                return ResultUtils.success(getTensorflowSocialSecurityResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_FIRSTALL.getCode()) {
                // 三分类模型的结果
                return ResultUtils.success(getTensorflowFirstAllResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_SYNTHESIS.getCode()) {
                // 综合模型
                return ResultUtils.success(getTensorflowSynthesisResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_CITY_MANAGEMENT.getCode()) {
                // 城管模型
                return ResultUtils.success(getTensorflowCityManagementResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_ZNZX.getCode()) {
                // 智能咨询-场景分类模型
                // 建议使用20191011
                return ResultUtils.success(getTensorflowZnzxParams(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_BERT_MATCH.getCode()) {
                // 是否是bert_match模型的结果
                // 智能咨询前置模型20191024
                return ResultUtils.success(getRemoteBertMatchPredictResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
//                return ResultUtils.success(getTensorflowBertMatchPredictResultVO(ps, sentence, params,
//                        afterGetParams - beforeGetParams,
//                        afterDoPredict - beforeDoPredict));
            } else if (modelEntity.getParam() == ModelParamEnum.TENSORFLOW_TAX_ENTITY.getCode()) {
                return ResultUtils.success(getTensorflowTaxEntityResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            } else {
                return ResultUtils.success(getTensorflowResultVO(ps, sentence, params,
                        afterGetParams - beforeGetParams,
                        afterDoPredict - beforeDoPredict));
            }
        } else if (modelEntity.getType() == ModelTypeEnum.COMPOSE.getCode()) {
            String[] modelsArray = modelEntity.getCompose().split(",");
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

    private Object getTensorflowTaxEntityResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {
        // 先将预测结果转化为JsonNode
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(ps);
        } catch (IOException e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }

        // 提取原始文本
        JsonNode sentenceNode = null;
        try {
            sentenceNode = objectMapper.readTree(sentence);
        } catch (IOException e) {
            log.error("e =", e);
        }
        ArrayNode sentenceArrayNode = (ArrayNode) sentenceNode;

        // 提取预测结果
        JsonNode predictions = jsonNode.get("predictions");
        ArrayNode arr = (ArrayNode) predictions;
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            ArrayNode arrayNode = (ArrayNode) arr.get(i);

            List<String> list = new ArrayList<>();
            for (JsonNode node : arrayNode) {
                list.add(String.valueOf(node.intValue()));
            }
            String vec = String.join(",", list);

            log.debug("vec = {}", vec);

            // r可能是['增值税 税率']
            String r = RuntimeUtil.execForStr("python",
                    "python/arask-tax-entity/export_result.py",
                    vec,
                    sentenceArrayNode.get(i).asText());
            // 变成 增值税 税率
            r = r.replaceAll("'", "")
                    .replaceAll("\\[", "")
                    .replaceAll("]", "")
                    .replaceAll("\r", "")
                    .replaceAll("\n", "");

            Map<String, Object> map = new HashMap<>();
            map.put("wb", sentenceArrayNode.get(i).asText());
            map.put("jg", r);
            mapList.add(map);
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
        try {
            predictResultVO.setPredictString(objectMapper.writeValueAsString(mapList));
        } catch (JsonProcessingException e) {
            log.error("e = ", e);
        }
        predictResultVO.setPredict(null);
        predictResultVO.setPreCostMs(getParamsCostMs);
        predictResultVO.setPredictCostMs(predictCostMs);
        return predictResultVO;
    }

    /**
     * 获取分词数据
     *
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

    private String getRawPythonTensorflowParams(String sentence, int paramCode, String other, int maxLength) {
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
            formparams.add(new BasicNameValuePair("maxLength", String.valueOf(maxLength)));
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
     * 通过Java获取社保模型Tensorflow前处理后的参数
     *
     * @param sentence
     * @param paramCode
     * @return
     */
    private String getRawJavaShebaoTensorflowParams(String sentence, int paramCode, int maxLength) {

        // 加载停用词
        List<String> stopWordList = stopWordService.getStopWordByPath("static/tensorflow/shebao/stopWord.txt");

        // 过滤停用词
        String filterStopWords = JiebaUtils.lcut(sentence).stream()
                .filter(wordCut -> !stopWordList.contains(wordCut))
                .collect(Collectors.joining());
        log.debug("sentence={}", sentence);
        log.debug("filterStopWords={}", filterStopWords);

        return getRawJavaTensorflowParams(filterStopWords, paramCode, maxLength);
    }

    /**
     * 通过Java获取问答模型Tensorflow前处理后的参数
     *
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
     * 将句子转成字，然后转换为向量
     *
     * @param sentence
     * @param paramCode
     * @param maxSeqLength
     * @return
     */
    private String getRawJavaTensorflowParams(String sentence, int paramCode, int maxSeqLength) {
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
                if (!StringUtils.isEmpty(number.toString())) {
                    // 将之前缓存的数字放入list中
                    wordList.add(number.toString());
                    // 清除缓存的数字
                    number.setLength(0);
                }
                wordList.add(word);
            }
        }
        if (!StringUtils.isEmpty(number.toString())) {
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
        if (paramCode != ModelParamEnum.TENSORFLOW_BERT_MATCH.getCode() &&
            paramCode != ModelParamEnum.TENSORFLOW_SENTIMENT_ANALYSIS.getCode() &&
            paramCode != ModelParamEnum.TENSORFLOW_TAX_ENTITY.getCode()) {
            result.put("label_ids", 0);
        }
        result.put("segment_ids", segmentIds);
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("e = {}", e);
            return "";
        }
    }

    /**
     * 将句子转成字，然后转换为向量，其中要考虑是否去除标点、去停词、长度
     *
     * @param sentence              原始句子
     * @param bRemovePunctuation    是否去除标点
     * @param bRemoveStopWord       是否去停词
     * @param maxSeqLength          长度或位数
     * @return
     */
    private String getRawJavaTensorflowParams(String sentence, boolean bRemovePunctuation, boolean bRemoveStopWord, int maxSeqLength) {

        if (bRemovePunctuation) {
            // 移除标点符号
            sentence.replaceAll("\\W", "");
        }
        if (bRemoveStopWord) {
            // 自动根据用户引入的分词库的jar来自动选择使用的引擎
            TokenizerEngine engine = TokenizerUtil.createEngine();
            // 解析文本
            Result result = engine.parse(sentence);
            Iterator<Word> iterator = result;
            List<String> wordList = new ArrayList<>();

            // 去停词
            try {
                File file = new ClassPathResource("static/tensorflow/stopWord.txt").getFile();
                FileReader fileReader = new FileReader(file);
                List<String> lines = fileReader.readLines();
                while (iterator.hasNext()) {
                    Word word = iterator.next();
                    String text = word.getText();
                    if (lines.contains(text)) {
                        continue;
                    } else {
                        wordList.add(text);
                    }
                }
            } catch (IOException e) {
                log.error("e = {}", e);
            }

            // 把去停词以后的字符串重组出来
            sentence = String.join("", wordList);
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
                if (!StringUtils.isEmpty(number.toString())) {
                    // 将之前缓存的数字放入list中
                    wordList.add(number.toString());
                    // 清除缓存的数字
                    number.setLength(0);
                }
                wordList.add(word);
            }
        }
        if (!StringUtils.isEmpty(number.toString())) {
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
     *
     * @param sentence
     * @param paramCode
     * @return
     */
    @GetMapping("/getRawTensorflowParams")
    public String getRawTensorflowParams(String sentence, int paramCode) {
        // TODO 每增加一个模型，需要添加一段代码逻辑
        if (paramCode == ModelParamEnum.TENSORFLOW_DIRTY_WORD.getCode() ||
                paramCode == ModelParamEnum.TENSORFLOW_IS_TAX_ISSUE.getCode()) {
            return getRawJavaTensorflowParams(sentence, paramCode, 128);
        } else if (paramCode == ModelParamEnum.TENSORFLOW_SENTIMENT_ANALYSIS.getCode()) {
            return getRawJavaTensorflowParams(sentence, paramCode, 50);
        } else if (paramCode == ModelParamEnum.TENSORFLOW_AP_BILSTM.getCode()) {
            // 问答模型处理很复杂，需要先通过预处理获取到参数，再用参数拿到中间结果，最后还要将中间结果经过后处理加工成最终结果
            return getRawPythonTensorflowParams(sentence, paramCode, "", 120);
        } else if (paramCode == ModelParamEnum.TENSORFLOW_BERT_MATCH.getCode()) {
            return getRawJavaTensorflowParams(sentence, paramCode, 84);
        } else if (paramCode == ModelParamEnum.TENSORFLOW_RERANKING.getCode()) {
            // 问答模型处理很复杂
            String result = getRawPythonTensorflowParams(sentence, paramCode, "", 120);
            try {
                JsonNode jsonNode = objectMapper.readTree(result);
                ArrayNode answer_ = (ArrayNode) jsonNode.get("answer_");
                ArrayNode probability = (ArrayNode) jsonNode.get("probability");
                ArrayNode question_ = (ArrayNode) jsonNode.get("question_");

                StringBuilder stringBuilder = new StringBuilder();

                int size = answer_.size();
                for (int i = 0; i < size; i++) {
                    stringBuilder.append("{");
                    stringBuilder.append("\"answer\":");
                    stringBuilder.append(objectMapper.writeValueAsString(answer_.get(i)));
                    stringBuilder.append(",\"question\":");
                    stringBuilder.append(objectMapper.writeValueAsString(question_.get(i)));
                    stringBuilder.append(",\"probability\":[");
                    stringBuilder.append(probability.get(i).asText());
                    stringBuilder.append("]},");
                }
                return stringBuilder.substring(0, stringBuilder.length() - 1);
            } catch (IOException e) {
                log.error("e = {}", e);
            }
        } else if (paramCode == ModelParamEnum.TENSORFLOW_SHEBAO.getCode()) {
            // 社保是512长度
            return getRawPythonTensorflowParams(sentence, paramCode, "", 512);
        } else if (paramCode == ModelParamEnum.TENSORFLOW_FIRSTALL.getCode() ||
                paramCode == ModelParamEnum.TENSORFLOW_SYNTHESIS.getCode() ||
                paramCode == ModelParamEnum.TENSORFLOW_CITY_MANAGEMENT.getCode()) {
            // 三分类和综合模型都是300长度
            return getRawPythonTensorflowParams(sentence, paramCode, "", 300);
        } else if (paramCode == ModelParamEnum.TENSORFLOW_ZNZX.getCode()) {
            // 智能咨询模型长度是128
            return getRawPythonTensorflowParams(sentence, paramCode, "", 128);
        } else if (paramCode == ModelParamEnum.TENSORFLOW_TAX_ENTITY.getCode()) {
            // 税务实体挖掘模型长度是90
            // 这里的sentence实际上是一个字符串数组，用objectMapper进行解析
            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(sentence);
            } catch (IOException e) {
                log.error("e = ", e);
            }
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            List<String> paramList = new ArrayList<>();
            for (int i = 0; i < arrayNode.size(); i++) {
                paramList.add(getRawJavaTensorflowParams(arrayNode.get(i).asText(), paramCode, 90));
            }
            return String.join(",", paramList);
        }

        log.error("unknown paramCode = {}", paramCode);
        return null;
    }

    /**
     * 获取喂给mleap的数据
     *
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
     *
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
        getRawPythonTensorflowParams(sentence, ModelParamEnum.TENSORFLOW_AP_BILSTM.getCode(), ps, 120);
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
    private Object getRemoteBertMatchPredictResultVO(String ps, String sentence, String params, Long getParamsCostMs, Long predictCostMs) {


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
        // 首先拿着ps去查询
        long beforePost = System.currentTimeMillis();
        try {
            String text = objectMapper.writeValueAsString(arrayNode1);
            rawQuestion = getRawPythonTensorflowParams(text, ModelParamEnum.TENSORFLOW_BERT_MATCH.getCode(), "rawQuestion", 120);
            if (! StringUtils.isEmpty(rawQuestion)) {
                rawQuestion = rawQuestion.trim();
            }
            predictString = getRawPythonTensorflowParams(rawQuestion, ModelParamEnum.TENSORFLOW_BERT_MATCH.getCode(), "predictString", 0);
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        log.debug("str = {}", rawQuestion);
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

        // 首先拿着ps去查询
        long beforePost = System.currentTimeMillis();
        try {
            String text = objectMapper.writeValueAsString(arrayNode1);
            rawQuestion = RuntimeUtil.execForStr("python",
                    "python/bert_match/deployment.py",
                    "python/bert_match/stand_em_.pk",
                    text);
            if (! StringUtils.isEmpty(rawQuestion)) {
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
        String result = getRawPythonTensorflowParams(sentence, ModelParamEnum.TENSORFLOW_RERANKING.getCode(), ps, 120);
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
        try {
            ArrayNode a1 = (ArrayNode) objectMapper.readTree(result);
            if (a1 != null && a1.size() == 1) {
                arrayNode = (ArrayNode) a1.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            throw new AlgorithmException(ResultEnum.JSON_ERROR);
        }

        JsonNode predictions = jsonNode.get("predictions");
        ArrayNode arrayNode = (ArrayNode) predictions;
        arrayNode = (ArrayNode) arrayNode.get(0);
        double p0 = arrayNode.get(0).doubleValue();
        double p1 = arrayNode.get(1).doubleValue();
        double p2 = arrayNode.get(2).doubleValue();
        double p3 = arrayNode.get(3).doubleValue();
        double p4 = arrayNode.get(4).doubleValue();
        double p5 = arrayNode.get(5).doubleValue();
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
