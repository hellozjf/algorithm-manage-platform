package com.zrar.algorithm.service.impl;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.zrar.algorithm.constant.CutMethodEnum;
import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.service.CutService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 各种切字、词、短语的服务实现
 * @author Jingfeng Zhou
 */
@Service
public class CutServiceImpl implements CutService {

    @Override
    public List<String> getListByMethod(String sentence, CutMethodEnum cutMethodEnum) {
        if (cutMethodEnum.getCode().equals(CutMethodEnum.WORD_CUT)) {
            return getWordCutList(sentence, null);
        } else if (cutMethodEnum.getCode().equals(CutMethodEnum.WORD_CUT_VSWZYC)) {
            return getWordCutList(sentence, SWZYC);
        } else if (cutMethodEnum.getCode().equals(CutMethodEnum.PHRASE_LIST)) {
            return getPhraseList(sentence);
        } else if (cutMethodEnum.getCode().equals(CutMethodEnum.CHAR_CUT)) {
            return getCharCutList(sentence);
        } else {
            // 不支持的切词方法，抛出异常
            throw new AlgorithmException(ResultEnum.UNKNOWN_CUT_METHOD_ERROR);
        }
    }

    @Override
    public String getStringByMethod(String sentence, CutMethodEnum cutMethodEnum) {
        return getStringByMethod(sentence, cutMethodEnum, " ", "null");
    }

    @Override
    public String getStringByMethod(String sentence, CutMethodEnum cutMethodEnum, String separator, String defaultString) {
        List<String> cutList = getListByMethod(sentence, cutMethodEnum);
        if (cutList == null || cutList.size() == 0) {
            return defaultString;
        } else {
            return String.join(separator, cutList);
        }
    }

    /**
     * 词性：税务专有词
     */
    private static final String SWZYC = "vswzyc";

    /**
     * 切字，最后的列表中包括单个汉字或数字
     * 例如，你好123我是456，切出来的结果['你','好','123','我','是','456']
     *
     * @param sentence
     * @return
     */
    private List<String> getCharCutList(String sentence) {
        if (StringUtils.isEmpty(sentence)) {
            return new ArrayList<>();
        }
        String[] words = sentence.split("");
        List<String> charList = new ArrayList<>();
        StringBuilder number = new StringBuilder();
        for (String word : words) {
            if (org.apache.commons.lang3.StringUtils.isNumeric(word)) {
                // 数字先放到缓冲区缓存起来
                number.append(word);
            } else {
                if (!StringUtils.isEmpty(number.toString())) {
                    // 将之前缓存的数字放入list中
                    charList.add(number.toString());
                    // 清除缓存的数字
                    number.setLength(0);
                }
                charList.add(word);
            }
        }
        // 把最后一个数字加入wordList
        if (!StringUtils.isEmpty(number.toString())) {
            // 将之前缓存的数字放入list中
            charList.add(number.toString());
            // 清除缓存的数字
            number.setLength(0);
        }
        return charList;
    }

    /**
     * 切词方法，仅保留税务专有词，切出来的词最后以空格分隔
     *
     * @param lineObject 需要分词的句子
     * @param saveNature 词性，如果为null表示不筛选词性
     */
    private String wordCut(Object lineObject, Object saveNature) {
        if (lineObject == null) {
            return "null";
        }
        String line = lineObject.toString().toUpperCase();
        List<Term> termList = HanLP.segment(line);
        StringBuffer res = new StringBuffer();
        for (Term i : termList) {
            // 这里可以打印i看看有哪些词性
            String word = i.word;
            String nature = i.nature.toString();
            // 如果saveNature==null或空字符串，表示不筛选词性，直接加进来
            // 如果saveNature!=null，表示需要筛选词性，要判断这是不是我需要的词性，是才加进来
            if (StringUtils.isEmpty(saveNature) || nature.contains(saveNature.toString())) {
                //词性为vswzyc（即税务专有词）的分词结果保留
                res.append(word).append(" ");
            }
        }
        if (res.length() > 0) {
            return res.substring(0, res.length() - 1);
        } else {
            return "null";
        }
    }

    /**
     * 切词方法，仅保留税务专有词
     *
     * @param sentence     需要分词的句子
     * @param wantedNature 词性，如果为null表示不筛选词性
     */
    private List<String> getWordCutList(String sentence, String wantedNature) {
        List<String> wordCutList = new ArrayList<>();
        if (StringUtils.isEmpty(sentence)) {
            return wordCutList;
        }
        sentence = sentence.toUpperCase();
        List<Term> termList = HanLP.segment(sentence);
        for (Term i : termList) {
            // 这里可以打印i看看有哪些词性
            String word = i.word;
            String nature = i.nature.toString();
            // 如果saveNature==null或空字符串，表示不筛选词性，直接加进来
            // 如果saveNature!=null，表示需要筛选词性，要判断这是不是我需要的词性，是才加进来
            if (StringUtils.isEmpty(wantedNature) || nature.contains(wantedNature)) {
                //词性为vswzyc（即税务专有词）的分词结果保留
                wordCutList.add(word);
            }
        }
        return wordCutList;
    }

    /**
     * 切短语
     * HanLP提取关键短语
     *
     * @author bigdata-陈晓曦
     */
    private String phraseList(Object lineObject) {
        if (lineObject == null) {
            return "null";
        }
        String line = lineObject.toString();
        List<String> phraseList = HanLP.extractPhrase(line, 10);
        StringBuffer res = new StringBuffer("");
        for (String i : phraseList) {
            res.append(i).append(" ");
        }
        if (res.length() > 0) {
            return res.substring(0, res.length() - 1);
        } else {
            return "null";
        }
    }

    /**
     * 切短语
     * HanLP提取关键短语
     *
     * @author bigdata-陈晓曦
     */
    private List<String> getPhraseList(String sentence) {
        if (StringUtils.isEmpty(sentence)) {
            return new ArrayList<>();
        }
        List<String> phraseList = HanLP.extractPhrase(sentence, 10);
        return phraseList;
    }
}
