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
 * 页眉页脚: 页脚页码(底部居中) + 页眉内容
 */
public final class HeaderFooterFormatter {

    private HeaderFooterFormatter() {
    }

    public static void apply(XWPFDocument doc, PageConfig page) {
        String type = page.getFooter().getPageNumber();
        if (type == null || "none".equals(type)) {
            return;
        }
        XWPFFooter footer = doc.createFooter(HeaderFooterType.DEFAULT);
        clearParagraphs(footer);
        XWPFParagraph p = footer.createParagraph();
        if ("center".equals(type)) {
            p.setAlignment(ParagraphAlignment.CENTER);
        } else if ("right".equals(type)) {
            p.setAlignment(ParagraphAlignment.RIGHT);
        } else {
            p.setAlignment(ParagraphAlignment.LEFT);
        }
        addPageNumberField(p);
        applyPageNumberFont(p);

        if (page.getHeader().getText() != null && !page.getHeader().getText().trim().isEmpty()) {
            XWPFHeader header = doc.createHeader(HeaderFooterType.DEFAULT);
            clearParagraphs(header);
            XWPFParagraph hp = header.createParagraph();
            hp.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = hp.createRun();
            run.setText(page.getHeader().getText());
        }
    }

    private static void addPageNumberField(XWPFParagraph paragraph) {
        XWPFRun run = paragraph.createRun();
        CTR ctr = run.getCTR();
        CTFldChar begin = ctr.addNewFldChar();
        begin.setFldCharType(STFldCharType.BEGIN);
        CTText instr = ctr.addNewInstrText();
        instr.setStringValue(" PAGE ");
        CTFldChar separate = ctr.addNewFldChar();
        separate.setFldCharType(STFldCharType.SEPARATE);
        ctr.addNewT().setStringValue("1");
        CTFldChar end = ctr.addNewFldChar();
        end.setFldCharType(STFldCharType.END);
    }

    /**
     * 页脚页码默认宋体五号
     */
    private static void applyPageNumberFont(XWPFParagraph paragraph) {
        for (XWPFRun run : paragraph.getRuns()) {
            TextFormatter.setFont(run, "宋体");
            run.setFontSize(10.5);
        }
    }

    private static void clearParagraphs(XWPFHeaderFooter headerFooter) {
        List<XWPFParagraph> paragraphs = new ArrayList<>(headerFooter.getParagraphs());
        for (XWPFParagraph paragraph : paragraphs) {
            headerFooter.removeParagraph(paragraph);
        }
    }
}
