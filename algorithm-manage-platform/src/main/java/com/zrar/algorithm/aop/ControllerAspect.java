/**
 *
 */
package com.zrar.algorithm.aop;

import com.zrar.algorithm.constant.ResultEnum;
import com.zrar.algorithm.exception.AlgorithmException;
import com.zrar.algorithm.util.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhailiang
 */
@Aspect
@Component
@Slf4j
public class ControllerAspect {

    @Around("execution(* com.zrar.algorithm.controller.*.*(..))")
    public Object handleValidateResult(ProceedingJoinPoint pjp) {

        // 首先检测参数是否合法
        Object[] args = pjp.getArgs();
        for (Object arg : args) {
            if (arg instanceof BindingResult) {
                BindingResult errors = (BindingResult) arg;
                if (errors.hasErrors()) {
                    List<ObjectError> objectErrorList = errors.getAllErrors();
                    List<String> stringList = objectErrorList.stream().map(objectError -> objectError.getDefaultMessage()).collect(Collectors.toList());
                    return ResultUtils.error(ResultEnum.FORM_ERROR.getCode(), stringList.toString());
                }
            }
        }

        // 然后检测运行是否正常
        try {
            Object result = pjp.proceed();
            return result;
        } catch (AlgorithmException e) {
            log.error("e = {}", e);
            return ResultUtils.error(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("e = {}", e);
            return ResultUtils.error(-1, "未知错误");
        }
    }

}
