package com.graduate.thesis.controller;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.LoginResponse;
import com.graduate.thesis.service.GitHubOAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GitHub 第三方登录回调(无需登录, 已在 WebConfig 白名单排除)
 */
@RestController
@RequestMapping("/auth/github")
public class OAuthController {

    private final GitHubOAuthService oauthService;
    private final String frontendUrl;
    /** state 有效期: state -> 过期时间戳 */
    private final ConcurrentHashMap<String, Long> states = new ConcurrentHashMap<>();

    public OAuthController(GitHubOAuthService oauthService,
                           @Value("${thesis.oauth.github.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.oauthService = oauthService;
        this.frontendUrl = frontendUrl;
    }

    /** 是否已配置 GitHub 登录(前端据此决定是否展示按钮) */
    @GetMapping("/enabled")
    public com.graduate.thesis.common.Result<Boolean> enabled() {
        return com.graduate.thesis.common.Result.ok(oauthService.isEnabled());
    }

    /** 1. 跳转 GitHub 授权页 */
    @GetMapping("/authorize")
    public void authorize(HttpServletResponse response) {
        if (!oauthService.isEnabled()) {
            throw new BusinessException(500, "GitHub 登录未配置，请联系管理员");
        }
        String state = oauthService.randomState();
        states.put(state, System.currentTimeMillis() + 10 * 60 * 1000L);
        response.setStatus(302);
        response.setHeader("Location", oauthService.authorizeUrl(state));
    }

    /** 2. GitHub 回调: 换 token -> 登录/注册 -> 跳回前端携带登录态 */
    @GetMapping("/callback")
    public void callback(@RequestParam("code") String code,
                         @RequestParam(value = "state", required = false) String state,
                         HttpServletRequest request, HttpServletResponse response) {
        try {
            if (state == null || !states.containsKey(state)) {
                throw new BusinessException("OAuth 状态校验失败，请重新登录");
            }
            states.remove(state);
            String ip = clientIp(request);
            LoginResponse login = oauthService.loginByCode(code, ip);
            redirect(response, "/oauth/callback?token=" + encode(login.getToken()));
        } catch (Exception e) {
            String msg = e instanceof BusinessException ? e.getMessage() : "登录失败";
            redirect(response, "/oauth/callback?error=" + encode(msg));
        }
    }

    private void redirect(HttpServletResponse response, String path) {
        response.setStatus(302);
        response.setHeader("Location", frontendUrl + path);
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
