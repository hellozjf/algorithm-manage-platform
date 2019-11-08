package com.zrar.ai.service;

import java.util.Map;

/**
 * @author Jingfeng Zhou
 */
public interface VocabMapService {

    /**
     * 默认读取的vocab.txt路径
     */
    String DEFAULT_VOCAB_FILE_PATH = "static/tensorflow/vocab.txt";

    /**
     * 读取默认的vocab.txt
     * @return
     */
    Map<String, Integer> getVocabMap();

    /**
     * 读取自定义的vocab.txt
     * @param path
     * @return
     */
    Map<String, Integer> getVocabMapByPath(String path);
}
