package com.graduate.thesis.dto;

import lombok.Data;

/**
 * 排版差异项: 一段被排版引擎修改过的文本及其变更描述
 */
@Data
public class DiffItem {
    /** 段落文本摘要 */
    private String text;
    /** 主差异类型(中文): 字号/字体/加粗/行距/缩进/对齐/段前距/段后距 */
    private String type;
    /** 变更描述: "字号 12→14; 行距 1.5→1.25" */
    private String change;
    /** 文档中第几处差异(从 1 起) */
    private int index;
    /** 在排版后 PDF 中的页码(1 起, 无缓存 PDF 时为 0) */
    private int page;
    /** 页面内纵向位置(0-1, 归一化) */
    private double y;
    /** 高亮区域高度(0-1, 归一化) */
    private double h;

    public DiffItem() {
    }

    public DiffItem(String text, String type, String change, int index, int page, double y, double h) {
        this.text = text;
        this.type = type;
        this.change = change;
        this.index = index;
        this.page = page;
        this.y = y;
        this.h = h;
    }
}
