package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 格式规则保存请求
 */
@Data
public class RuleSaveDTO {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    /** heading1 / heading2 / heading3 / body / figure / table */
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    private String font;
    private String fontLatin;
    private Integer fontSize;
    private Boolean bold;
    private String align;
    private Float lineSpacing;
    private String lineSpacingType;
    private Integer lineSpacingExact;
    private Integer firstLineIndent;
    private Integer spaceBefore;
    private Integer spaceAfter;
    private String captionPosition;
    private String numberingPattern;
    private Boolean captionEnabled;
}
