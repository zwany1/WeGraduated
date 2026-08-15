package com.graduate.thesis.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Excel 导出服务(基于 Apache POI)
 */
@Service
public class ExcelExportService {

    public byte[] export(String sheetName, String[] headers, List<String[]> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            Row headRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }
            int r = 1;
            if (rows != null) {
                for (String[] rowData : rows) {
                    Row row = sheet.createRow(r++);
                    for (int c = 0; c < rowData.length; c++) {
                        row.createCell(c).setCellValue(rowData[c] == null ? "" : rowData[c]);
                    }
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new com.graduate.thesis.common.BusinessException(500, "导出失败: " + e.getMessage());
        }
    }
}
