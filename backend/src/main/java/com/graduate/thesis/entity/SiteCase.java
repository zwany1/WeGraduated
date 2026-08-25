package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目案例
 */
@Data
@TableName("t_site_case")
public class SiteCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签: 本科毕业论文 / 三线表 / ER图等 */
    private String tag;

    private String title;

    private String description;

    /** 预览色: blue / green / purple / orange */
    private String color;

    /** 案例署名(如"张同学") */
    private String author;

    private String school;

    /** 评分 0-5 */
    private BigDecimal rating;

    /** 排序, 小在前 */
    private Integer sortOrder;

    /** 前台是否展示 */
    private Boolean visible;

    /** 成果指标 JSON: {"pages":300,"minutes":5,"matchRate":99} */
    private String metrics;

    /** 详情正文(纯文本, 保留换行) */
    private String detail;

    /** 截图 base64 dataURL JSON 数组 */
    private String images;

    /** 关联模板 */
    private Long templateId;

    /** 关联真实排版任务(空=手写示范案例) */
    private Long taskId;

    private Long createUser;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ===== 派生字段(非表, 查询时填充) =====
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String templateName;
    @TableField(exist = false)
    private String originalName;
    @TableField(exist = false)
    private Long minutes;
    @TableField(exist = false)
    private Long publicTemplateId;
    @TableField(exist = false)
    private Boolean hasDoc;
    @TableField(exist = false)
    private String sourceType;
}
