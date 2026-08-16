package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.LoginResponse;
import com.graduate.thesis.entity.Role;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.entity.UserRole;
import com.graduate.thesis.mapper.RoleMapper;
import com.graduate.thesis.mapper.UserMapper;
import com.graduate.thesis.mapper.UserRoleMapper;
import com.graduate.thesis.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * GitHub 第三方登录(OAuth2 Authorization Code 流程)。
 * 需在 application.yml / 环境变量配置 GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET 后启用。
 */
@Service
public class GitHubOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GitHubOAuthService.class);

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final JwtUtil jwtUtil;
    private final LoginSessionService sessionService;
    private final PermissionService permissionService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GitHubOAuthService(@Value("${thesis.oauth.github.client-id:}") String clientId,
                              @Value("${thesis.oauth.github.client-secret:}") String clientSecret,
                              @Value("${thesis.oauth.github.redirect-uri:}") String redirectUri,
                              UserMapper userMapper, RoleMapper roleMapper,
                              UserRoleMapper userRoleMapper, JwtUtil jwtUtil,
                              LoginSessionService sessionService,
                              PermissionService permissionService) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.jwtUtil = jwtUtil;
        this.sessionService = sessionService;
        this.permissionService = permissionService;
    }

    public boolean isEnabled() {
        return clientId != null && !clientId.isEmpty() && clientSecret != null && !clientSecret.isEmpty();
    }

    /** 构造跳转 GitHub 授权页的 URL */
    public String authorizeUrl(String state) {
        if (!isEnabled()) {
            throw new BusinessException(500, "GitHub 登录未配置，请联系管理员");
        }
        return "https://github.com/login/oauth/authorize?client_id=" + clientId
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=read:user&state=" + encode(state);
    }

    /** 用授权码换取 access_token, 再拉取用户信息并登录/注册 */
    public LoginResponse loginByCode(String code, String ip) {
        String accessToken = exchangeCode(code);
        JsonNode gh = fetchUser(accessToken);
        String githubId = gh.path("id").asText("");
        String login = gh.path("login").asText("");
        String email = gh.path("email").isNull() ? "" : gh.path("email").asText("");
        String avatarUrl = gh.path("avatar_url").isNull() ? "" : gh.path("avatar_url").asText("");
        if (githubId.isEmpty()) {
            throw new BusinessException(500, "GitHub 授权信息无效");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getGithubId, githubId).last("LIMIT 1"));
        boolean isNew = false;
        if (user == null) {
            user = createUser(githubId, login, email, avatarUrl);
            isNew = true;
        } else {
            // 更新 GitHub 登录名/头像信息
            user.setGithubLogin(login);
            if (email != null && !email.isEmpty() && (user.getEmail() == null || user.getEmail().isEmpty())) {
                user.setEmail(email);
            }
            userMapper.updateById(user);
        }
        if (user.getStatus() != null && !user.getStatus()) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        LoginResponse resp = buildLoginResponse(user);
        sessionService.createSession(resp.getToken(), user.getId(), user.getUsername(), ip,
                jwtUtil.getExpiration(resp.getToken()).getTime());
        log.info("GitHub 登录成功 userId={} isNew={}", user.getId(), isNew);
        return resp;
    }

    private User createUser(String githubId, String login, String email, String avatarUrl) {
        String base = login == null || login.isEmpty() ? "github" : login;
        String username = base;
        int i = 1;
        while (isUsernameTaken(username)) {
            username = base + (i++);
        }
        User user = new User();
        user.setUsername(username);
        user.setNickname(base);
        user.setEmail(email == null || email.isEmpty() ? null : email);
        user.setPassword(""); // 第三方账号不可密码登录
        user.setRole(User.ROLE_USER);
        user.setStatus(true);
        user.setGithubId(githubId);
        user.setGithubLogin(login);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        assignDefaultUserRole(user.getId());
        return user;
    }

    private boolean isUsernameTaken(String username) {
        Long c = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return c != null && c > 0;
    }

    private void assignDefaultUserRole(Long userId) {
        Role userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleKey, Role.KEY_USER).last("LIMIT 1"));
        if (userRole != null) {
            Long exists = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, userId).eq(UserRole::getRoleId, userRole.getId()));
            if (exists == null || exists == 0) {
                userRoleMapper.insert(new UserRole(userId, userRole.getId()));
            }
        }
    }

    private String exchangeCode(String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("code", code);
            body.add("redirect_uri", redirectUri);
            String resp = restTemplate.postForObject(
                    "https://github.com/login/oauth/access_token",
                    new HttpEntity<>(body, headers), String.class);
            JsonNode node = objectMapper.readTree(resp);
            String token = node.path("access_token").asText("");
            if (token.isEmpty()) {
                throw new BusinessException(500, "GitHub 授权失败: " + node.path("error_description").asText("未知错误"));
            }
            return token;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GitHub 换 token 失败", e);
            throw new BusinessException(500, "GitHub 授权失败");
        }
    }

    private JsonNode fetchUser(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            org.springframework.http.ResponseEntity<String> resp = restTemplate.exchange(
                    "https://api.github.com/user",
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            return objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            log.error("GitHub 拉取用户信息失败", e);
            throw new BusinessException(500, "获取 GitHub 用户信息失败");
        }
    }

    private LoginResponse buildLoginResponse(User user) {
        LoginResponse resp = new LoginResponse();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setEmail(user.getEmail());
        resp.setRole(user.getRole() == null ? User.ROLE_USER : user.getRole());
        resp.setRoles(new java.util.ArrayList<>(permissionService.getUserRoleKeys(user.getId())));
        resp.setPerms(permissionService.getUserPerms(user.getId()));
        resp.setToken(jwtUtil.generate(user.getId()));
        return resp;
    }

    /** 生成 OAuth state(防 CSRF) */
    public String randomState() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(Character.forDigit((x & 0xF0) >> 4, 16)).append(Character.forDigit(x & 0x0F, 16));
        }
        return sb.toString();
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
