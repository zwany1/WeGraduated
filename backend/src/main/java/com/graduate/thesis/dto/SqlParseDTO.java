package com.graduate.thesis.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * SQL 解析请求
 */
@Data
public class SqlParseDTO {

    @NotBlank(message = "SQL 语句不能为空")
    private String sql;
}
