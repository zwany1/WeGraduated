package com.graduate.thesis.engine.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

/**
 * 固定章节页(谢辞/参考文献/附录)配置(原硬编码按桂林信息科技学院规范, 现可由模板 page_config JSON 的 "section" 字段覆盖)
 * <pre>
 * 示例 page_config:
 * {
 *   "paper":"A4", ...,
 *   "section": {
 *     "font":"黑体", "fontSize":14, "bold":true, "pageBreakBefore":true, "outlineLevel":0,
 *     "xieCi":"谢  辞", "references":"参考文献", "appendix":"附  录"
 *   }
 * }
 * </pre>
 */
@Data
public class SectionConfig {

    /** 章节标题字体(默认 黑体) */
    private String font = "黑体";
    /** 章节标题字号pt(四号14) */
    private int fontSize = 14;
    /** 是否加粗 */
    private boolean bold = true;
    /** 是否另起一页 */
    private boolean pageBreakBefore = true;
    /** 大纲级别(0=一级) */
    private int outlineLevel = 0;

    /** 谢辞标题文字 */
    private String xieCi = "谢  辞";
    /** 参考文献标题文字 */
    private String references = "参考文献";
    /** 附录标题文字 */
    private String appendix = "附  录";

    /** 谢辞对齐: center/left */
    private String xieCiAlign = "center";
    /** 参考文献对齐 */
    private String referencesAlign = "left";
    /** 附录对齐 */
    private String appendixAlign = "center";

    /**
     * 从 page_config JSON 的 "section" 字段解析; 缺失时返回默认(原学校规范)
     */
    public static SectionConfig parse(String pageConfigJson) {
        SectionConfig cfg = new SectionConfig();
        if (pageConfigJson == null || pageConfigJson.trim().isEmpty()) {
            return cfg;
        }
        try {
            JsonNode root = new ObjectMapper().readTree(pageConfigJson);
            JsonNode node = root.get("section");
            if (node == null || node.isNull() || node.isMissingNode()) {
                return cfg;
            }
            return new ObjectMapper().readerForUpdating(cfg).readValue(node);
        } catch (Exception ignore) {
            return cfg;
        }
    }
}