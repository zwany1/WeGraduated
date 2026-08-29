package com.graduate.thesis.engine.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

/**
 * 摘要/关键词页面配置(原硬编码按桂林信息科技学院规范, 现可由模板 page_config JSON 的 "abstract" 字段覆盖)
 * <pre>
 * 示例 page_config:
 * {
 *   "paper":"A4", ...,
 *   "abstract": {
 *     "zhTitleFont":"黑体", "zhTitleFontSize":16, "zhTitleBold":true, "zhTitleText":"摘  要",
 *     "zhBodyFont":"宋体", "zhBodyFontSize":12,
 *     "keywordLabelFont":"黑体", "keywordBodyFont":"宋体", "keywordFontSize":12,
 *     "enTitleFont":"Times New Roman", "enTitleFontSize":16, "enTitleBold":true,
 *     "enBodyFont":"Times New Roman", "enBodyFontSize":12,
 *     "enKeywordLabelFont":"Times New Roman", "enKeywordBodyFont":"Times New Roman", "enKeywordFontSize":12,
 *     "titlePageBreak":true
 *   }
 * }
 * </pre>
 */
@Data
public class AbstractConfig {

    /** 中文摘要标题字体 */
    private String zhTitleFont = "黑体";
    /** 中文摘要标题字号pt(三号16) */
    private int zhTitleFontSize = 16;
    /** 中文摘要标题加粗 */
    private boolean zhTitleBold = true;
    /** 中文摘要标题文字(含字距) */
    private String zhTitleText = "摘  要";

    /** 中文摘要正文字体 */
    private String zhBodyFont = "宋体";
    /** 中文摘要正文字号pt(小四12) */
    private int zhBodyFontSize = 12;

    /** 关键词标签字体 */
    private String keywordLabelFont = "黑体";
    /** 关键词内容字体 */
    private String keywordBodyFont = "宋体";
    /** 关键词字号pt */
    private int keywordFontSize = 12;

    /** 英文摘要标题字体 */
    private String enTitleFont = "Times New Roman";
    /** 英文摘要标题字号pt(三号16) */
    private int enTitleFontSize = 16;
    /** 英文摘要标题加粗 */
    private boolean enTitleBold = true;

    /** 英文摘要正文字体 */
    private String enBodyFont = "Times New Roman";
    /** 英文摘要正文字号pt */
    private int enBodyFontSize = 12;

    /** Key words 标签字体 */
    private String enKeywordLabelFont = "Times New Roman";
    /** Key words 内容字体 */
    private String enKeywordBodyFont = "Times New Roman";
    /** Key words 字号pt */
    private int enKeywordFontSize = 12;

    /** 摘要/Abstract 标题是否另起一页 */
    private boolean titlePageBreak = true;

    /**
     * 从 page_config JSON 的 "abstract" 字段解析; 缺失时返回默认(原学校规范)
     */
    public static AbstractConfig parse(String pageConfigJson) {
        AbstractConfig cfg = new AbstractConfig();
        if (pageConfigJson == null || pageConfigJson.trim().isEmpty()) {
            return cfg;
        }
        try {
            JsonNode root = new ObjectMapper().readTree(pageConfigJson);
            JsonNode node = root.get("abstract");
            if (node == null || node.isNull() || node.isMissingNode()) {
                return cfg;
            }
            return new ObjectMapper().readerForUpdating(cfg).readValue(node);
        } catch (Exception ignore) {
            return cfg;
        }
    }
}