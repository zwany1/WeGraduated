package com.graduate.thesis.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台任务行
 */
@Data
public class AdminTaskVO {

    private Long id;

    private Long userId;

    private String username;

    private Long templateId;

    private String templateName;

    private String originalName;

    /** PENDING / PROCESSING / SUCCESS / FAILED */
    private String status;

    private Integer progress;

    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
