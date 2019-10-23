package com.zrar.algorithmbridge.controller;

import com.zrar.algorithmbridge.util.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

/**
 * @author Jingfeng Zhou
 *
 * 参考：https://www.dozer.cc/2014/03/use-spring-mvc-and-resttemplate-impl-corsproxy.html
 */
@Slf4j
@RestController
public class BridgeController {

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 通过algorithm-bridge访问mleap-params-transformer，将文本转化为分词参数
     * @param request
     * @param headers
     * @param body
     * @return
     * @throws UnsupportedEncodingException
     */
    @RequestMapping("/mleap/params/transformer")
    public ResponseEntity<byte[]> mleapParamsTransformer(HttpServletRequest request,
                                                         @RequestHeader MultiValueMap<String, String> headers,
                                                         @RequestBody(required = false) byte[] body) throws UnsupportedEncodingException {
        String url = UrlUtils.getUrl(request.getScheme(), "mleap-params-transformer", 8080, "getParams");
        String queryString = request.getQueryString();
        if (!StringUtils.isEmpty(queryString)) {
            url = url + "?" + queryString;
        }
        log.debug("url = {}, queryString = {}", url, queryString);

        ResponseEntity<byte[]> result = null;
        try {
            result = restTemplate.exchange(url, HttpMethod.valueOf(request.getMethod()), new HttpEntity<>(body, headers), byte[].class);
        } catch (Exception exp) {
            return new ResponseEntity<>(exp.getMessage().getBytes("utf-8"), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(result.getBody(), result.getHeaders(), result.getStatusCode());
    }

    /**
     * 通过algorithm-bridge访问tensorflow-params-transformer，将文本转化为tensorflow所需要的参数
     * @param request
     * @param headers
     * @param body
     * @return
     * @throws UnsupportedEncodingException
     */
    @RequestMapping("/tensorflow/params/transformer")
    public ResponseEntity<byte[]> tensorflowParamsTransformer(HttpServletRequest request,
                                                         @RequestHeader MultiValueMap<String, String> headers,
                                                         @RequestBody(required = false) byte[] body) throws UnsupportedEncodingException {
        String url = UrlUtils.getUrl(request.getScheme(), "tensorflow-params-transformer", 5000, "getParams");
        String queryString = request.getQueryString();
        if (!StringUtils.isEmpty(queryString)) {
            url = url + "?" + queryString;
        }
        log.debug("url = {}, queryString = {}", url, queryString);

        ResponseEntity<byte[]> result = null;
        try {
            result = restTemplate.exchange(url, HttpMethod.valueOf(request.getMethod()), new HttpEntity<>(body, headers), byte[].class);
        } catch (Exception exp) {
            return new ResponseEntity<>(exp.getMessage().getBytes("utf-8"), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(result.getBody(), result.getHeaders(), result.getStatusCode());
    }

    /**
     * 通过algorithm-bridge访问mleap模型预测结果
     * @param request
     * @param headers
     * @param body
     * @param mleap
     * @param uri
     * @return
     * @throws UnsupportedEncodingException
     */
    @RequestMapping("/mleap/{mleap}/{uri}")
    public ResponseEntity<byte[]> mleapPredictRedirect(HttpServletRequest request,
                                           @RequestHeader MultiValueMap<String, String> headers,
                                           @RequestBody(required = false) byte[] body,
                                           @PathVariable("mleap") String mleap,
                                           @PathVariable("uri") String uri) throws UnsupportedEncodingException {

        log.debug("mleap = {}, uri = {}", mleap, uri);
        String url = UrlUtils.getUrl(request.getScheme(), mleap, 65327, uri);
        String queryString = request.getQueryString();
        if (!StringUtils.isEmpty(queryString)) {
            url = url + "?" + queryString;
        }
        log.debug("url = {}, queryString = {}", url, queryString);

        ResponseEntity<byte[]> result = null;
        try {
            result = restTemplate.exchange(url, HttpMethod.valueOf(request.getMethod()), new HttpEntity<>(body, headers), byte[].class);
        } catch (Exception exp) {
            return new ResponseEntity<>(exp.getMessage().getBytes("utf-8"), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(result.getBody(), result.getHeaders(), result.getStatusCode());
    }

    /**
     * 通过algorithm-bridge访问tensorflow模型预测结果
     * @param request
     * @param headers
     * @param body
     * @param tensorflow
     * @return
     * @throws UnsupportedEncodingException
     */
    @RequestMapping("/tensorflow/{tensorflow}/**")
    public ResponseEntity<byte[]> tensorflowPredictRedirect(HttpServletRequest request,
                                           @RequestHeader MultiValueMap<String, String> headers,
                                           @RequestBody(required = false) byte[] body,
                                           @PathVariable("tensorflow") String tensorflow) throws UnsupportedEncodingException {

        log.debug("tensorflow = {}", tensorflow);
        String uri = request.getServletPath().replace("/tensorflow/" + tensorflow + "/", "");
        log.debug("uri = {}", uri);
        String url = UrlUtils.getUrl(request.getScheme(), tensorflow, 8501, uri);
        String queryString = request.getQueryString();
        if (!StringUtils.isEmpty(queryString)) {
            url = url + "?" + queryString;
        }
        log.debug("url = {}, queryString = {}", url, queryString);

        ResponseEntity<byte[]> result = null;
        try {
            result = restTemplate.exchange(url, HttpMethod.valueOf(request.getMethod()), new HttpEntity<>(body, headers), byte[].class);
        } catch (Exception exp) {
            if (exp instanceof HttpClientErrorException) {
                HttpClientErrorException e = (HttpClientErrorException) exp;
                return new ResponseEntity(e.getResponseBodyAsByteArray(), e.getResponseHeaders(), e.getStatusCode());
            } else {
                return new ResponseEntity<>(exp.getMessage().getBytes("utf-8"), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(result.getBody(), result.getHeaders(), result.getStatusCode());
    }
}
