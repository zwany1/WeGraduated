package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.model.AbstractConfig;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatRule;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 摘要/关键词页面格式化(默认按桂林信息科技学院规范, 可用模板 page_config 的 "abstract" 字段覆盖):
 * 摘要 三号黑体居中加粗("摘要"两字空两格); 摘要内容 小四宋体首行缩进两字;
 * 关键词 小四黑体顶格, 内容小四宋体接排; Abstract 三号 Times 加粗居中;
 * 摘要内容 小四 Times; Key words 小四 Times 加粗顶格, 内容小四 Times.
 */
public class AbstractFormatter {

    private static final Pattern ABSTRACT_TITLE = Pattern.compile("^摘\\s*要\\s*$");
    private static final Pattern KEYWORDS = Pattern.compile("^关键词[:：].*");
    private static final Pattern EN_TITLE = Pattern.compile("^\\s*Abstract\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EN_KEYWORDS = Pattern.compile("^Key\\s*words\\s*[:：].*", Pattern.CASE_INSENSITIVE);

    public void apply(XWPFDocument doc, RuleSet ruleSet, List<DocItem> items) {
        AbstractConfig c = ruleSet.getAbstractConfig();
        FormatRule bodyRule = ruleSet.rule("body");
        boolean inZh = false;
        boolean inEn = false;
        for (DocItem item : items) {
            if (item.isFrontMatter()) {
                continue; // 封面/声明/目录等纯前置不动
            }
            XWPFParagraph p = item.getParagraph();
            String text = item.getText() == null ? "" : item.getText().trim();
            if (ABSTRACT_TITLE.matcher(text).matches()) {
                setTitle(p, c.getZhTitleText(), c.getZhTitleFont(), c.getZhTitleFontSize(),
                        c.isZhTitleBold(), c.getZhBodyFont(), c.isTitlePageBreak());
                inZh = true;
                inEn = false;
                continue;
            }
            if (KEYWORDS.matcher(text).matches()) {
                setLabelBody(p, "关键词", c.getKeywordLabelFont(), c.getKeywordBodyFont(), c.getKeywordFontSize());
                inZh = false;
                inEn = false;
                continue;
            }
            if (EN_TITLE.matcher(text).matches()) {
                setTitle(p, text, c.getEnTitleFont(), c.getEnTitleFontSize(),
                        c.isEnTitleBold(), c.getEnBodyFont(), c.isTitlePageBreak());
                inEn = true;
                inZh = false;
                continue;
            }
            if (EN_KEYWORDS.matcher(text).matches()) {
                setLabelBody(p, "Key words", c.getEnKeywordLabelFont(), c.getEnKeywordBodyFont(), c.getEnKeywordFontSize());
                inEn = false;
                inZh = false;
                continue;
            }
            // 摘要正文: 仅摘要区段, 避免正文段被误套(正文交 applyBody)
            if (item.isAbstractSection() && inZh && !text.isEmpty()) {
                setBody(p, c.getZhBodyFont(), c.getZhBodyFontSize(), bodyRule);
            } else if (item.isAbstractSection() && inEn && !text.isEmpty()) {
                setBody(p, c.getEnBodyFont(), c.getEnBodyFontSize(), null);
            }
        }
    }

    private static void setTitle(XWPFParagraph p, String text, String font, int sizePt, boolean bold,
                                 String latin, boolean pageBreak) {
        clearRuns(p);
        if (pageBreak) {
            // 显式分页符(兼容 Word/WPS): 标题前插入 <w:br w:type="page"/>
            XWPFRun brRun = p.createRun();
            brRun.addBreak(BreakType.PAGE);
        }
        XWPFRun run = p.createRun();
        run.setText(text);
        TextFormatter.setFont(run, font, latin);
        run.setFontSize(sizePt);
        run.setBold(bold);
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        ParagraphFormatter.setOutlineLevel(p, 0);
    }

    private static void setBody(XWPFParagraph p, String font, int sizePt, FormatRule bodyRule) {
        for (XWPFRun run : p.getRuns()) {
            TextFormatter.setFont(run, font, TextFormatter.DEFAULT_LATIN);
            run.setFontSize(sizePt);
            run.setBold(false);
        }
        if (bodyRule != null) {
            ParagraphFormatter.apply(p, bodyRule);
        }
        p.setAlignment(ParagraphAlignment.BOTH);
    }

    /**
     * 标签+接排内容: 标签(关键词:/Key words:)加粗顶格, 内容普通字体接排
     */
    private static void setLabelBody(XWPFParagraph p, String fallbackLabel,
                                     String boldFont, String bodyFont, int sizePt) {
        String full = p.getText() == null ? "" : p.getText();
        int splitIdx = -1;
        for (int i = 0; i < full.length(); i++) {
            char c = full.charAt(i);
            if (c == ':' || c == '：') {
                splitIdx = i + 1;
                break;
            }
        }
        String label;
        String body;
        if (splitIdx > 0) {
            label = full.substring(0, splitIdx);
            body = full.substring(splitIdx);
        } else {
            label = fallbackLabel;
            body = full;
        }
        clearRuns(p);
        XWPFRun labelRun = p.createRun();
        labelRun.setText(label);
        TextFormatter.setFont(labelRun, boldFont, TextFormatter.DEFAULT_LATIN);
        labelRun.setFontSize(sizePt);
        labelRun.setBold(true);
        if (body != null && !body.isEmpty()) {
            XWPFRun bodyRun = p.createRun();
            bodyRun.setText(body);
            TextFormatter.setFont(bodyRun, bodyFont, TextFormatter.DEFAULT_LATIN);
            bodyRun.setFontSize(sizePt);
            bodyRun.setBold(false);
        }
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
    }

    private static void clearRuns(XWPFParagraph p) {
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }
    }
}