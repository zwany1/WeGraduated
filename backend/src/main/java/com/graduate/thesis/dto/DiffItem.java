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
    /**
     * 差异所在位置, 形如「正文第 208 段」/「表 2」。
     * 只靠 text 摘要无法区分同名段落(7 个章都有"本章小结"、多个表格都有"10"),
     * 没有位置信息时用户会以为索引里出现了重复条目。
     */
    private String where;
    /** 文档中第几处差异(从 1 起) */
    private int index;

    public DiffItem() {
    }

    public DiffItem(String text, String type, String change, int index) {
        this.text = text;
        this.type = type;
        this.change = change;
        this.index = index;
    }
}
