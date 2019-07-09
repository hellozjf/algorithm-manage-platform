package com.zrar.tools.mleapbridge.util;

/**
 * @author Jingfeng Zhou
 */
public class UrlUtils {
    public static String getUrl(String http, String host, String uri) {
        if (http == null) {
            http = "https";
        }
        return http + "://" + host + "/" + uri;
    }

    public static String getUrl(String http, String host, Integer port, String uri) {
        if (http == null) {
            http = "https";
        }
        return http + "://" + host + ":" + port + "/" + uri;
    }
}
