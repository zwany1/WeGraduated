package com.graduate.thesis.engine;

import com.graduate.thesis.engine.model.FormatReport;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTemplate;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 排版问题修复的回归验证(P1-P10):
 * 用真实论文样张(原稿 / 章级乱序 / 段级乱序) × 三套版式跑一遍引擎, 输出产物与报告,
 * 供后续脚本做 XML 级核对。样张不存在时自动跳过, 不影响常规构建。
 *
 * 用法: mvn -o test -Dtest=LayoutFixVerifyTest
 */
public class LayoutFixVerifyTest {

    private static final String SAMPLE_DIR = "D:/project/Graduated/tmp_v4";
    private static final String OUT_DIR = "D:/project/Graduated/tmp_fix/out";

    @Test
    void formatRealSamples() throws Exception {
        File orig = new File(SAMPLE_DIR + "/orig/orig.docx");
        assumeTrue(orig.exists(), "真实论文样张不存在, 跳过");
        File outDir = new File(OUT_DIR);
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IllegalStateException("无法创建输出目录 " + outDir);
        }

        String[][] cases = {
                {"orig", orig.getAbsolutePath()},
                {"s1", SAMPLE_DIR + "/s1_chapter_shuffle.docx"},
                {"s2", SAMPLE_DIR + "/s2_block_shuffle.docx"},
        };
        RuleSet[] templates = {ruleSetA(), ruleSetB(), ruleSetC()};
        String[] names = {"A", "B", "C"};

        for (String[] c : cases) {
            File src = new File(c[1]);
            if (!src.exists()) {
                continue;
            }
            for (int t = 0; t < templates.length; t++) {
                FormatReport report = new FormatReport();
                long t0 = System.currentTimeMillis();
                File out;
                try {
                    out = new FormatEngine().format(src, templates[t], p -> { }, report);
                } catch (RuntimeException e) {
                    System.out.println("[失败] " + c[0] + "/" + names[t]);
                    e.printStackTrace(System.out);
                    Throwable cause = e.getCause();
                    while (cause != null) {
                        System.out.println("  原因: " + cause);
                        cause.printStackTrace(System.out);
                        cause = cause.getCause();
                    }
                    throw e;
                }
                report.evaluateStructure();
                File target = new File(outDir, "fix_" + c[0] + "_" + names[t] + ".docx");
                try (FileInputStream in = new FileInputStream(out);
                     FileOutputStream fos = new FileOutputStream(target)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        fos.write(buf, 0, n);
                    }
                }
                out.delete();
                System.out.println("[产物] " + target.getName() + " 用时 " + (System.currentTimeMillis() - t0) + "ms");
                System.out.println("[报告] " + c[0] + "/" + names[t]
                        + " 章=" + report.getChapterCount()
                        + " 节=" + report.getSectionCount()
                        + " 图=" + report.getFigureCount()
                        + " 表=" + report.getTableCount()
                        + " 文献=" + report.getReferenceCount()
                        + " 正文=" + report.getBodyParagraphs()
                        + " 自动识别标题=" + report.getAutoFixedHeadings()
                        + " 无题注图=" + report.getUncaptionedFigures()
                        + " 无题注表=" + report.getUncaptionedTables()
                        + " 游离题注=" + report.getOrphanCaptions()
                        + " 疑似=" + report.getSuspects().size());
                if (!report.getAutoFixedHeadingItems().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (FormatReport.AutoFixed a : report.getAutoFixedHeadingItems()) {
                        sb.append("[").append(a.getIndex()).append(":").append(a.getText()).append("] ");
                    }
                    System.out.println("[报告] 自动识别标题清单: " + sb);
                }
                inspect(target);
            }
        }
    }

    /** 粗略核对产物结构, 便于人工/脚本比对 */
    private void inspect(File f) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(f))) {
            int drawings = 0;
            int pbbOn = 0;
            int pbbOff = 0;
            int firstLine480 = 0;
            int captions = 0;
            int tocEntries = 0;
            for (XWPFParagraph p : doc.getParagraphs()) {
                String xml = p.getCTP().xmlText();
                drawings += countOf(xml, "<w:drawing");
                if (xml.contains("<w:sectPr")) {
                    continue;
                }
                if (xml.contains("<w:pageBreakBefore")) {
                    if (xml.contains("w:pageBreakBefore w:val=\"0\"") || xml.contains(":pageBreakBefore w:val=\"0\"")) {
                        pbbOff++;
                    } else {
                        pbbOn++;
                    }
                }
                if (xml.contains("w:firstLine=\"480\"")) {
                    firstLine480++;
                }
                String text = p.getText() == null ? "" : p.getText().trim();
                if (text.matches("^(图|表)\\s*\\d+.*")) {
                    captions++;
                }
                if (StructureHelper.isTocEntry(text)) {
                    tocEntries++;
                }
            }
            String docXml = doc.getDocument().xmlText();
            System.out.println("  核对: 段=" + doc.getParagraphs().size()
                    + " 图片=" + drawings
                    + " 题注段=" + captions
                    + " 目录条目=" + tocEntries
                    + " 段前分页(开/关)=" + pbbOn + "/" + pbbOff
                    + " 首行480=" + firstLine480
                    + " sectPr=" + countOf(docXml, "<w:sectPr")
                    + " headerRef=" + countOf(docXml, "<w:headerReference")
                    + " tcBorders=" + countOf(docXml, "<w:tcBorders")
                    + " tblBorders=" + countOf(docXml, "<w:tblBorders"));
        }
    }

    private static int countOf(String s, String sub) {
        int c = 0;
        int i = 0;
        while ((i = s.indexOf(sub, i)) >= 0) {
            c++;
            i += sub.length();
        }
        return c;
    }

    // ===== 三套版式 =====

    private RuleSet ruleSetA() {
        FormatTemplate t = baseTemplate("A-校规", page("A4", 2.5, 2.5, 2.5, 2.2, ""));
        List<FormatRule> rules = new ArrayList<>();
        rules.add(rule("heading1", "黑体", 14, false, "left", 0, "exact", 20, 0, 0));
        rules.add(rule("heading2", "黑体", 12, false, "left", 0, "exact", 20, 0, 0));
        rules.add(rule("heading3", "楷体", 12, false, "left", 0, "exact", 20, 0, 0));
        rules.add(rule("body", "宋体", 12, false, "both", 2, "exact", 20, 0, 0));
        rules.add(rule("figure", "宋体", 10.5, false, "center", 0, "exact", 20, 0, 0));
        rules.add(rule("table", "宋体", 10.5, false, "center", 0, "exact", 20, 0, 0));
        rules.add(rule("tableText", "宋体", 10.5, false, "center", 0, "exact", 20, 0, 0));
        RuleSet rs = RuleSet.from(t, rules);
        return rs;
    }

    private RuleSet ruleSetB() {
        FormatTemplate t = baseTemplate("B-B5", page("B5", 2.2, 2.2, 2.0, 2.0, "桂林信息科技学院毕业设计（论文）"));
        List<FormatRule> rules = new ArrayList<>();
        rules.add(rule("heading1", "黑体", 16, true, "center", 0, "multiple", 1.5f, 12, 6));
        rules.add(rule("heading2", "黑体", 14, true, "center", 0, "multiple", 1.5f, 6, 3));
        rules.add(rule("heading3", "楷体", 12, true, "left", 0, "multiple", 1.5f, 3, 0));
        rules.add(rule("body", "仿宋", 12, false, "both", 2, "multiple", 1.5f, 0, 0));
        rules.add(rule("figure", "宋体", 10.5, false, "center", 0, "multiple", 1f, 0, 0));
        rules.add(rule("table", "宋体", 10.5, false, "center", 0, "multiple", 1f, 0, 0));
        rules.add(rule("tableText", "宋体", 10.5, false, "center", 0, "multiple", 1f, 0, 0));
        return RuleSet.from(t, rules);
    }

    private RuleSet ruleSetC() {
        FormatTemplate t = baseTemplate("C-A4五号", page("A4", 2.0, 2.0, 2.0, 2.0, ""));
        List<FormatRule> rules = new ArrayList<>();
        rules.add(rule("heading1", "黑体", 15, true, "left", 0, "multiple", 1f, 10, 4));
        rules.add(rule("heading2", "黑体", 13, true, "left", 0, "multiple", 1f, 6, 0));
        rules.add(rule("heading3", "黑体", 11.5, true, "left", 0, "multiple", 1f, 4, 0));
        // 显式 0: 不缩进(P3 的验证点)
        rules.add(rule("body", "宋体", 10.5, false, "both", 0, "multiple", 1f, 0, 0));
        rules.add(rule("figure", "宋体", 9, false, "center", 0, "multiple", 1f, 0, 0));
        rules.add(rule("table", "宋体", 9, false, "center", 0, "multiple", 1f, 0, 0));
        rules.add(rule("tableText", "宋体", 9, false, "center", 0, "multiple", 1f, 0, 0));
        return RuleSet.from(t, rules);
    }

    private FormatTemplate baseTemplate(String name, String pageConfig) {
        FormatTemplate t = new FormatTemplate();
        t.setId(1L);
        t.setName(name);
        t.setPageConfig(pageConfig);
        t.setHeadingPatterns("{\"heading1\":\"^\\\\d+\",\"heading2\":\"^\\\\d+\\\\.\\\\d+\",\"heading3\":\"^\\\\d+\\\\.\\\\d+\\\\.\\\\d+\"}");
        t.setGenerateToc(true);
        t.setGenerateAbstract(true);
        t.setReferenceConfig("{\"enabled\":true,\"title\":\"参考文献\",\"titleFont\":\"黑体\",\"titleFontSize\":14,"
                + "\"itemFont\":\"宋体\",\"itemFontLatin\":\"Times New Roman\",\"itemFontSize\":10.5,"
                + "\"removeDoi\":true,\"maxAuthors\":3,\"renumber\":true,\"hangingIndentTwips\":640,\"lineSpacing\":1.5}");
        t.setTocConfig(null);
        return t;
    }

    private String page(String paper, double top, double bottom, double left, double right, String headerText) {
        return "{\"paper\":\"" + paper + "\","
                + "\"margin\":{\"top\":" + top + ",\"bottom\":" + bottom + ",\"left\":" + left + ",\"right\":" + right + "},"
                + "\"header\":{\"height\":1.5,\"text\":\"" + headerText + "\"},"
                + "\"footer\":{\"pageNumber\":\"center\",\"font\":\"宋体\",\"fontSize\":10.5,\"skipFirst\":true,\"format\":\"decimal\"}}";
    }

    private FormatRule rule(String type, String font, double size, boolean bold, String align, int indent,
                            String spacingType, float spacing, int spaceBefore, int spaceAfter) {
        FormatRule r = new FormatRule();
        r.setRuleType(type);
        r.setFont(font);
        r.setFontLatin("Times New Roman");
        r.setFontSize(size);
        r.setBold(bold);
        r.setAlign(align);
        r.setLineSpacing(spacing);
        r.setLineSpacingType(spacingType);
        r.setLineSpacingExact(spacingType.equals("exact") ? Math.round(spacing) : null);
        r.setFirstLineIndent(indent);
        r.setSpaceBefore(spaceBefore);
        r.setSpaceAfter(spaceAfter);
        r.setCaptionPosition("below");
        r.setCaptionEnabled(true);
        r.setNumberingPattern("table".equals(type) ? "表{chapter}-{no}" : "图{chapter}-{no}");
        return r;
    }

    @SuppressWarnings("unused")
    private static <T> void noop(Consumer<T> c) {
        // 占位, 保持导入整洁
    }
}
