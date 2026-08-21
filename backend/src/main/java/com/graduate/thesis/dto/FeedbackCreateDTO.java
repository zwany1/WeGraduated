package com.graduate.thesis.dto;

import lombok.Data;

import java.util.List;

/**
 * 提交反馈请求体
 */
@Data
public class FeedbackCreateDTO {

    /** 分类: suggestion/bug/other */
    private String category;

    private String content;

    /** 选填联系方式 */
    private String contact;

    /** 图片(base64 data URL 列表) */
    private List<String> images;
}
