package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.entity.LoginSession;
import com.graduate.thesis.mapper.LoginSessionMapper;
import com.graduate.thesis.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录会话管理: 记录活跃登录, 支持后台查看在线用户与强制下线.
 * 踢下线通过 JWT 撤销 + 删除会话记录实现, 立即生效。
 */
@Slf4j
@Service
public class LoginSessionService {

    private final LoginSessionMapper sessionMapper;
    private final JwtUtil jwtUtil;

    /** 最近活跃时间(内存): token -> 时间戳, 避免高频写库 */
    private final ConcurrentHashMap<String, Long> lastActive = new ConcurrentHashMap<>();

    public LoginSessionService(LoginSessionMapper sessionMapper, JwtUtil jwtUtil) {
        this.sessionMapper = sessionMapper;
        this.jwtUtil = jwtUtil;
    }

    /** 登录成功后创建会话记录 */
    public void createSession(String token, Long userId, String username, String ip, long expireAt) {
        try {
            LoginSession s = new LoginSession();
            s.setUserId(userId);
            s.setUsername(username);
            s.setToken(token);
            s.setIp(ip == null || ip.isEmpty() ? null : ip);
            s.setLoginTime(LocalDateTime.now());
            s.setExpireTime(expireAt);
            sessionMapper.insert(s);
        } catch (Exception e) {
            log.warn("记录登录会话失败 userId={}: {}", userId, e.getMessage());
        }
        lastActive.put(token, System.currentTimeMillis());
    }

    /** 请求时更新最近活跃(登录拦截器调用) */
    public void touch(String token) {
        if (token != null && !token.isEmpty()) {
            lastActive.put(token, System.currentTimeMillis());
        }
    }

    /** 在线会话列表(仅未过期且未撤销的) */
    public List<Map<String, Object>> listOnline() {
        List<LoginSession> all = sessionMapper.selectList(null);
        List<Map<String, Object>> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (LoginSession s : all) {
            if (s.getExpireTime() != null && s.getExpireTime() < now) {
                continue;
            }
            if (jwtUtil.isRevoked(s.getToken())) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("userId", s.getUserId());
            m.put("username", s.getUsername());
            m.put("ip", s.getIp());
            m.put("loginTime", s.getLoginTime());
            Long last = lastActive.get(s.getToken());
            m.put("lastActive", last == null ? s.getLoginTime()
                    : LocalDateTime.ofInstant(Instant.ofEpochMilli(last), ZoneId.systemDefault()));
            out.add(m);
        }
        return out;
    }

    /** 强制下线某个会话 */
    public void kick(Long sessionId) {
        LoginSession s = sessionMapper.selectById(sessionId);
        if (s == null) {
            throw new BusinessException(404, "会话不存在或已失效");
        }
        jwtUtil.revoke(s.getToken());
        lastActive.remove(s.getToken());
        sessionMapper.deleteById(sessionId);
    }

    /** 登出时清理会话 */
    public void removeByToken(String token) {
        if (token != null && !token.isEmpty()) {
            sessionMapper.delete(new LambdaQueryWrapper<LoginSession>()
                    .eq(LoginSession::getToken, token));
            lastActive.remove(token);
        }
    }

    /** 每小时清理过期会话 */
    @Scheduled(fixedDelay = 3600000)
    public void cleanup() {
        try {
            sessionMapper.delete(new LambdaQueryWrapper<LoginSession>()
                    .lt(LoginSession::getExpireTime, System.currentTimeMillis()));
            lastActive.entrySet().removeIf(e -> {
                try {
                    return jwtUtil.isRevoked(e.getKey());
                } catch (Exception x) {
                    return true;
                }
            });
        } catch (Exception e) {
            log.warn("清理登录会话失败: {}", e.getMessage());
        }
    }
}
