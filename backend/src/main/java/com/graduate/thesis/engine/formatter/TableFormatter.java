package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.DocItem;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * 三线表格式化: 顶线/底线 1.5pt、表头下栏目线 0.75pt, 无竖线、无行间横线.
 *
 * 背景: 从 PDF/网页转换来的表格常把边框画在每个单元格上(tcBorders 每行一条 single 线),
 * 只改 tblBorders 完全看不出效果 —— 必须先清掉单元格级边框, 再由表格级边框统一表达三条线。
 */
public final class TableFormatter {

    /** 顶线/底线粗细: 1.5pt = 12 个 1/8 磅 */
    private static final BigInteger THICK = BigInteger.valueOf(12);
    /** 栏目线粗细: 0.75pt = 6 个 1/8 磅 */
    private static final BigInteger THIN = BigInteger.valueOf(6);

    private TableFormatter() {
    }

    /**
     * @param items 结构识别结果(用于跳过正文开始之前的封面/声明等前置表格)
     */
    public static void apply(XWPFDocument doc, List<DocItem> items) {
        Map<XWPFParagraph, DocItem> itemMap = new java.util.HashMap<>();
        if (items != null) {
            for (DocItem item : items) {
                if (item.getParagraph() != null) {
                    itemMap.put(item.getParagraph(), item);
                }
            }
        }
        boolean bodyStarted = false;
        for (IBodyElement el : doc.getBodyElements()) {
            if (el instanceof XWPFParagraph) {
                DocItem item = itemMap.get(el);
                if (item != null && !item.isFrontMatter() && item.getKind() != null
                        && item.getKind().name().startsWith("HEADING")) {
                    bodyStarted = true;
                }
                continue;
            }
            if (!(el instanceof XWPFTable)) {
                continue;
            }
            // 正文开始前的表格(封面花名册等)保持原样
            if (!bodyStarted) {
                continue;
            }
            applyThreeLine((XWPFTable) el);
        }
    }

    /** 把一张表改写成三线表 */
    private static void applyThreeLine(XWPFTable table) {
        rewriteTableBorders(table);
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 0; r < rows.size(); r++) {
            // 行级表格属性例外(w:tblPrEx)会覆盖表格级边框, 导致"每行仍有一条横线"; 直接去掉
            if (rows.get(r).getCtRow().isSetTblPrEx()) {
                rows.get(r).getCtRow().unsetTblPrEx();
            }
            for (XWPFTableCell cell : rows.get(r).getTableCells()) {
                // 单元格级边框是"每行一条横线"的元凶, 一律清除
                CTTcPr tcPr = cell.getCTTc().isSetTcPr()
                        ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
                if (tcPr.isSetTcBorders()) {
                    tcPr.unsetTcBorders();
                }
                // 表头行: 恢复一条 0.75pt 的栏目线(只写 bottom, 不能写 top=nil, 否则会盖掉表格顶线)
                if (r == 0) {
                    CTTcBorders tb = tcPr.addNewTcBorders();
                    bottomBorder(tb).setVal(STBorder.SINGLE);
                    bottomBorder(tb).setSz(THIN);
                    bottomBorder(tb).setSpace(BigInteger.ZERO);
                    bottomBorder(tb).setColor("auto");
                }
            }
        }
        // 单元格间距会把三线"扯断", 一并去掉
        CTTblPr tblPr = table.getCTTbl().getTblPr() != null
                ? table.getCTTbl().getTblPr() : table.getCTTbl().addNewTblPr();
        if (tblPr.isSetTblCellSpacing()) {
            tblPr.unsetTblCellSpacing();
        }
    }

    /** 表格级边框: 只留顶线、底线(1.5pt); 左右、内部横竖线全部去掉 */
    private static void rewriteTableBorders(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr() != null
                ? table.getCTTbl().getTblPr() : table.getCTTbl().addNewTblPr();
        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
        setBorder(topBorder(borders), STBorder.SINGLE, THICK);
        setBorder(leftBorder(borders), STBorder.NIL, BigInteger.ZERO);
        setBorder(bottomBorder(borders), STBorder.SINGLE, THICK);
        setBorder(rightBorder(borders), STBorder.NIL, BigInteger.ZERO);
        setBorder(insideHBorder(borders), STBorder.NIL, BigInteger.ZERO);
        setBorder(insideVBorder(borders), STBorder.NIL, BigInteger.ZERO);
    }

    private static void setBorder(CTBorder b, STBorder.Enum type, BigInteger sz) {
        b.setVal(type);
        b.setSz(sz);
        b.setSpace(BigInteger.ZERO);
        b.setColor("auto");
    }

    private static CTBorder topBorder(CTTblBorders b) {
        return b.isSetTop() ? b.getTop() : b.addNewTop();
    }

    private static CTBorder leftBorder(CTTblBorders b) {
        return b.isSetLeft() ? b.getLeft() : b.addNewLeft();
    }

    private static CTBorder bottomBorder(CTTblBorders b) {
        return b.isSetBottom() ? b.getBottom() : b.addNewBottom();
    }

    private static CTBorder rightBorder(CTTblBorders b) {
        return b.isSetRight() ? b.getRight() : b.addNewRight();
    }

    private static CTBorder insideHBorder(CTTblBorders b) {
        return b.isSetInsideH() ? b.getInsideH() : b.addNewInsideH();
    }

    private static CTBorder insideVBorder(CTTblBorders b) {
        return b.isSetInsideV() ? b.getInsideV() : b.addNewInsideV();
    }

    private static CTBorder bottomBorder(CTTcBorders b) {
        return b.isSetBottom() ? b.getBottom() : b.addNewBottom();
    }
}
