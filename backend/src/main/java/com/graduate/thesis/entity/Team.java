package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 团队
 */
@Data
@TableName("t_team")
public class Team {

    public static final String ROLE_OWNER = "owner";
    public static final String ROLE_MEMBER = "member";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Long ownerId;

    private LocalDateTime createTime;
}
