package com.graduate.thesis.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户反馈 VO(前台反馈墙 + 后台管理共用)
 */
@Data
public class FeedbackVO {

    private Long id;

    private Long userId;

    /** 提交者用户名 */
    private String username;

    /** 提交者昵称 */
    private String nickname;

    /** 提交者头像(data URL base64) */
    private String avatar;

    /** 分类: suggestion/bug/other */
    private String category;

    private String content;

    /** 图片列表(base64 data URL) */
    private List<String> images;

    /** 选填联系方式 */
    private String contact;

    /** 状态: PENDING/REPLIED/CLOSED */
    private String status;

    /** 管理员回复 */
    private String reply;

    private Long replyUserId;

    /** 回复管理员用户名 */
    private String replyUsername;

    private LocalDateTime replyTime;

    private LocalDateTime createTime;
}
