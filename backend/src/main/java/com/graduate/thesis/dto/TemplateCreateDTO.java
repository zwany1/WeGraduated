package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建格式模板请求
 */
@Data
public class TemplateCreateDTO {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 64, message = "模板名称过长")
    private String name;
}
