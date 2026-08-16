package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内信
 */
@Data
@TableName("t_notification")
public class Notification {

    public static final String TYPE_TEAM_INVITE = "team_invite";
    public static final String TYPE_TEAM_SYSTEM = "team_system";
    public static final String TYPE_SYSTEM = "system";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者 */
    private Long userId;

    private String type;

    private String title;

    private String content;

    /** JSON 扩展, 如团队邀请含 inviteId/teamId */
    private String data;

    private Boolean isRead;

    private LocalDateTime createTime;
}
