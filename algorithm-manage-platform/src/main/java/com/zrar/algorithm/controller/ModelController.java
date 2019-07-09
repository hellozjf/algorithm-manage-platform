package com.zrar.algorithm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zrar.algorithm.config.CustomConfig;
import com.zrar.algorithm.constant.ModelTypeEnum;
import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.domain.ModelEntity;
import com.zrar.algorithm.repository.ModelRepository;
import com.zrar.algorithm.service.FileService;
import com.zrar.algorithm.service.MLeapService;
import com.zrar.algorithm.service.RemoteService;
import com.zrar.algorithm.util.ResultUtils;
import com.zrar.algorithm.vo.ResultVO;
import com.zrar.tools.mleapcontroller.config.CustomConfig;
import com.zrar.tools.mleapcontroller.constant.ResultEnum;
import com.zrar.tools.mleapcontroller.entity.ModelEntity;
import com.zrar.tools.mleapcontroller.repository.ModelRepository;
import com.zrar.tools.mleapcontroller.service.FileService;
import com.zrar.tools.mleapcontroller.service.MLeapService;
import com.zrar.tools.mleapcontroller.util.ResultUtils;
import com.zrar.tools.mleapcontroller.vo.ResultVO;
import com.zrar.tools.mleapcontroller.vo.TaxClassifyPredictVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
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

    /**
     * 预测某句话的分类，以及这个分类的可信度
     * predict实际上会发送
     * {"schema":{"fields":[{"name":"word","type":"string"}]},"rows":[["增值税 的 税率 是 多少"]]}
     *
     * @param modelName
     * @param line
     * @return
     */
    @PostMapping("/{modelName}/predict")
    public ResultVO predict(@PathVariable("modelName") String modelName,
                            @RequestBody String line) {
        ModelEntity modelEntity = modelRepository.findByName(modelName);
        if (modelEntity.getType() == ModelTypeEnum.MLEAP.getCode()) {
            // mleap的模型
        } else if (modelEntity.getType() == ModelTypeEnum.TENSORFLOW.getCode()) {
            // tensorflow的模型
        }
        TaxClassifyPredictVO taxClassifyPredictVO = mLeapService.predict(mleap, line, nature);
        return ResultUtils.success(taxClassifyPredictVO);
    }

    /**
     * 预测多句话的分类，以及这个分类的可信度
     *
     * @param mleap
     * @param lines
     * @return
     */
    @PostMapping("/{mleap}/predict2")
    public ResultVO predict2(@PathVariable("mleap") String mleap,
                             @RequestBody String lines,
                             @RequestParam(defaultValue = "") String nature) {
        log.debug("lines = {}", lines);
        String[] lineArray = lines.split("(\r\n)|(\n)");
        for (String line : lineArray) {
            log.debug("line = {}", line);
        }
        List<String> lineList = Arrays.asList(lineArray);
        List<TaxClassifyPredictVO> taxClassifyPredictVOList = mLeapService.predict(mleap, lineList, nature);
        return ResultUtils.success(taxClassifyPredictVOList);
    }

}
