package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户反馈
 */
@Data
@TableName("t_feedback")
public class Feedback {

    /** 待处理 */
    public static final String STATUS_PENDING = "PENDING";
    /** 已回复 */
    public static final String STATUS_REPLIED = "REPLIED";
    /** 已关闭 */
    public static final String STATUS_CLOSED = "CLOSED";

    /** 功能建议 */
    public static final String CATEGORY_SUGGESTION = "suggestion";
    /** Bug 反馈 */
    public static final String CATEGORY_BUG = "bug";
    /** 其他 */
    public static final String CATEGORY_OTHER = "other";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交者ID */
    private Long userId;

    /** 分类: suggestion/bug/other */
    private String category;

    private String content;

    /** 图片(base64 data URL 的 JSON 数组字符串) */
    private String images;

    /** 选填联系方式 */
    private String contact;

    /** 状态: PENDING/REPLIED/CLOSED */
    private String status;

    /** 管理员回复 */
    private String reply;

    /** 回复管理员ID */
    private Long replyUserId;

    private LocalDateTime replyTime;

    private LocalDateTime createTime;
}
