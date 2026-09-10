package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.entity.LoginSession;
import com.graduate.thesis.mapper.LoginSessionMapper;
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
 * 登录会话管理: t_login_session 会话行是 token 有效性的权威依据, 行存在即有效.
 * 登出/踢下线/改密/封禁均删除会话行, 撤销因此跨重启持久生效.
 */
@Slf4j
@Service
public class LoginSessionService {

    private final LoginSessionMapper sessionMapper;

    /** 最近活跃时间(内存): token -> 时间戳, 避免高频写库 */
    private final ConcurrentHashMap<String, Long> lastActive = new ConcurrentHashMap<>();

    public LoginSessionService(LoginSessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
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

    /** token 是否有效: 签名与有效期由 JwtUtil 校验, 会话行存在即未被撤销 */
    public boolean isActive(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        Long count = sessionMapper.selectCount(new LambdaQueryWrapper<LoginSession>()
                .eq(LoginSession::getToken, token));
        return count != null && count > 0;
    }

    /** 在线会话列表(仅未过期的) */
    public List<Map<String, Object>> listOnline() {
        List<LoginSession> all = sessionMapper.selectList(null);
        List<Map<String, Object>> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (LoginSession s : all) {
            if (s.getExpireTime() != null && s.getExpireTime() < now) {
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
        sessionMapper.deleteById(sessionId);
        lastActive.remove(s.getToken());
    }

    /** 登出时清理会话 */
    public void removeByToken(String token) {
        if (token != null && !token.isEmpty()) {
            sessionMapper.delete(new LambdaQueryWrapper<LoginSession>()
                    .eq(LoginSession::getToken, token));
            lastActive.remove(token);
        }
    }

    /** 撤销某用户的全部会话(改密/封禁/角色变更场景) */
    public void revokeAllForUser(Long userId) {
        List<LoginSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<LoginSession>()
                .eq(LoginSession::getUserId, userId));
        sessionMapper.delete(new LambdaQueryWrapper<LoginSession>()
                .eq(LoginSession::getUserId, userId));
        for (LoginSession s : sessions) {
            lastActive.remove(s.getToken());
        }
    }

    /** 每小时清理过期会话 */
    @Scheduled(fixedDelay = 3600000)
    public void cleanup() {
        try {
            List<LoginSession> expired = sessionMapper.selectList(new LambdaQueryWrapper<LoginSession>()
                    .lt(LoginSession::getExpireTime, System.currentTimeMillis()));
            for (LoginSession s : expired) {
                lastActive.remove(s.getToken());
            }
            sessionMapper.delete(new LambdaQueryWrapper<LoginSession>()
                    .lt(LoginSession::getExpireTime, System.currentTimeMillis()));
        } catch (Exception e) {
            log.warn("清理登录会话失败: {}", e.getMessage());
        }
    }
}
