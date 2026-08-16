package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 团队成员
 */
@Data
@TableName("t_team_member")
public class TeamMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private Long userId;

    /** owner / member */
    private String role;

    private LocalDateTime joinTime;
}
