package com.graduate.thesis.engine.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

/**
 * 目录样式配置(toc1/toc2/toc3 三级独立配字体/字号, 行距/前导符三级共用)
 */
@Data
public class TocConfig {

    /** 一级标题条目样式 */
    private Level toc1 = defaultLevel(14);
    /** 二级标题条目样式 */
    private Level toc2 = defaultLevel(12);
    /** 三级标题条目样式 */
    private Level toc3 = defaultLevel(12);
    /** 行距倍(三级共用) */
    private float lineSpacing = 1.5f;
    /** 制表位前导符(三级共用): 空=保留原文不改, none/dot/hyphen/underscore/middleDot */
    private String leader = "dot";

    public static TocConfig parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new TocConfig();
        }
        try {
            String s = unescapeHtml(json.trim());
            TocConfig c = new ObjectMapper().readValue(s, TocConfig.class);
            if (c.toc1 == null) c.toc1 = defaultLevel(14);
            if (c.toc2 == null) c.toc2 = defaultLevel(12);
            if (c.toc3 == null) c.toc3 = defaultLevel(12);
            return c;
        } catch (Exception e) {
            return new TocConfig();
        }
    }

    /** 历史数据可能把 JSON 引号存成 HTML 实体(&quot; 等), 还原后解析 */
    private static String unescapeHtml(String s) {
        if (s == null || s.indexOf('&') < 0) {
            return s;
        }
        return s.replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    private static Level defaultLevel(int fontSize) {
        Level l = new Level();
        l.setFont("宋体");
        l.setFontLatin("Times New Roman");
        l.setFontSize(fontSize);
        return l;
    }

    /**
     * 单级目录样式(字体/字号, 各级独立)
     */
    @Data
    public static class Level {
        /** 中文字体 */
        private String font = "宋体";
        /** 西文字体 */
        private String fontLatin = "Times New Roman";
        /** 字号pt */
        private int fontSize = 12;
    }
}
