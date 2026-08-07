package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排版任务
 */
@Data
@TableName("t_format_task")
public class FormatTask {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long fileId;

    private Long templateId;

    /** PENDING / PROCESSING / SUCCESS / FAILED */
    private String status;

    private Integer progress;

    /** 结果文件相对路径 */
    private String resultPath;

    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;

    /** 非表字段: 源论文文件名 */
    @TableField(exist = false)
    private String originalName;
}
