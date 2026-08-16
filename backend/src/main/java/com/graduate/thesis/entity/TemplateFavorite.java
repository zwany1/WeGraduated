package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板收藏
 */
@Data
@TableName("t_template_favorite")
public class TemplateFavorite {

    private Long userId;

    private Long templateId;

    private LocalDateTime createTime;
}
