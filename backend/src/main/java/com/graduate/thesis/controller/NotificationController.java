package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.entity.Notification;
import com.graduate.thesis.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 站内信接口
 */
@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 我的通知列表 */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        List<Notification> list = notificationService.listByUser(UserContext.get());
        return Result.ok(list.stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("type", n.getType());
            m.put("title", n.getTitle());
            m.put("content", n.getContent());
            m.put("data", NotificationService.parseData(n.getData()));
            m.put("isRead", Boolean.TRUE.equals(n.getIsRead()));
            m.put("createTime", n.getCreateTime());
            return m;
        }).collect(Collectors.toList()));
    }

    /** 未读数 */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount(UserContext.get()));
    }

    /** 标记已读 */
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, UserContext.get());
        return Result.ok();
    }

    /** 全部已读 */
    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(UserContext.get());
        return Result.ok();
    }
}
