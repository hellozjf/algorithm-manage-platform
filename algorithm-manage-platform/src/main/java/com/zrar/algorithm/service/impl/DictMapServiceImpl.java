package com.zrar.algorithm.service.impl;

import com.zrar.algorithm.service.DictMapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jingfeng Zhou
 */
@Service
@Slf4j
public class DictMapServiceImpl implements DictMapService {

    @Cacheable(value = "DictMapServiceImpl", key = "#path")
    @Override
    public Map<String, Integer> getDictMapByPath(String path) {
        Map<String, Integer> dictMap = new HashMap<>();
        ClassPathResource classPathResource = new ClassPathResource(path);
        try (InputStream inputStream = classPathResource.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line = null;
            for (int lineNum = 0; (line = bufferedReader.readLine()) != null; lineNum++) {
                dictMap.put(line, lineNum);
            }
        } catch (Exception e) {
            log.error("e = {}", e);
        }
        return dictMap;
    }
}
