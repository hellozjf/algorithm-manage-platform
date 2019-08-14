package com.zrar.algorithm.service;

import java.util.List;

/**
 * @author Jingfeng Zhou
 */
public interface StopWordService {
    List<String> getStopWordByPath(String path);
}
