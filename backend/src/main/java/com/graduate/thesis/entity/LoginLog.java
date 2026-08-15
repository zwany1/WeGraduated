package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志
 */
@Data
@TableName("t_login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String ip;

    private String userAgent;

    /** 1成功 0失败 */
    private Boolean status;

    private String message;

    private LocalDateTime createTime;
}
