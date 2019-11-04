package com.zrar.algorithm.service.impl;

import com.zrar.algorithm.config.CustomDockerConfig;
import com.zrar.algorithm.service.RemoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Jingfeng Zhou
 */
@Service
public class RemoteServiceImpl implements RemoteService {

    @Autowired
    private CustomDockerConfig customDockerConfig;

    @Override
    public String createExecCommand(String cmd) {
        String command = "ssh root@" + customDockerConfig.getRestIp()
                + " \"" + cmd + "\"";
        return command;
    }

    @Override
    public String createScpCommand(String from, String to) {
        String command = "scp " + from
                + " root@" + customDockerConfig.getRestIp() + ":" + to;
        return command;
    }

    @Override
    public String createScpRCommand(String from, String to) {
        String command = "scp -r " + from
                + " root@" + customDockerConfig.getRestIp() + ":" + to;
        return command;
    }
}
