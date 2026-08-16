package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量排版请求
 */
@Data
public class PaperFormatBatchDTO {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotEmpty(message = "请选择要排版的论文")
    private List<Long> fileIds;
}
