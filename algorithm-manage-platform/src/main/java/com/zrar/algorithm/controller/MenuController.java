package com.zrar.algorithm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 定义各种路由
 * @author Jingfeng Zhou
 */
@Controller
@Slf4j
public class MenuController {

    /**
     * 首页
     * @param model
     * @return
     */
    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }
}
