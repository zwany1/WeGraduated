package com.graduate.thesis.engine;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 临时调试: 对比 166 原文 vs 排版后, 按段落顺序打印参考文献/附录/表格题注格式前后.
 */
class StructureDebugTest {

    @Test
    void dumpStructure() throws Exception {
        ZipSecureFile.setMinInflateRatio(0.001);
        File original = new File("d:/project/Graduated/backend/data/storage/upload/20260821/ceb1ec13dfe5476bbf04280d2edecc5b.docx");
        File formatted = new File("d:/project/Graduated/backend/data/storage/result/20260821/result_17_4dd69b45bf0e.docx");
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                Files.newOutputStream(Paths.get("target/diff-out.txt")), StandardCharsets.UTF_8));
             XWPFDocument o = new XWPFDocument(new FileInputStream(original));
             XWPFDocument f = new XWPFDocument(new FileInputStream(formatted))) {
            out.println("==== 原文 参考文献附录表格题注 ====");
            int i = 0;
            for (XWPFParagraph p : o.getParagraphs()) {
                String t = p.getText() == null ? "" : p.getText().trim();
                if (t.contains("参考文献") || t.startsWith("附录") || t.startsWith("表4-1") || t.startsWith("表7-1") || t.equals("附录")) {
                    out.printf("[o %3d] firstLine=%d | %s | %s%n", i, p.getIndentationFirstLine(), fp(p), t.length() > 40 ? t.substring(0, 40) : t);
                }
                i++;
            }
            out.println("\n==== 排版后 参考文献附录表格题注 ====");
            int j = 0;
            for (XWPFParagraph p : f.getParagraphs()) {
                String t = p.getText() == null ? "" : p.getText().trim();
                if (t.contains("参考文献") || t.startsWith("附录") || t.startsWith("表4-1") || t.startsWith("表7-1") || t.equals("附录")) {
                    out.printf("[f %3d] firstLine=%d | %s | %s%n", j, p.getIndentationFirstLine(), fp(p), t.length() > 40 ? t.substring(0, 40) : t);
                }
                j++;
            }
        }
    }

    private static String fp(XWPFParagraph p) {
        XWPFRun r = null;
        for (XWPFRun x : p.getRuns()) {
            if (x.getText(0) != null && !x.getText(0).trim().isEmpty()) { r = x; break; }
        }
        if (r == null) return "[no-run]";
        return "[sz=" + (r.getFontSize() < 0 ? "?" : r.getFontSize())
                + "/f=" + (r.getFontFamily() != null ? r.getFontFamily() : r.getFontName())
                + "/b=" + r.isBold() + "]";
    }
}
