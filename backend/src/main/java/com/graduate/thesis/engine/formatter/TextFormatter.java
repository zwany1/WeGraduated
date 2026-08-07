package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.entity.FormatRule;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;

/**
 * 字体格式化: 字体/字号/加粗(中西文字体分离: 中文 eastAsia, 西文 ascii/hAnsi)
 */
public final class TextFormatter {

    public static final String DEFAULT_LATIN = "Times New Roman";

    private TextFormatter() {
    }

    public static void apply(XWPFParagraph paragraph, FormatRule rule) {
        if (rule == null) {
            return;
        }
        String latin = rule.getFontLatin() != null && !rule.getFontLatin().isEmpty()
                ? rule.getFontLatin() : DEFAULT_LATIN;
        for (XWPFRun run : paragraph.getRuns()) {
            applyRun(run, rule, latin);
        }
    }

    public static void applyRun(XWPFRun run, FormatRule rule) {
        if (rule == null || run == null) {
            return;
        }
        String latin = rule.getFontLatin() != null && !rule.getFontLatin().isEmpty()
                ? rule.getFontLatin() : DEFAULT_LATIN;
        applyRun(run, rule, latin);
    }

    private static void applyRun(XWPFRun run, FormatRule rule, String latin) {
        if (rule.getFont() != null && !rule.getFont().isEmpty()) {
            setFont(run, rule.getFont(), latin);
        }
        if (rule.getFontSize() != null && rule.getFontSize() > 0) {
            // POI setFontSize 参数为 pt, 内部自行换算 half-point
            run.setFontSize(rule.getFontSize());
        }
        if (rule.getBold() != null) {
            run.setBold(rule.getBold());
        }
    }

    /**
     * 设置中西文字体: 中文(eastAsia)用 font, 西文(ascii/hAnsi/cs)用 latin
     */
    public static void setFont(XWPFRun run, String font, String latin) {
        run.setFontFamily(font);
        CTR ctr = run.getCTR();
        CTRPr rPr = ctr.isSetRPr() ? ctr.getRPr() : ctr.addNewRPr();
        CTFonts fonts = rPr.sizeOfRFontsArray() > 0 ? rPr.getRFontsArray(0) : rPr.insertNewRFonts(0);
        fonts.setEastAsia(font);
        fonts.setAscii(latin);
        fonts.setHAnsi(latin);
        fonts.setCs(latin);
    }

    /**
     * 兼容: 中文西文同字体
     */
    public static void setFont(XWPFRun run, String font) {
        setFont(run, font, DEFAULT_LATIN);
    }
}
