package com.zrar.ai.service;

import com.zrar.ai.BaseTests;
import com.zrar.ai.bo.AiModelBO;
import com.zrar.ai.vo.AiModelVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * @author Jingfeng Zhou
 */
@Slf4j
public class AiModelServiceTest extends BaseTests {

    @Autowired
    private AiModelService aiModelService;

    @Test
    public void showAll() {
        Page<AiModelVO> aiModelVOPage = aiModelService.getPage(null, PageRequest.of(0, 10));
        aiModelVOPage.stream().forEach(aiModelVO -> log.debug("{}", aiModelVO));
    }
}