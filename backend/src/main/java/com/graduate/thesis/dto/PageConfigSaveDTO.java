package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 页面设置保存请求
 */
@Data
public class PageConfigSaveDTO {

    @NotBlank(message = "页面配置不能为空")
    private String pageConfig;
}
