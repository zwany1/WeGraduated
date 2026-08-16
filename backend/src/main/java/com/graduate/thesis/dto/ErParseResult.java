package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ER 图 SQL 解析结果: 实体(表)列表 + 关系(外键)列表
 */
@Data
public class ErParseResult {
    private List<Map<String, Object>> entities = new ArrayList<>();
    private List<Map<String, Object>> relations = new ArrayList<>();
}
