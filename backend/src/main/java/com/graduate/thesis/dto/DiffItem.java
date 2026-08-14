package com.graduate.thesis.dto;

import lombok.Data;

/**
 * 排版差异项: 一段被排版引擎修改过的文本及其在结果 PDF 中的位置
 */
@Data
public class DiffItem {
    /** 段落文本摘要 */
    private String text;
    /** 差异类型(中文, 逗号分隔): 字号/字体/加粗/行距/缩进/对齐/段前距/段后距 */
    private String type;
    /** 在排版后 PDF 中的页码(1 起) */
    private int page;
    /** 页面内纵向位置(0-1, 归一化) */
    private double y;
    /** 高亮区域高度(0-1, 归一化) */
    private double h;

    public DiffItem() {
    }

    public DiffItem(String text, String type, int page, double y, double h) {
        this.text = text;
        this.type = type;
        this.page = page;
        this.y = y;
        this.h = h;
    }
}
