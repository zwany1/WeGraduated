package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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

    private String originalName;

    /** 存储相对路径 */
    private String storedPath;

    private Long fileSize;

    private LocalDateTime createTime;
}
