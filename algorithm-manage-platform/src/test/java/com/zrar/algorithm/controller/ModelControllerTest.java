package com.zrar.algorithm.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * @author Jingfeng Zhou
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
@ActiveProfiles("unittest")
public class ModelControllerTest {

    @Autowired
    private ModelController modelController;

    @Test
    public void predict() {
        modelController.predict("arask-tax-entity", "[\"增值税的税率是多少\",\"房产税的税率是多少\"]");
    }
}