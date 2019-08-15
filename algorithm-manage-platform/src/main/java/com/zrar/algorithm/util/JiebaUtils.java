package com.zrar.algorithm.util;

import com.huaban.analysis.jieba.JiebaSegmenter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jingfeng Zhou
 */
public class JiebaUtils {

    /**
     * 通过结巴进行切词
     * @param str
     * @return
     */
    public static List<String> lcut(String str) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<String> wordList = segmenter.process(str, JiebaSegmenter.SegMode.INDEX).stream()
                .map(segToken -> segToken.word)
                .collect(Collectors.toList());
        return wordList;
    }
}
