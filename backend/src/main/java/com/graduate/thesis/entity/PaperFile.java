package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论文文件
 */
@Data
@TableName("t_paper_file")
public class PaperFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 所属团队(空=个人) */
    private Long teamId;

    private String originalName;

    /** 存储相对路径 */
    private String storedPath;

    private Long fileSize;

    private LocalDateTime createTime;

    /** 非表字段: 关联排版任务数 */
    @TableField(exist = false)
    private Integer taskCount;
}
