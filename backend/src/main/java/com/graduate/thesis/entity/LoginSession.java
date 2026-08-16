package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录会话
 */
@Data
@TableName("t_login_session")
public class LoginSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    /** 完整 JWT */
    private String token;

    private String ip;

    private LocalDateTime loginTime;

    /** 过期时间戳(毫秒) */
    private Long expireTime;
}
