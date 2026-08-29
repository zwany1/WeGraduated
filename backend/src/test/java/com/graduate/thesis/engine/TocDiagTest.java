package com.graduate.thesis.engine;

import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTemplate;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 临时诊断: 用 testdata 样本 + 非默认 tocConfig 跑一次, 看目录条目 run 的 sz/rFonts 是否真被改写.
 */
public class TocDiagTest {

    @Test
    void diag() throws Exception {
        File src = findTocSample(new File("data/storage/upload"));
        if (src == null) src = findTocSample(new File("data/storage/result"));
        if (src == null) src = findTocSample(new File("data/storage/upload/20260805"));
        if (src == null) {
            File td = new File("../testdata/test_paper.docx");
            if (td.exists()) src = td;
        }
        if (src == null) {
            System.out.println("未找到含自动目录的样本");
            return;
        }
        System.out.println("样本: " + src.getAbsolutePath());
        printRuns("before", src);

        FormatTemplate t = new FormatTemplate();
        t.setId(1L);
        t.setName("diag");
        t.setTocConfig("{\"toc1\":{\"font\":\"楷体\",\"fontLatin\":\"Arial\",\"fontSize\":20},"
                + "\"toc2\":{\"font\":\"楷体\",\"fontLatin\":\"Arial\",\"fontSize\":18},"
                + "\"toc3\":{\"font\":\"楷体\",\"fontLatin\":\"Arial\",\"fontSize\":16},"
                + "\"lineSpacing\":1.5,\"leader\":\"dot\"}");
        List<FormatRule> rules = new ArrayList<>();
        rules.add(rule("body", "宋体", 12, false, "left", 2));
        rules.add(rule("heading1", "黑体", 16, true, "left", 0));
        rules.add(rule("heading2", "黑体", 14, true, "left", 0));
        rules.add(rule("heading3", "黑体", 12, true, "left", 0));
        RuleSet rs = RuleSet.from(t, rules);

        File out = new FormatEngine().format(src, rs, p -> {});
        out.deleteOnExit();
        printRuns("after", out);
    }

    private static int scanned = 0;

    private static File findTocSample(File dir) {
        if (dir == null || !dir.isDirectory() || scanned >= 40) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (scanned >= 40) return null;
            if (f.isDirectory()) {
                File r = findTocSample(f);
                if (r != null) return r;
                continue;
            }
            if (!f.getName().toLowerCase().endsWith(".docx")) continue;
            scanned++;
            try (XWPFDocument doc = new XWPFDocument(new FileInputStream(f))) {
                int cnt = 0;
                for (XWPFParagraph p : doc.getParagraphs()) {
                    if (StructureDetector.isTocStructure(p, doc)) cnt++;
                }
                if (cnt > 0) {
                    System.out.println("hit toc=" + cnt + " : " + f.getName());
                    return f;
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    private void printRuns(String label, File f) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(f))) {
            int n = 0;
            for (XWPFParagraph p : doc.getParagraphs()) {
                if (!StructureDetector.isTocStructure(p, doc)) continue;
                String t = p.getText() == null ? "" : p.getText().trim();
                if (t.isEmpty()) continue;
                n++;
                if (n <= 6) {
                    System.out.println(label + " #" + n + " pStyle=" + pStyleOf(p)
                            + " | " + t.substring(0, Math.min(28, t.length()))
                            + " | " + firstRPr(p));
                }
            }
            System.out.println(label + " isTocStructure非空条目=" + n);
        }
    }

    private static String firstRPr(XWPFParagraph p) {
        var ctp = p.getCTP();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR ctr = null;
        for (var c : ctp.getRList()) { ctr = c; break; }
        if (ctr == null) for (var hl : ctp.getHyperlinkList()) {
            for (var c : hl.getRList()) { ctr = c; break; }
            if (ctr != null) break;
        }
        if (ctr == null) return "no-run";
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr rPr = ctr.getRPr();
        if (rPr == null) return "no-rPr";
        int sz = -1;
        if (rPr.sizeOfSzArray() > 0) sz = ((java.math.BigInteger) rPr.getSzArray(0).getVal()).intValue();
        String ea = "-", asc = "-";
        if (rPr.sizeOfRFontsArray() > 0) {
            var f = rPr.getRFontsArray(0);
            ea = f.getEastAsia();
            asc = f.getAscii();
        }
        return "sz=" + sz + " ea=" + ea + " asc=" + asc;
    }

    private static String shortXml(Object rPr) {
        String s = rPr.toString().replaceAll("\\s+", " ");
        return s.length() > 220 ? s.substring(0, 220) + "..." : s;
    }

    private static String pStyleOf(XWPFParagraph p) {
        try {
            var pPr = p.getCTP().getPPr();
            if (pPr != null && pPr.isSetPStyle()) return pPr.getPStyle().getVal();
        } catch (Exception e) {
        }
        return "-";
    }

    private FormatRule rule(String type, String font, int size, boolean bold, String align, int indent) {
        FormatRule r = new FormatRule();
        r.setRuleType(type);
        r.setFont(font);
        r.setFontLatin("Times New Roman");
        r.setFontSize(size);
        r.setBold(bold);
        r.setAlign(align);
        r.setLineSpacing(1.5f);
        r.setLineSpacingType("multiple");
        r.setFirstLineIndent(indent);
        r.setSpaceAfter(0);
        r.setCaptionEnabled(true);
        return r;
    }
}
