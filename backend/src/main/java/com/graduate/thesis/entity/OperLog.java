package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@TableName("t_oper_log")
public class OperLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    /** 模块, 如 用户管理 */
    private String module;

    /** 动作, 如 删除用户 */
    private String action;

    /** 请求方法: 类名#方法名 */
    private String method;

    /** 请求参数 */
    private String params;

    private String ip;

    /** 1成功 0失败 */
    private Boolean status;

    private String errorMsg;

    private Long costMs;

    private LocalDateTime createTime;
}
