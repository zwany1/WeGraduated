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
    }

    public static double cmToTwips(double cm) {
        return Math.round(cm * 567.0);
    }
}
