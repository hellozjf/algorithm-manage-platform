package com.zrar.algorithm.service.impl;

import cn.hutool.core.io.FileUtil;
import com.zrar.algorithm.service.VocabMapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jingfeng Zhou
 */
@Service
@Slf4j
public class VocabMapServiceImpl implements VocabMapService {

    @Autowired
    private VocabMapService vocabMapService;

    @Cacheable(value = "VocabMapService")
    @Override
    public Map<String, Integer> getVocabMap() {
        return vocabMapService.getVocabMapByPath(DEFAULT_VOCAB_FILE_PATH);
    }

    @Cacheable(value = "VocabMapService", key = "#path")
    @Override
    public Map<String, Integer> getVocabMapByPath(String path) {
        Map<String, Integer> vocabMap = new HashMap<>();
        ClassPathResource classPathResource = new ClassPathResource(path);
        try (InputStream inputStream = classPathResource.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line = null;
            for (int lineNum = 0; (line = bufferedReader.readLine()) != null; lineNum++) {
                vocabMap.put(line, lineNum);
            }
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        return vocabMap;
    }
}
