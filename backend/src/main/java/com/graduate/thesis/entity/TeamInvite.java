package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 团队邀请(需被邀请人同意后才加入)
 */
@Data
@TableName("t_team_invite")
public class TeamInvite {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    /** 被邀请用户 */
    private Long userId;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime handleTime;
}
