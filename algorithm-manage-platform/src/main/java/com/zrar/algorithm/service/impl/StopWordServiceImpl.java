package com.zrar.algorithm.service.impl;

import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.service.StopWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jingfeng Zhou
 */
@Service
@Slf4j
public class StopWordServiceImpl implements StopWordService {

    @Autowired
    private StopWordService stopWordService;

    @Cacheable(value = "StopWordServiceImpl")
    @Override
    public List<String> getStopWord() {
        return stopWordService.getStopWordByPath(DEFAULT_STOP_WORD_FILE_PATH);
    }

    @Cacheable(value = "StopWordServiceImpl", key = "#path")
    @Override
    public List<String> getStopWordByPath(String path) {
        List<String> stopWordList = new ArrayList<>();
        Resource stopWordResource = new ClassPathResource(path);
        try (InputStream inputStream = stopWordResource.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stopWordList.add(line);
            }
        } catch (Exception e) {
            log.error("e = {}", e);
            throw new AlgorithmException(ResultEnum.LOAD_STOP_WORD_ERROR);
        }
        return stopWordList;
    }
}
