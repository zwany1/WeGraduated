package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.model.PageConfig;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

/**
 * 页面设置: 纸张类型/页边距/页眉页脚距离
 */
public final class PageFormatter {

    private PageFormatter() {
    }

    public static void apply(XWPFDocument doc, PageConfig page) {
        CTSectPr sectPr = getSectPr(doc);
        applyPaper(sectPr, page.getPaper());
        applyMargin(sectPr, page);
    }

    private static CTSectPr getSectPr(XWPFDocument doc) {
        CTBody body = doc.getDocument().getBody();
        if (body.isSetSectPr()) {
            return body.getSectPr();
        }
        return body.addNewSectPr();
    }

    private static void applyPaper(CTSectPr sectPr, String paper) {
        CTPageSz pageSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        if ("A4".equalsIgnoreCase(paper)) {
            pageSz.setW(11906L);
            pageSz.setH(16838L);
        } else if ("A3".equalsIgnoreCase(paper)) {
            pageSz.setW(16838L);
            pageSz.setH(23811L);
        } else if ("A5".equalsIgnoreCase(paper)) {
            pageSz.setW(8391L);
            pageSz.setH(11906L);
        } else if ("B5".equalsIgnoreCase(paper)) {
            pageSz.setW(8293L);
            pageSz.setH(11810L);
        } else if ("Letter".equalsIgnoreCase(paper)) {
            pageSz.setW(12240L);
            pageSz.setH(15840L);
        }
    }

    private static void applyMargin(CTSectPr sectPr, PageConfig page) {
        CTPageMar mar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        PageConfig.Margin margin = page.getMargin();
        mar.setTop(PageConfig.cmToTwips(margin.getTop()));
        mar.setBottom(PageConfig.cmToTwips(margin.getBottom()));
        mar.setLeft(PageConfig.cmToTwips(margin.getLeft()));
        mar.setRight(PageConfig.cmToTwips(margin.getRight()));
        mar.setHeader(PageConfig.cmToTwips(page.getHeader().getHeight()));
        mar.setFooter(PageConfig.cmToTwips(1.5));
    }
}
