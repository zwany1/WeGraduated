package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 格式模板
 */
@Data
@TableName("t_format_template")
public class FormatTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 所属团队(空=个人) */
    private Long teamId;

    private String name;

    /** 页面设置 JSON: {"paper":"A4","margin":{...},"header":{"height":1.5},"footer":{"type":"center"}} */
    private String pageConfig;

    /** 标题识别正则 JSON: {"heading1":"^...章","heading2":"^\\d+\\.\\d+","heading3":"^\\d+\\.\\d+\\.\\d+"} */
    private String headingPatterns;

    /** 封面信息 JSON: {"enabled":true,"title":"...","college":"...","major":"...","studentName":"...","studentNo":"...","teacherUnit":"...","teacher":"...","teacherTitle":"...","topicType":"...","date":"..."} */
    private String coverConfig;

    /** 是否生成目录(默认 false) */
    private Boolean generateToc;

    /** 参考文献配置 JSON: {"enabled":true,"title":"参考文献","titleFont":"黑体","titleFontSize":14,"itemFont":"宋体","itemFontLatin":"Times New Roman","itemFontSize":10,"removeDoi":true,"maxAuthors":3,"renumber":true} */
    private String referenceConfig;

    /** 是否上架模板市场 */
    private Boolean isPublic;

    /** 是否推荐 */
    private Boolean recommended;

    /** 上架时间 */
    private LocalDateTime publicTime;

    /** 市场分类(毕业论文/期刊论文/报告文档等) */
    private String category;

    /** 下载量(市场复制次数) */
    private Integer downloadCount;

    /** 平均评分 */
    private java.math.BigDecimal ratingAvg;

    /** 评分人数 */
    private Integer ratingCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
