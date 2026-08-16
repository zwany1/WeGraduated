package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.entity.Notification;
import com.graduate.thesis.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内信服务: 发送/列表/未读数/已读
 */
@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /** 发送站内信 */
    public Notification send(Long userId, String type, String title, String content, Map<String, Object> data) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setData(data == null ? null : toJson(data));
        n.setIsRead(false);
        n.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(n);
        return n;
    }

    /** 我的通知(新→旧) */
    public List<Notification> listByUser(Long userId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getId));
    }

    /** 未读数 */
    public long unreadCount(Long userId) {
        Long c = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false));
        return c == null ? 0 : c;
    }

    /** 标记单条已读 */
    public void markRead(Long id, Long userId) {
        Notification n = notificationMapper.selectById(id);
        if (n == null || !n.getUserId().equals(userId)) {
            throw new BusinessException(404, "通知不存在");
        }
        if (n.getIsRead() == null || !n.getIsRead()) {
            n.setIsRead(true);
            notificationMapper.updateById(n);
        }
    }

    /** 全部已读 */
    public void markAllRead(Long userId) {
        List<Notification> unread = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false));
        for (Notification n : unread) {
            n.setIsRead(true);
            notificationMapper.updateById(n);
        }
    }

    /** 解析 data 字段 */
    public static Map<String, Object> parseData(String data) {
        Map<String, Object> m = new HashMap<>();
        if (data == null || data.isEmpty()) {
            return m;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(data,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            return m;
        }
    }

    private static String toJson(Map<String, Object> data) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }
}
