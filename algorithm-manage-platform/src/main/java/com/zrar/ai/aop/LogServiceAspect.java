package com.zrar.ai.aop;

import com.zrar.ai.annotation.Log;
import com.zrar.ai.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * @author chenzhangyi
 */
@Aspect
@Component
@Slf4j
public class LogServiceAspect {
    @Autowired
    private LogService logService;

    @Around("execution(* com.zrar.ai.controller.WebController.*(..)) && @annotation(com.zrar.ai.annotation.Log)")
    public  Object  addLog(ProceedingJoinPoint jp) throws Throwable {
        Object result;
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method1 = signature.getMethod();
        String method = method1.getName();
        //日志描述
        Log annotation = method1.getAnnotation(Log.class);
        String description = annotation.value();
        //访问来源
        String ip = getIpAddress();
        StringBuilder params = new StringBuilder("{");
        //参数值
        Object[] argValues = jp.getArgs();
        //参数名称
        String[] argNames = ((MethodSignature)jp.getSignature()).getParameterNames();
        if(argValues != null){
            for (int i = 0; i < argValues.length; i++) {
                params.append(" ").append(argNames[i]).append(": ").append(argValues[i]);
            }
        }
        String  parameters = params.toString()+"}";
        long befTime = System.currentTimeMillis();
        result = jp.proceed();
        long nowTime = System.currentTimeMillis();
        Date date = new Date(nowTime);
        Long cost =nowTime-befTime;
        logService.addLog(method,ip,parameters,description,cost,date);
        return result;
    }

    /**
     * 获取ip
     * @return
     */
    public static String getIpAddress() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
