package com.graduate.thesis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * XSS 防护过滤器: 对 JSON 请求体的字符串字段做 HTML 转义(存储前消毒)
 * 仅处理 application/json, 不影响文件上传与下载
 */
@Component
@Order(1)
public class XssFilter extends OncePerRequestFilter {

    /** JSON 请求体消毒上限: 超过直接拒绝, 避免超大 body 全量缓冲进内存 */
    private static final int MAX_JSON_BODY = 30 * 1024 * 1024;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")
                && !isMultipart(request)) {
            int len = request.getContentLength();
            if (len > MAX_JSON_BODY) {
                response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "请求体过大");
                return;
            }
            byte[] body = readBody(request);
            String json = new String(body, StandardCharsets.UTF_8);
            String cleaned = sanitizeJson(json);
            byte[] cleanedBody = cleaned.getBytes(StandardCharsets.UTF_8);
            XssRequestWrapper wrapper = new XssRequestWrapper(request, cleanedBody);
            chain.doFilter(wrapper, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private byte[] readBody(HttpServletRequest request) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        javax.servlet.ServletInputStream in = request.getInputStream();
        while ((len = in.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    private boolean isMultipart(HttpServletRequest request) {
        String ct = request.getContentType();
        return ct != null && ct.toLowerCase().contains("multipart/");
    }

    /** 递归转义 JSON 中的字符串值 */
    @SuppressWarnings("unchecked")
    private String sanitizeJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        try {
            Object root = objectMapper.readValue(json, Object.class);
            Object cleaned = sanitize(root);
            return objectMapper.writeValueAsString(cleaned);
        } catch (Exception e) {
            // 解析失败(非 JSON 或格式异常), 原样放行
            return json;
        }
    }

    /** 不转义字段: 密码/验证码等敏感字段, 以及内部 JSON 配置字段(其引号是 JSON 语法, 转义会破坏结构) */
    private static final java.util.Set<String> NO_ESCAPE_KEYS = new java.util.HashSet<>(java.util.Arrays.asList(
            "password", "newPassword", "confirmPassword", "emailCode", "captchaCode", "securityAnswer",
            "pageConfig", "headingPatterns", "coverConfig", "referenceConfig", "images"
    ));

    @SuppressWarnings("unchecked")
    private Object sanitize(Object value) {
        if (value instanceof String) {
            return escapeHtml((String) value);
        }
        if (value instanceof Map) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) value).entrySet()) {
                if (NO_ESCAPE_KEYS.contains(e.getKey())) {
                    map.put(e.getKey(), e.getValue());
                } else {
                    map.put(e.getKey(), sanitize(e.getValue()));
                }
            }
            return map;
        }
        if (value instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                list.add(sanitize(item));
            }
            return list;
        }
        return value;
    }

    /** 转义 HTML 特殊字符 */
    private String escapeHtml(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                case '&': sb.append("&amp;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 包装请求, 用清洗后的 body 替代原 body */
    private static class XssRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        XssRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public javax.servlet.ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new javax.servlet.ServletInputStream() {
                @Override
                public int read() {
                    return bais.read();
                }

                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(javax.servlet.ReadListener readListener) {
                }
            };
        }
    }
}
