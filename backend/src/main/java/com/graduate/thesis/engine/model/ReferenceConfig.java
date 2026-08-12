package com.graduate.thesis.engine.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

/**
 * 参考文献配置
 */
@Data
public class ReferenceConfig {

    /** 是否启用参考文献排版(默认 false) */
    private boolean enabled = false;

    /** 标题文字 */
    private String title = "参考文献";

    /** 标题字体(默认 黑体) */
    private String titleFont = "黑体";

    /** 标题字号pt(默认 四号14) */
    private int titleFontSize = 14;

    /** 条目中文字体(默认 宋体) */
    private String itemFont = "宋体";

    /** 条目西文字体(默认 Times New Roman) */
    private String itemFontLatin = "Times New Roman";

    /** 条目字号pt(默认 五号10.5) */
    private int itemFontSize = 10;

    /** 是否删掉 DOI(默认 true) */
    private boolean removeDoi = true;

    /** 作者最多保留人数(默认3, 超出加"等"/"et al") */
    private int maxAuthors = 3;

    /** 条目序号是否重排(默认 true) */
    private boolean renumber = true;

    public static ReferenceConfig parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ReferenceConfig();
        }
        try {
            return new ObjectMapper().readValue(json, ReferenceConfig.class);
        } catch (Exception e) {
            return new ReferenceConfig();
        }
    }
}
