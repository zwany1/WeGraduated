package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告
 */
@Data
@TableName("t_notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 1通知 2公告 */
    private String noticeType;

    private Boolean status;

    private Long createUser;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
