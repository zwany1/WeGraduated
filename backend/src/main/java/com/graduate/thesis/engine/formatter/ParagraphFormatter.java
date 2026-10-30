package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.entity.FormatRule;
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff;

import java.math.BigInteger;

/**
 * 段落格式化: 对齐/行距/缩进/段前后距
 */
public final class ParagraphFormatter {

    private ParagraphFormatter() {
    }

    public static void apply(XWPFParagraph paragraph, FormatRule rule) {
        if (rule == null) {
            return;
        }
        if (rule.getAlign() != null && !rule.getAlign().isEmpty()) {
            paragraph.setAlignment(parseAlign(rule.getAlign()));
        }
        applyLineSpacing(paragraph, rule);
        // 区分「未配置(null)」与「显式 0」: 模板里显式写 0 表示不要首行缩进,
        // 此时必须把原稿遗留的 w:firstLine/w:firstLineChars 清成 0, 否则沿用原缩进(版式 C 的问题)。
        if (rule.getFirstLineIndent() != null) {
            setFirstLineIndent(paragraph, Math.max(rule.getFirstLineIndent(), 0));
        }
        if (rule.getSpaceBefore() != null) {
            paragraph.setSpacingBefore(rule.getSpaceBefore() * 20);
        }
        if (rule.getSpaceAfter() != null) {
            paragraph.setSpacingAfter(rule.getSpaceAfter() * 20);
        }
    }

    /** 行距: 固定值优先(lineRule=exact), 否则按倍数(auto) */
    public static void applyLineSpacing(XWPFParagraph paragraph, FormatRule rule) {
        if (rule == null) {
            return;
        }
        if ("exact".equals(rule.getLineSpacingType())
                && rule.getLineSpacingExact() != null && rule.getLineSpacingExact() > 0) {
            // 固定值行距: w:line 单位 1/20 磅, lineRule=exact
            setFixedLineSpacing(paragraph, rule.getLineSpacingExact());
            return;
        }
        if (rule.getLineSpacing() != null && rule.getLineSpacing() > 0) {
            paragraph.setSpacingBetween(rule.getLineSpacing(), LineSpacingRule.AUTO);
        }
    }

    /**
     * 固定值行距(磅), lineRule=exact
     */
    private static void setFixedLineSpacing(XWPFParagraph paragraph, int points) {
        CTPPr pPr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        CTSpacing spacing = pPr.isSetSpacing() ? pPr.getSpacing() : pPr.addNewSpacing();
        spacing.setLine(BigInteger.valueOf(points * 20L));
        spacing.setLineRule(STLineSpacingRule.EXACT);
    }

    /**
     * 段落前分页(另起一页)
     */
    public static void setPageBreakBefore(XWPFParagraph paragraph) {
        setPageBreakBefore(paragraph, true);
    }

    /**
     * 段落前分页开关: 写入显式的 w:val(1/0), 而不是无脑 addNewPageBreakBefore()。
     * 目的有两个:
     *   1) 同一段落被多个 formatter 处理时不会叠加出重复的 pageBreakBefore 元素;
     *   2) 关掉分页时必须写 val="0", 否则会回落到样式定义里的分页设置。
     * w:val 用 DOM 属性写入: STOnOff 在 ooxml-lite 里是 union 简单类型, 直接 setVal 无法表达 0/1。
     */
    public static void setPageBreakBefore(XWPFParagraph paragraph, boolean on) {
        CTPPr pPr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : null;
        if (pPr == null) {
            if (!on) {
                return; // 没有 pPr 且要求"不分页": 什么都不用做
            }
            pPr = paragraph.getCTP().addNewPPr();
        }
        if (!pPr.isSetPageBreakBefore() && !on) {
            return; // 原本就没有分页设置, 无需写一个 val=0 的空元素
        }
        CTOnOff pbb = pPr.isSetPageBreakBefore() ? pPr.getPageBreakBefore() : pPr.addNewPageBreakBefore();
        org.w3c.dom.Node node = pbb.getDomNode();
        if (node instanceof org.w3c.dom.Element) {
            org.w3c.dom.Element el = (org.w3c.dom.Element) node;
            if (on) {
                el.removeAttributeNS(W_NS, "val");
            } else {
                el.setAttributeNS(W_NS, "w:val", "0");
            }
        }
    }

    /** OOXML 主命名空间 */
    private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    /**
     * 段落大纲级别(0=一级,1=二级,2=三级): 供 Word 自动目录据此收录条目; 已设则保留原值
     */
    public static void setOutlineLevel(XWPFParagraph paragraph, int level) {
        CTPPr pPr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        if (pPr.isSetOutlineLvl()) {
            return;
        }
        pPr.addNewOutlineLvl().setVal(BigInteger.valueOf(level));
    }

    private static ParagraphAlignment parseAlign(String align) {
        if ("center".equals(align)) {
            return ParagraphAlignment.CENTER;
        }
        if ("right".equals(align)) {
            return ParagraphAlignment.RIGHT;
        }
        if ("left".equals(align)) {
            return ParagraphAlignment.LEFT;
        }
        return ParagraphAlignment.BOTH;
    }

    /**
     * 首行缩进: 按字符数设置 w:firstLineChars 与 w:firstLine;
     * chars=0 时两者都写 0, 用于显式取消原稿遗留的缩进。
     */
    private static void setFirstLineIndent(XWPFParagraph paragraph, int chars) {
        CTPPr pPr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        CTInd ind = pPr.isSetInd() ? pPr.getInd() : pPr.addNewInd();
        ind.setFirstLineChars(BigInteger.valueOf(chars * 100L));
        ind.setFirstLine(BigInteger.valueOf(chars * 240L));
    }
}
