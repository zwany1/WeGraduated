package com.graduate.thesis.engine;

import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTemplate;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * StructureDetector / FormatEngine 自测: 模拟手敲文本目录(条目带页码) + 签名行 + 正文,
 * 验证目录条目与签名行在排版后格式不被改动.
 */
public class StructureDetectorSelfTest {

    /** 自测1: detect 应把手敲文本目录条目与签名行判为 frontMatter=true, 正文第一章正常触发章节起点. */
    @Test
    void detect_textTocAndSignature_protected() {
        RuleSet rs = buildRuleSet();
        XWPFDocument doc = new XWPFDocument();
        addPara(doc, "目录");
        addPara(doc, "第一章 绪论 1");
        addPara(doc, "1.1 研究背景 3");
        addPara(doc, "1.2 研究意义 5");
        addPara(doc, "第二章 实验方法...........9");
        addPara(doc, "参考文献 45");
        addPara(doc, "附录 50");
        addPara(doc, "学位论文作者签名： 日期：");
        addPara(doc, "第一章 绪论");
        addPara(doc, "这是正文内容测试段落。");

        List<DocItem> items = new StructureDetector().detect(doc, rs);
        System.out.println("==== detect 自测 ====");
        for (DocItem it : items) {
            System.out.println("kind=" + pad(it.getKind()) + " fm=" + it.isFrontMatter() + " ch=" + it.getChapterNo() + " | " + it.getText());
        }
        check("目录条目'第一章 绪论 1'", items, "第一章 绪论 1");
        check("目录条目'1.1 研究背景 3'", items, "1.1 研究背景 3");
        check("目录条目'参考文献 45'", items, "参考文献 45");
        check("目录条目'附录 50'", items, "附录 50");
        check("签名行", items, "学位论文作者签名： 日期：");
        DocItem h1 = find(items, "第一章 绪论");
        System.out.println("[断言] 正文标题'第一章 绪论': kind=" + h1.getKind() + " fm=" + h1.isFrontMatter() + " ch=" + h1.getChapterNo());
        assert h1.getKind() == ParagraphKind.HEADING1 : "正文应为 HEADING1";
        assert !h1.isFrontMatter() : "正文标题应 frontMatter=false";
        assert h1.getChapterNo() == 1 : "正文 chapterNo 应=1";
        System.out.println("==== detect 断言全部通过 ====");
    }

    /** 自测2: 完整排版后, 目录条目与签名行的字体/字号/加粗/下划线应保持不变. */
    @Test
    void format_tocAndSignature_formatUnchanged() throws Exception {
        RuleSet rs = buildRuleSet();
        File src = File.createTempFile("selftest_src_", ".docx");
        src.deleteOnExit();
        try (XWPFDocument doc = new XWPFDocument()) {
            addPara(doc, "目录", null, 0, false, false);
            // 目录条目用楷体5号(10pt), 与 body 规则(宋体小四12pt)明显不同
            addPara(doc, "第一章 绪论 1", "楷体", 10, false, false);
            addPara(doc, "1.1 研究背景 3", "楷体", 10, false, false);
            addPara(doc, "参考文献 45", "楷体", 10, false, false);
            addPara(doc, "附录 50", "楷体", 10, false, false);
            // 签名行用仿宋4号(14pt) + 下划线
            addPara(doc, "学位论文作者签名： 日期：", "仿宋", 14, false, true);
            addPara(doc, "第一章 绪论", "黑体", 16, true, false);
            addPara(doc, "这是正文内容测试段落。", "宋体", 12, false, false);
            try (FileOutputStream fos = new FileOutputStream(src)) {
                doc.write(fos);
            }
        }
        String beforeToc = fmtOf(src, "参考文献 45");
        String beforeSig = fmtOf(src, "学位论文作者签名： 日期：");
        System.out.println("==== format 自测 ====");
        System.out.println("[排版前] 参考文献目录条目: " + beforeToc);
        System.out.println("[排版前] 签名行:           " + beforeSig);

        File out = new FormatEngine().format(src, rs, p -> {});
        out.deleteOnExit();
        String afterToc = fmtOf(out, "参考文献 45");
        String afterSig = fmtOf(out, "学位论文作者签名： 日期：");
        System.out.println("[排版后] 参考文献目录条目: " + afterToc);
        System.out.println("[排版后] 签名行:           " + afterSig);

        boolean tocOk = beforeToc.equals(afterToc);
        boolean sigOk = beforeSig.equals(afterSig);
        System.out.println("[断言] 参考文献目录条目格式未变: " + tocOk);
        System.out.println("[断言] 签名行格式未变:           " + sigOk);
        assert tocOk : "目录条目格式被改动!\n前:" + beforeToc + "\n后:" + afterToc;
        assert sigOk : "签名行格式被改动!\n前:" + beforeSig + "\n后:" + afterSig;
        System.out.println("==== format 断言全部通过: 目录/签名格式未被改动 ====");
    }

    private String fmtOf(File f, String text) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(f))) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText() == null ? "" : p.getText().trim();
                if (t.equals(text)) {
                    if (p.getRuns().isEmpty()) return "(no run)";
                    XWPFRun r = p.getRuns().get(0);
                    return "font=" + r.getFontFamily() + " size=" + r.getFontSize()
                            + " bold=" + r.isBold() + " underline=" + (r.getUnderline() != null);
                }
            }
        }
        return "(not found)";
    }

    private void check(String label, List<DocItem> items, String text) {
        DocItem it = find(items, text);
        System.out.println("[断言] " + label + ": kind=" + it.getKind() + " fm=" + it.isFrontMatter());
        assert it.isFrontMatter() : label + " 应 frontMatter=true";
    }

    private DocItem find(List<DocItem> items, String text) {
        return items.stream().filter(i -> text.equals(i.getText())).findFirst().orElseThrow();
    }

    private String pad(ParagraphKind k) {
        String s = String.valueOf(k);
        while (s.length() < 18) s = s + " ";
        return s;
    }

    private void addPara(XWPFDocument doc, String text) {
        addPara(doc, text, null, 0, false, false);
    }

    private void addPara(XWPFDocument doc, String text, String font, int sizePt, boolean bold, boolean underline) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        if (font != null) r.setFontFamily(font);
        if (sizePt > 0) r.setFontSize(sizePt);
        r.setBold(bold);
        if (underline) r.setUnderline(UnderlinePatterns.SINGLE);
    }

    private RuleSet buildRuleSet() {
        FormatTemplate t = new FormatTemplate();
        t.setId(1L);
        t.setName("self-test");
        List<FormatRule> rules = new ArrayList<>();
        rules.add(rule("body", "宋体", 12, false, "both", 2));
        rules.add(rule("heading1", "黑体", 16, true, "left", 0));
        rules.add(rule("heading2", "黑体", 14, true, "left", 0));
        rules.add(rule("heading3", "黑体", 12, true, "left", 0));
        return RuleSet.from(t, rules);
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
