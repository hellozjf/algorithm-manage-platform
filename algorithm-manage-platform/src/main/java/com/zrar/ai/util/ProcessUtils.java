package com.zrar.ai.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Jingfeng Zhou
 */
@Slf4j
public class ProcessUtils {

    /**
     * 获取命令行返回的字符串结果
     * @param process
     * @return
     */
    public static String getInputStreamString(Process process) {
        try (InputStream inputStream = process.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            StringBuilder stringBuilder = new StringBuilder();
            String t = null;
            while ((t = bufferedReader.readLine()) != null) {
                stringBuilder.append(t);
                stringBuilder.append("\n");
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            log.error("e = {}", e);
            return "";
        }
    }
}
