package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 格式规则
 */
@Data
@TableName("t_format_rule")
public class FormatRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    /** 规则类型: heading1 / heading2 / heading3 / body / figure / table */
    private String ruleType;

    private String font;

    /** 西文字体(ascii/hAnsi), 默认 Times New Roman */
    private String fontLatin;

    private Integer fontSize;

    private Boolean bold;

    private String align;

    /** 行距倍数 */
    private Float lineSpacing;

    /** 行距类型: multiple(多倍) / exact(固定值磅) */
    private String lineSpacingType;

    /** 固定值行距(磅), 当 lineSpacingType=exact 时生效 */
    private Integer lineSpacingExact;

    /** 首行缩进(字符数) */
    private Integer firstLineIndent;

    /** 段前距(pt) */
    private Integer spaceBefore;

    /** 段后距(pt) */
    private Integer spaceAfter;

    /** 图表标题位置: below(图) / above(表) */
    private String captionPosition;

    /** 编号格式: 图{chapter}-{no} / 表{chapter}-{no} */
    private String numberingPattern;

    /** 是否启用图表题注编号(下标) */
    private Boolean captionEnabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
