package com.zrar.ai.exception;

import com.zrar.ai.constant.ResultEnum;
import lombok.Getter;

/**
 * @author Jingfeng Zhou
 */
@Getter
public class AlgorithmException extends RuntimeException {

    private Integer code;

    public AlgorithmException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public AlgorithmException(ResultEnum resultEnum) {
        super(resultEnum.getMessage());
        this.code = resultEnum.getCode();
    }
}
