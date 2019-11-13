package com.zrar.ai.service;

import com.zrar.ai.bo.LogBO;
import com.zrar.ai.vo.LogVO;
import org.springframework.data.domain.Sort;

import java.awt.print.Pageable;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

public interface LogService extends BaseService<LogVO> {

    void addLog(String method, String ip, String parameters, String description, Long cost, Date date);

}
