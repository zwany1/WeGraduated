package com.graduate.thesis.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 排版进度 SSE 凭据: 一次性短期票据, 绑定 taskId + userId, 校验后销毁.
 */
@Service
public class ProgressTicketService {

    private static final long TTL_MILLIS = 60_000L;

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();

    public String generate(Long userId, Long taskId) {
        sweep();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = toHex(bytes);
        tickets.put(token, new Ticket(userId, taskId, System.currentTimeMillis() + TTL_MILLIS));
        return token;
    }

    /** 校验并销毁票据; 有效且 taskId 匹配返回 true */
    public boolean consume(String token, Long taskId) {
        if (token == null) {
            return false;
        }
        Ticket t = tickets.remove(token);
        if (t == null) {
            return false;
        }
        return t.expireAt >= System.currentTimeMillis() && taskId.equals(t.taskId);
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        tickets.entrySet().removeIf(e -> e.getValue().expireAt < now);
    }

    private static String toHex(byte[] bytes) {
        char[] hex = "0123456789abcdef".toCharArray();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(hex[(b >> 4) & 0xf]).append(hex[b & 0xf]);
        }
        return sb.toString();
    }

    private static class Ticket {
        final Long userId;
        final Long taskId;
        final long expireAt;

        Ticket(Long userId, Long taskId, long expireAt) {
            this.userId = userId;
            this.taskId = taskId;
            this.expireAt = expireAt;
        }
    }
}
