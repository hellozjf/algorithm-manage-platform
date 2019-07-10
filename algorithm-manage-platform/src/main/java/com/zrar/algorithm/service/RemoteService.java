package com.zrar.algorithm.service;

/**
 * @author Jingfeng Zhou
 */
public interface RemoteService {
    String createExecCommand(String cmd);
    String createScpCommand(String from, String to);
    String createScpRCommand(String from, String to);
}
