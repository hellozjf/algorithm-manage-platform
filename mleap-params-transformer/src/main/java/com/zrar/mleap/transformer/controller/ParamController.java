package com.zrar.mleap.transformer.controller;

import com.zrar.mleap.transformer.constant.ModelParamEnum;
import com.zrar.mleap.transformer.util.WordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jingfeng Zhou
 */
@RestController
@Slf4j
public class ParamController {

    @PostMapping("/getParams")
    public String getParams(String sentence, int paramCode) {
        String wordCut = null;
        if (paramCode == ModelParamEnum.MLEAP_CUT_WORD.getCode()) {
            wordCut = WordUtils.wordCut(sentence, "");
        } else if (paramCode == ModelParamEnum.MLEAP_CUT_WORD_VSWZYC.getCode()) {
            wordCut = WordUtils.wordCut(sentence, ModelParamEnum.MLEAP_CUT_WORD_VSWZYC.getNature());
        } else if (paramCode == ModelParamEnum.MLEAP_PHRASE_LIST.getCode()) {
            wordCut = WordUtils.phraseList(sentence);
        }
        log.debug("sentence = {}, paramCode = {}, wordCut = {}", sentence, paramCode, wordCut);
        return wordCut;
    }
}
