package com.graduate.thesis.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 工具
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    /** 已撤销 token: token -> 过期时间戳(用于清理) */
    private final ConcurrentHashMap<String, Long> revokedTokens = new ConcurrentHashMap<>();
    /** 用户级撤销时间点: userId -> 撤销时间戳, 早于此时间签发的 token 全部失效 */
    private final ConcurrentHashMap<Long, Long> revokedSince = new ConcurrentHashMap<>();

    public JwtUtil(@Value("${thesis.jwt.secret}") String secret,
                   @Value("${thesis.jwt.expire-hours}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600 * 1000L;
    }

    public String generate(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.valueOf(claims.getSubject());
    }

    public Date getIssuedAt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getIssuedAt();
    }

    public Date getExpiration(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }

    /** 撤销单个 token */
    public void revoke(String token) {
        try {
            Long exp = getExpiration(token).getTime();
            revokedTokens.put(token, exp);
        } catch (Exception ignored) {
        }
    }

    /** 撤销某用户的全部 token(重置密码场景) */
    public void revokeAllForUser(Long userId) {
        revokedSince.put(userId, System.currentTimeMillis());
    }

    /** 检查 token 是否已被撤销 */
    public boolean isRevoked(String token) {
        // 清理已过期 token
        revokedTokens.entrySet().removeIf(e -> e.getValue() < System.currentTimeMillis());
        if (revokedTokens.containsKey(token)) {
            return true;
        }
        try {
            Long userId = parseUserId(token);
            Long since = revokedSince.get(userId);
            if (since != null && getIssuedAt(token).getTime() < since) {
                return true;
            }
        } catch (Exception ignored) {
            return true;
        }
        return false;
    }
}
