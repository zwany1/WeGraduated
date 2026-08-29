package com.graduate.thesis.engine.model;

import lombok.Data;

/**
 * 页面配置 (对应 template.page_config JSON)
 * <pre>
 * {
 *   "paper": "A4",
 *   "margin": {"top":2.5,"bottom":2.5,"left":3,"right":2.5},
 *   "header": {"height":1.5,"text":""},
 *   "footer": {"pageNumber":"center"}
 * }
 * </pre>
 */
@Data
public class PageConfig {

    private String paper = "A4";

    private Margin margin = new Margin();

    private Header header = new Header();

    private Footer footer = new Footer();

    @Data
    public static class Margin {
        private double top = 2.5;
        private double bottom = 2.5;
        private double left = 3;
        private double right = 2.5;
    }

    @Data
    public static class Header {
        private double height = 1.5;
        private String text = "";
    }

    @Data
    public static class Footer {
        /** none / left / center / right */
        private String pageNumber = "center";
        /** 页码字体(默认 宋体) */
        private String font = "宋体";
        /** 页码字号pt(默认 五号10.5) */
        private double fontSize = 10.5;
        /** 首页是否不显示页码(封面无页码), 启用 w:titlePg + 空首页页脚 */
        private boolean skipFirst = false;
        /** 页码数字格式: decimal(默认) / roman(小写罗马数字, 前置部分常用) */
        private String format = "decimal";
    }

    public static double cmToTwips(double cm) {
        return Math.round(cm * 567.0);
    }

    /** 纸张宽度(twips), 未识别的纸张按 A4 */
    public long pageWidthTwips() {
        switch (paper == null ? "" : paper.toUpperCase()) {
            case "A3": return 16838L;
            case "A5": return 8391L;
            case "B5": return 8293L;
            case "LETTER": return 12240L;
            case "A4":
            default: return 11906L;
        }
    }

    /** 文本区宽度 = 页宽 - 左右页边距(twips), 用于目录制表位等按版心定位 */
    public long textWidthTwips() {
        return pageWidthTwips() - (long) cmToTwips(margin.getLeft()) - (long) cmToTwips(margin.getRight());
    }
}
