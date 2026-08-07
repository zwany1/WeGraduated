package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 三线表生成请求
 */
@Data
public class Table3DTO {

    /** 完整表题(如: 表3-1 实验数据统计), 为空时自动拼接 */
    private String caption;

    /** 表题文字 */
    private String title;

    /** 是否自动编号(表{chapter}-{no}) */
    private Boolean autoNumber = true;

    /** 章节号 */
    private Integer chapterNo = 1;

    /** 表序号 */
    private Integer tableNo = 1;

    /** 列头 */
    @NotEmpty(message = "表头不能为空")
    private List<String> headers;

    /** 数据行 */
    @NotNull(message = "数据行不能为空")
    private List<List<String>> rows;

    /** 正文字号(pt), 默认 10(五号) */
    private Integer fontSize = 10;

    /** 对齐方式: center / left, 默认 center */
    private String align = "center";
}
