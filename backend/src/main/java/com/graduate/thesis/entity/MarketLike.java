package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板市场点赞(每用户每模板最多一次)
 */
@Data
@TableName("t_market_like")
public class MarketLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private Long userId;

    private LocalDateTime createTime;
}
