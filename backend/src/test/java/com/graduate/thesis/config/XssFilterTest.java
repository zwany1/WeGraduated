package com.graduate.thesis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

/**
 * XssFilter 行为验证: 正则类字段不被 HTML 转义, 普通字符串字段转义, 非法 JSON 原样放行
 */
class XssFilterTest {

    private XssFilter filter;

    @BeforeEach
    void setUp() {
        filter = new XssFilter();
    }

    private String wrappedBody(String json) throws Exception {
        final String[] captured = new String[1];
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/template/1/heading-patterns");
        request.setContentType("application/json");
        request.setContent(json.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse res)
                    throws java.io.IOException, javax.servlet.ServletException {
                HttpServletRequest httpReq = (HttpServletRequest) req;
                captured[0] = new String(httpReq.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        };
        filter.doFilter(request, response, chain);
        return captured[0];
    }

    @Test
    void 正则字段中的反斜杠与中文应原样保留() throws Exception {
        String body = "{\"heading1\":\"^第[一-龥]+章\",\"heading2\":\"^\\\\d+\\\\.\\\\d+\",\"heading3\":\"^b\"}";
        String out = wrappedBody(body);
        // JSON 文本层面 \\d 表示值 \d, 过滤器 parse→sanitize→serialize 后不应丢失或转义成正则以外的内容
        ObjectMapper mapper = new ObjectMapper();
        var node = mapper.readTree(out);
        Assertions.assertEquals("^第[一-龥]+章", node.get("heading1").asText());
        Assertions.assertEquals("^\\d+\\.\\d+", node.get("heading2").asText());
    }

    @Test
    void 普通字符串字段的HTML特殊字符应被转义() throws Exception {
        String body = "{\"font\":\"<b>&</b>\"}";
        String out = wrappedBody(body);
        Assertions.assertTrue(out.contains("&lt;b&gt;&amp;&lt;/b&gt;"), "应转义 HTML 特殊字符: " + out);
    }

    @Test
    void 非法JSON应原样放行不抛异常() throws Exception {
        String body = "{\"heading2\":\"^\\d+\"}";
        Assertions.assertEquals(body, wrappedBody(body));
    }

    @Test
    void 非JSON请求体不包装() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/user/login");
        request.setContentType("application/x-www-form-urlencoded");
        request.setContent("a=1".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        Assertions.assertNotNull(chain.getRequest());
    }
}
