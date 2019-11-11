package com.zrar.ai.service;

import java.util.List;

/**
 * 各种切字、词、短语的服务
 * @author Jingfeng Zhou
 */
public interface CutService {

    /**
     * 将sentence，按照cutMethod进行切割
     * @param sentence
     * @param cutMethod
     * @return
     */
    List<String> getListByMethod(String sentence, String cutMethod);

    /**
     * 将sentence，按照CutMethod进行切割，割完组装
     * @param sentence
     * @param cutMethod
     * @return
     */
    String getStringByMethod(String sentence, String cutMethod);

    /**
     * 将sentence，按照CutMethod进行切割，割完用separator做分隔符组装，如果切不出任何东西返回defaultString
     * @param sentence
     * @param cutMethod
     * @param separator
     * @param defaultString
     * @return
     */
    String getStringByMethod(String sentence, String cutMethod, String separator, String defaultString);
}
