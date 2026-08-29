package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.engine.model.SectionConfig;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 固定章节页格式化(默认按桂林信息科技学院规范, 可用模板 page_config 的 "section" 字段覆盖):
 * 谢辞 黑体四号居中("谢辞"两字中间空两格)、另起一页;
 * 参考文献 黑体四号顶格、另起一页; 附录 黑体四号居中("附录"空两格)、另起一页.
 * 仅格式化正文区(第一章之后)的章节标题, 前置/目录区的同名标题保持原样.
 */
public class SectionFormatter {

    private static final Pattern XIE_CI = Pattern.compile("^谢\\s*辞\\s*$");
    private static final Pattern CANKAO = Pattern.compile("^参考文献\\s*$");
    private static final Pattern FULU = Pattern.compile("^附\\s*录\\s*$");

    public void apply(XWPFDocument doc, List<DocItem> items, RuleSet ruleSet) {
        SectionConfig c = ruleSet.getSectionConfig();
        for (DocItem item : items) {
            // 前置/目录区的参考文献/附录/谢辞标题保持原样, 不被章节标题规则改动
            if (item.isFrontMatter()) {
                continue;
            }
            XWPFParagraph p = item.getParagraph();
            String text = p.getText() == null ? "" : p.getText().trim();
            if (XIE_CI.matcher(text).matches()) {
                setSectionTitle(p, c.getXieCi(), align(c.getXieCiAlign()), c);
            } else if (CANKAO.matcher(text).matches()) {
                setSectionTitle(p, c.getReferences(), align(c.getReferencesAlign()), c);
            } else if (FULU.matcher(text).matches()) {
                setSectionTitle(p, c.getAppendix(), align(c.getAppendixAlign()), c);
            }
        }
    }

    private static ParagraphAlignment align(String s) {
        return "center".equals(s) ? ParagraphAlignment.CENTER
                : "right".equals(s) ? ParagraphAlignment.RIGHT : ParagraphAlignment.LEFT;
    }

    private static void setSectionTitle(XWPFParagraph p, String text, ParagraphAlignment align, SectionConfig c) {
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }
        XWPFRun run = p.createRun();
        run.setText(text);
        TextFormatter.setFont(run, c.getFont(), TextFormatter.DEFAULT_LATIN);
        run.setFontSize(c.getFontSize());
        run.setBold(c.isBold());
        p.setAlignment(align);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        if (c.isPageBreakBefore()) {
            ParagraphFormatter.setPageBreakBefore(p);
        }
        ParagraphFormatter.setOutlineLevel(p, c.getOutlineLevel());
    }
}