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

    private String name;

    /** 页面设置 JSON: {"paper":"A4","margin":{...},"header":{"height":1.5},"footer":{"type":"center"}} */
    private String pageConfig;

    /** 标题识别正则 JSON: {"heading1":"^...章","heading2":"^\\d+\\.\\d+","heading3":"^\\d+\\.\\d+\\.\\d+"} */
    private String headingPatterns;

    /** 封面信息 JSON: {"enabled":true,"title":"...","college":"...","major":"...","studentName":"...","studentNo":"...","teacherUnit":"...","teacher":"...","teacherTitle":"...","topicType":"...","date":"..."} */
    private String coverConfig;

    /** 是否生成目录(默认 false) */
    private Boolean generateToc;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
