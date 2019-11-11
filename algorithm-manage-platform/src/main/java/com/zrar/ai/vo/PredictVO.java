package com.zrar.ai.vo;

import lombok.Data;

/**
 * @author Jingfeng Zhou
 */
@Data
public class PredictVO {
    private String type;
    private Integer version;
    private String shortName;
    private String sentence;
}
