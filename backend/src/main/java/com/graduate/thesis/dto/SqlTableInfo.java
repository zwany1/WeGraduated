package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 解析结果: 表名 + 列清单
 */
@Data
public class SqlTableInfo {

    private String tableName;
    private List<TableColumnInfo> columns = new ArrayList<>();

    public SqlTableInfo() {
    }

    public SqlTableInfo(String tableName, List<TableColumnInfo> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }
}
