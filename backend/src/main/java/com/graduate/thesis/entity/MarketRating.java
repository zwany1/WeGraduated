package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板市场评分(每个用户对每个市场模板一条)
 */
@Data
@TableName("t_market_rating")
public class MarketRating {

    private Long userId;

    private Long templateId;

    private Integer score;

    private LocalDateTime createTime;
}
