package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.model.PageConfig;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;

import java.util.ArrayList;
import java.util.List;

/**
 * 页眉页脚: 页脚页码(按位置/字体/格式) + 页眉内容.
 * 支持: 首页不显示页码(封面去页码)、页码字体字号可配、罗马数字/阿拉伯数字页码.
 */
public final class HeaderFooterFormatter {

    private HeaderFooterFormatter() {
    }

    public static void apply(XWPFDocument doc, PageConfig page) {
        PageConfig.Footer footer = page.getFooter();
        String type = footer.getPageNumber();
        if (type == null || "none".equals(type)) {
            return;
        }

        // 首页不同: 开启 titlePg, 首页页脚留空(封面/目录前置部分无页码)
        if (footer.isSkipFirst()) {
            try {
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr sectPr =
                        doc.getDocument().getBody().isSetSectPr()
                                ? doc.getDocument().getBody().getSectPr()
                                : doc.getDocument().getBody().addNewSectPr();
                if (!sectPr.isSetTitlePg()) {
                    sectPr.addNewTitlePg();
                }
            } catch (Exception ignore) {
            }
            XWPFFooter first = doc.createFooter(HeaderFooterType.FIRST);
            clearParagraphs(first);
            first.createParagraph();
        }

        XWPFFooter footerP = doc.createFooter(HeaderFooterType.DEFAULT);
        clearParagraphs(footerP);
        XWPFParagraph p = footerP.createParagraph();
        setAlign(p, type);
        addPageNumberField(p, footer.getFormat());
        applyPageNumberFont(p, footer);

        if (page.getHeader().getText() != null && !page.getHeader().getText().trim().isEmpty()) {
            XWPFHeader header = doc.createHeader(HeaderFooterType.DEFAULT);
            clearParagraphs(header);
            XWPFParagraph hp = header.createParagraph();
            hp.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = hp.createRun();
            run.setText(page.getHeader().getText());
        }
    }

    private static void setAlign(XWPFParagraph p, String type) {
        if ("center".equals(type)) {
            p.setAlignment(ParagraphAlignment.CENTER);
        } else if ("right".equals(type)) {
            p.setAlignment(ParagraphAlignment.RIGHT);
        } else {
            p.setAlignment(ParagraphAlignment.LEFT);
        }
    }

    private static void addPageNumberField(XWPFParagraph paragraph, String format) {
        XWPFRun run = paragraph.createRun();
        CTR ctr = run.getCTR();
        CTFldChar begin = ctr.addNewFldChar();
        begin.setFldCharType(STFldCharType.BEGIN);
        CTText instr = ctr.addNewInstrText();
        // 罗马数字: PAGE \* roman(小写), 阿拉伯: PAGE
        String instruction = "roman".equalsIgnoreCase(format) ? " PAGE \\* roman " : " PAGE ";
        instr.setStringValue(instruction);
        CTFldChar separate = ctr.addNewFldChar();
        separate.setFldCharType(STFldCharType.SEPARATE);
        ctr.addNewT().setStringValue("roman".equalsIgnoreCase(format) ? "i" : "1");
        CTFldChar end = ctr.addNewFldChar();
        end.setFldCharType(STFldCharType.END);
    }

    /**
     * 页脚页码字体(默认宋体五号, 可配)
     */
    private static void applyPageNumberFont(XWPFParagraph paragraph, PageConfig.Footer footer) {
        String font = footer.getFont() == null || footer.getFont().isEmpty() ? "宋体" : footer.getFont();
        double size = footer.getFontSize() > 0 ? footer.getFontSize() : 10.5;
        for (XWPFRun run : paragraph.getRuns()) {
            TextFormatter.setFont(run, font);
            run.setFontSize(size);
        }
    }

    private static void clearParagraphs(XWPFHeaderFooter headerFooter) {
        List<XWPFParagraph> paragraphs = new ArrayList<>(headerFooter.getParagraphs());
        for (XWPFParagraph paragraph : paragraphs) {
            headerFooter.removeParagraph(paragraph);
        }
    }
}