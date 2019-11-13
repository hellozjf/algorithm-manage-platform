package com.zrar.ai.service.impl;

import com.zrar.ai.bo.LogBO;
import com.zrar.ai.dao.LogDao;
import com.zrar.ai.mapper.LogMapper;
import com.zrar.ai.service.LogService;
import com.zrar.ai.vo.LogVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.awt.print.Pageable;
import java.util.*;

@Service
public class LogServiceImpl extends BaseServiceImpl<LogVO, LogBO, LogDao, LogMapper>
        implements LogService {

    @Override
    public void addLog(String method, String ip, String parameters, String description, Long cost, Date date) {
        LogBO logBO = new LogBO();
        logBO.setCost(cost);
        logBO.setDate(date);
        logBO.setDescription(description);
        logBO.setIp(ip);
        logBO.setMethod(method);
        logBO.setParam(parameters);
        dao.save(logBO);
    }

}
