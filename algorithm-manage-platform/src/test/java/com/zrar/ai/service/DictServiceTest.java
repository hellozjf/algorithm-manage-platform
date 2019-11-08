package com.zrar.ai.service;

import com.zrar.ai.BaseTests;
import com.zrar.ai.vo.DictVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.Assert.*;

/**
 * @author Jingfeng Zhou
 */
@Slf4j
public class DictServiceTest extends BaseTests {

    @Autowired
    private DictService dictService;

    @Test
    public void findByNameLike() {
    }
}