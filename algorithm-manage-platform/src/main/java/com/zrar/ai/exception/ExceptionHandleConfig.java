package com.zrar.ai.exception;

import com.zrar.ai.util.ResultUtils;
import com.zrar.ai.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 统一异常处理
 * Created by 廖师兄
 * 2017-01-21 13:59
 */
@Slf4j
@ControllerAdvice
public class ExceptionHandleConfig {

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResponseEntity handle(Exception e) {
        if (e instanceof AlgorithmException) {
            AlgorithmException exception = (AlgorithmException) e;
            ResultVO resultVO = ResultUtils.error(exception.getCode(), exception.getMessage());
            return new ResponseEntity(resultVO, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            log.error("【系统异常】{}", e);
            ResultVO resultVO = ResultUtils.error(-1, "未知错误");
            return new ResponseEntity(resultVO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
