package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.model.PageConfig;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;

/**
 * 页面设置: 纸张类型/页边距/页眉页脚距离。
 *
 * 文档可以有多节(封面、摘要、目录、横向大表、正文…), 每节都有自己的 sectPr。
 * 原先只改最后一节, 一旦其它节的 sectPr 被保留下来, 就会出现"A4 与 B5 混排";
 * 因此这里对全部 sectPr 统一套用纸张与页边距, 并保留原有的横向/纵向方向。
 */
public final class PageFormatter {

    private PageFormatter() {
    }

    public static void apply(XWPFDocument doc, PageConfig page) {
        applyTo(getSectPr(doc), page);
        // 段落级分节符(节与节之间的分界)
        for (XWPFParagraph p : doc.getParagraphs()) {
            try {
                CTPPr pPr = p.getCTP().getPPr();
                if (pPr != null && pPr.isSetSectPr()) {
                    applyTo(pPr.getSectPr(), page);
                }
            } catch (Exception ignore) {
                // 个别段落结构异常不影响整体排版
            }
        }
    }

    private static void applyTo(CTSectPr sectPr, PageConfig page) {
        if (sectPr == null) {
            return;
        }
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
        long[] size = paperSize(paper);
        if (size == null) {
            return;
        }
        CTPageSz pageSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        boolean landscape = pageSz.isSetOrient() && pageSz.getOrient() == STPageOrientation.LANDSCAPE;
        if (landscape) {
            pageSz.setW(size[1]);
            pageSz.setH(size[0]);
        } else {
            pageSz.setW(size[0]);
            pageSz.setH(size[1]);
        }
    }

    /** 纸张尺寸(twips): [宽, 高], 未知纸张返回 null(保持原样) */
    private static long[] paperSize(String paper) {
        if (paper == null) {
            return null;
        }
        if ("A4".equalsIgnoreCase(paper)) {
            return new long[]{11906L, 16838L};
        }
        if ("A3".equalsIgnoreCase(paper)) {
            return new long[]{16838L, 23811L};
        }
        if ("A5".equalsIgnoreCase(paper)) {
            return new long[]{8391L, 11906L};
        }
        if ("B5".equalsIgnoreCase(paper)) {
            // ISO B5 = 176×250mm。原先写的 8293×11810(≈146×208mm) 其实是 A5 一档的成品尺寸,
            // 选"B5"却排出接近 A5 的页面, 打印装订规格会错
            return new long[]{9978L, 14173L};
        }
        if ("Letter".equalsIgnoreCase(paper)) {
            return new long[]{12240L, 15840L};
        }
        return null;
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
