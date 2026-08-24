package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板市场评论(parentId 为空为一级评论, 否则为对该评论的回复)
 */
@Data
@TableName("t_market_comment")
public class MarketComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private Long userId;

    private String content;

    /** 父评论 id(回复), 一级评论为 null */
    private Long parentId;

    private LocalDateTime createTime;
}
