package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 标题识别规则保存请求
 */
@Data
public class HeadingPatternsSaveDTO {

    /** 一级标题正则 */
    @NotBlank(message = "一级标题规则不能为空")
    private String heading1;

    /** 二级标题正则 */
    @NotBlank(message = "二级标题规则不能为空")
    private String heading2;

    /** 三级标题正则 */
    @NotBlank(message = "三级标题规则不能为空")
    private String heading3;
}
