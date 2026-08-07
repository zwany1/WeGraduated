package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.entity.FormatRule;
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

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
        if (rule.getFirstLineIndent() != null && rule.getFirstLineIndent() > 0) {
            setFirstLineIndent(paragraph, rule.getFirstLineIndent());
        }
        if (rule.getSpaceBefore() != null) {
            paragraph.setSpacingBefore(rule.getSpaceBefore() * 20);
        }
        if (rule.getSpaceAfter() != null) {
            paragraph.setSpacingAfter(rule.getSpaceAfter() * 20);
        }
    }

    private static void applyLineSpacing(XWPFParagraph paragraph, FormatRule rule) {
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
        CTPPr pPr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        pPr.addNewPageBreakBefore();
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
     * 首行缩进: 按字符数设置 w:firstLineChars
     */
    private static void setFirstLineIndent(XWPFParagraph paragraph, int chars) {
        CTPPr pPr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        CTInd ind = pPr.isSetInd() ? pPr.getInd() : pPr.addNewInd();
        ind.setFirstLineChars(BigInteger.valueOf(chars * 100L));
        ind.setFirstLine(BigInteger.valueOf(chars * 240L));
    }
}
