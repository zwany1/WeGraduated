package com.graduate.thesis.dto;

import lombok.Data;

/**
 * SQL 解析出的列信息
 */
@Data
public class TableColumnInfo {

    private String name;
    private String type;
    private boolean nullable = true;
    private String defaultValue;
    private String comment;

    public TableColumnInfo() {
    }

    public TableColumnInfo(String name, String type, boolean nullable, String defaultValue, String comment) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.comment = comment;
    }
}
