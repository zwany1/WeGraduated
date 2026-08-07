package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 开始排版请求
 */
@Data
public class PaperFormatDTO {

    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    @NotNull(message = "模板ID不能为空")
    private Long templateId;
}
