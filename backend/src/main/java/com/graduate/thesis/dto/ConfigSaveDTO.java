package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.Valid;
import java.util.List;

/**
 * 模板配置一次性保存请求: 页面/标题正则/开关/参考文献/目录样式 + 全部格式规则, 服务端单事务落库
 * 各配置字段为 null 时表示"本次不修改", 非 null 才覆盖
 */
@Data
public class ConfigSaveDTO {

    /** 页面设置 JSON 串 */
    private String pageConfig;

    /** 一级/二级/三级标题识别正则(三者任一非 null 则整体覆盖) */
    private String heading1;
    private String heading2;
    private String heading3;

    private Boolean generateToc;
    private Boolean generateAbstract;

    /** 参考文献配置 JSON 串 */
    private String referenceConfig;

    /** 目录样式配置 JSON 串 */
    private String tocConfig;

    /** 格式规则(heading1/heading2/heading3/body/figure/table/tableText), 规则内的 templateId 被忽略, 以路径为准 */
    @Valid
    private List<RuleSaveDTO> rules;
}
