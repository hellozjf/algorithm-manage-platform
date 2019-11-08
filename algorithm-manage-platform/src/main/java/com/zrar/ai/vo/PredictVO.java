package com.zrar.ai.vo;

import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
public class PredictVO {
    private int type;
    private int version;
    private String shortName;
    private String sentence;
}
