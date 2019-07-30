package com.zrar.algorithm.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jingfeng Zhou
 */
public class DictMapUtils {

    private static final Logger log = LoggerFactory.getLogger(DictMapUtils.class);
    private static Map<String, Integer> dictMap = null;

    /**
     * 获取tensorflow所需要的字典map
     * 首次加载需要读取文件并加载到内存中，之后再次获取则使用内存中的数据
     * @return
     */
    public static Map<String, Integer> getDictMap() {
        if (dictMap == null) {
            dictMap = new HashMap<>();
            ClassPathResource classPathResource = new ClassPathResource("vocab.txt");
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
        } else {
            return dictMap;
        }
    }

}
