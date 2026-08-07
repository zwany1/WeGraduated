package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.Table3DTO;
import com.graduate.thesis.engine.formatter.TextFormatter;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * 三线表生成: 顶线/底线为粗线(1.5pt), 表头下线为细线(0.75pt), 无竖线
 */
@Service
public class Table3Service {

    /** A4 内容宽度(twips): 21cm - 左右边距各3cm = 15cm ≈ 8500 */
    private static final long CONTENT_WIDTH = 8500L;
    private static final int BORDER_BOLD = 12;   // 1.5pt
    private static final int BORDER_THIN = 6;    // 0.75pt
    private static final String CN_FONT = "宋体";

    public byte[] generate(Table3DTO dto) {
        if (dto.getHeaders() == null || dto.getHeaders().isEmpty()) {
            throw new BusinessException(400, "表头不能为空");
        }
        int cols = dto.getHeaders().size();
        int rows = dto.getRows() == null ? 0 : dto.getRows().size();
        if (rows == 0) {
            throw new BusinessException(400, "请至少填写一行数据");
        }

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            addCaption(doc, dto);
            XWPFTable table = doc.createTable(rows + 1, cols);
            applyBorders(table);
            fillRow(table.getRow(0), dto.getHeaders(), true, dto);
            for (int i = 0; i < rows; i++) {
                fillRow(table.getRow(i + 1), dto.getRows().get(i), false, dto);
            }
            applyColumnWidth(table, cols);
            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "三线表生成失败: " + e.getMessage());
        }
    }

    /**
     * 表格边框: 顶线/底线粗线, 其余无边框; 表头行下边框单独加细线
     */
    private void applyBorders(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }
        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();

        borders.addNewTop().setVal(STBorder.SINGLE);
        borders.getTop().setSz(BigInteger.valueOf(BORDER_BOLD));
        borders.getTop().setColor("000000");

        borders.addNewBottom().setVal(STBorder.SINGLE);
        borders.getBottom().setSz(BigInteger.valueOf(BORDER_BOLD));
        borders.getBottom().setColor("000000");

        borders.addNewLeft().setVal(STBorder.NONE);
        borders.addNewRight().setVal(STBorder.NONE);
        borders.addNewInsideH().setVal(STBorder.NONE);
        borders.addNewInsideV().setVal(STBorder.NONE);

        if (table.getNumberOfRows() > 0) {
            for (XWPFTableCell cell : table.getRow(0).getTableCells()) {
                CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
                CTTcBorders tcBorders = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders();
                CTBorder bottom = tcBorders.isSetBottom() ? tcBorders.getBottom() : tcBorders.addNewBottom();
                bottom.setVal(STBorder.SINGLE);
                bottom.setSz(BigInteger.valueOf(BORDER_THIN));
                bottom.setColor("000000");
            }
        }
    }

    /**
     * 填充一行: 表头加粗, 默认垂直居中
     */
    private void fillRow(XWPFTableRow row, List<String> values, boolean header, Table3DTO dto) {
        int size = dto.getFontSize() != null && dto.getFontSize() > 0 ? dto.getFontSize() : 10;
        ParagraphAlignment align = "left".equalsIgnoreCase(dto.getAlign())
                ? ParagraphAlignment.LEFT : ParagraphAlignment.CENTER;
        for (int i = 0; i < values.size(); i++) {
            XWPFTableCell cell = row.getCell(i);
            if (cell == null) {
                continue;
            }
            String v = values.get(i) == null ? "" : values.get(i).trim();
            cell.setText(v);
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            for (XWPFParagraph p : cell.getParagraphs()) {
                p.setAlignment(align);
                p.setSpacingAfter(0);
                p.setSpacingBefore(0);
                for (XWPFRun run : p.getRuns()) {
                    run.setFontSize(size);
                    run.setBold(header);
                    TextFormatter.setFont(run, CN_FONT);
                }
            }
        }
    }

    /**
     * 均分列宽
     */
    private void applyColumnWidth(XWPFTable table, int cols) {
        long colW = CONTENT_WIDTH / Math.max(cols, 1);
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
                CTTblWidth tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
                tcW.setType(STTblWidth.DXA);
                tcW.setW(BigInteger.valueOf(colW));
            }
        }
    }

    /**
     * 表题段落: 宋体五号(10pt)加粗居中
     */
    private void addCaption(XWPFDocument doc, Table3DTO dto) {
        String caption = buildCaption(dto);
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBefore(120);
        p.setSpacingAfter(120);
        XWPFRun run = p.createRun();
        run.setText(caption);
        run.setBold(true);
        run.setFontSize(10);
        TextFormatter.setFont(run, CN_FONT);
    }

    private String buildCaption(Table3DTO dto) {
        if (dto.getCaption() != null && !dto.getCaption().trim().isEmpty()) {
            return dto.getCaption().trim();
        }
        StringBuilder sb = new StringBuilder();
        if (dto.getAutoNumber() == null || dto.getAutoNumber()) {
            int ch = dto.getChapterNo() != null ? dto.getChapterNo() : 1;
            int no = dto.getTableNo() != null ? dto.getTableNo() : 1;
            sb.append("表").append(ch).append('-').append(no).append(' ');
        }
        if (dto.getTitle() != null) {
            sb.append(dto.getTitle().trim());
        }
        return sb.toString();
    }
}
