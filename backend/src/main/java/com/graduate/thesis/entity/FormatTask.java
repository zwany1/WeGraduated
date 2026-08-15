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

    /** 预览 PDF 相对路径(缓存, 排版成功后生成) */
    private String pdfPath;

    private String errorMsg;

    /** 失败自动重试次数 */
    private Integer retryCount;

    /** 排版校验摘要, 如 "一级标题5个/二级标题12个/正文段落120个/图表题注8个" */
    private String summary;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;

    /** 非表字段: 源论文文件名 */
    @TableField(exist = false)
    private String originalName;
}
