package com.graduate.thesis.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台模板行
 */
@Data
public class AdminTemplateVO {

    private Long id;

    private String name;

    private Long userId;

    private String username;

    private Boolean generateToc;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private long ruleCount;

    private long taskCount;
}
