package com.graduate.thesis.engine.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 校规文档启发式抽取结果:
 * config 为已识别出的模板配置(仅含命中的字段, 键结构与 ConfigSaveDTO 对应),
 * evidence 为每个命中字段的原文摘录, 供前端向导展示"依据"
 */
@Data
public class SpecExtractVO {

    /** 页面设置(纸张/边距, 键与前端 page 对象一致) */
    private Map<String, Object> pageConfig = new LinkedHashMap<>();

    /** 标题正则(heading1/heading2/heading3) */
    private Map<String, String> headingPatterns = new LinkedHashMap<>();

    /** 格式规则(键 ruleType, 值为该规则命中的字段) */
    private Map<String, Map<String, Object>> rules = new LinkedHashMap<>();

    /** 参考文献配置(命中字段) */
    private Map<String, Object> refConfig = new LinkedHashMap<>();

    /** 目录配置(命中字段) */
    private Map<String, Object> tocConfig = new LinkedHashMap<>();

    /** 命中依据: 字段路径 → 值/原文摘录/置信度 */
    private List<Evidence> evidence = new ArrayList<>();

    /** 识别到的段落总数(供前端判断文档是否可解析) */
    private int paragraphCount;

    @Data
    public static class Evidence {
        /** 字段路径, 如 body.font / page.marginTop / heading1.fontSize */
        private String field;
        /** 抽取出的值 */
        private String value;
        /** 原文摘录 */
        private String quote;
        /** 置信度: high=语句明确, low=根据惯例推断 */
        private String confidence;

        public Evidence() {
        }

        public Evidence(String field, String value, String quote, String confidence) {
            this.field = field;
            this.value = value;
            this.quote = quote;
            this.confidence = confidence;
        }
    }
}
