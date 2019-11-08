package com.zrar.ai.service;

import com.zrar.ai.constant.CutMethodEnum;

import java.util.List;

/**
 * 各种切字、词、短语的服务
 * @author Jingfeng Zhou
 */
public interface CutService {

    /**
     * 将sentence，按照CutMethod进行切割
     * @param sentence
     * @param cutMethodEnum
     * @return
     */
    List<String> getListByMethod(String sentence, CutMethodEnum cutMethodEnum);

    /**
     * 将sentence，按照CutMethod进行切割，割完组装
     * @param sentence
     * @param cutMethodEnum
     * @return
     */
    String getStringByMethod(String sentence, CutMethodEnum cutMethodEnum);

    /**
     * 将sentence，按照CutMethod进行切割，割完用separator做分隔符组装，如果切不出任何东西返回defaultString
     * @param sentence
     * @param cutMethodEnum
     * @param separator
     * @param defaultString
     * @return
     */
    String getStringByMethod(String sentence, CutMethodEnum cutMethodEnum, String separator, String defaultString);
}
