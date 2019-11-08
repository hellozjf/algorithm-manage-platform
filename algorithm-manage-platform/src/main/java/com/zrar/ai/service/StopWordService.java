package com.zrar.ai.service;

import java.util.List;

/**
 * @author Jingfeng Zhou
 */
public interface StopWordService {

    /**
     * 默认的去停词文件路径
     */
    String DEFAULT_STOP_WORD_FILE_PATH = "static/tensorflow/stopWord.txt";

    /**
     * 获取默认的去停词列表
     * @return
     */
    List<String> getStopWord();

    /**
     * 获取自定义的去停词列表
     * @param path
     * @return
     */
    List<String> getStopWordByPath(String path);
}
