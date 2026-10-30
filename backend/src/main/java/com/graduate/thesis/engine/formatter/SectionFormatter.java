package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.engine.model.SectionConfig;
import com.graduate.thesis.entity.FormatRule;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 固定章节页格式化(默认按桂林信息科技学院规范, 可用模板 page_config 的 "section" 字段覆盖):
 * 谢辞 黑体四号居中("谢辞"两字中间空两格)、另起一页;
 * 参考文献 黑体四号顶格、另起一页; 附录 黑体四号居中("附录"空两格)、另起一页.
 * 仅格式化正文区(第一章之后)的章节标题, 前置/目录区的同名标题保持原样.
 *
 * 字号/加粗/段前段后/行距复用模板的「一级标题」规则, 只保留"另起一页 + 各自对齐方式"的差异,
 * 避免换版式时 参考文献/附录 标题不跟着版式变(此前写死 14pt)。
 */
public class SectionFormatter {

    private static final Pattern XIE_CI = Pattern.compile("^谢\\s*辞\\s*$");
    private static final Pattern CANKAO = Pattern.compile("^参考文献\\s*$");
    private static final Pattern FULU = Pattern.compile("^附\\s*录\\s*$");

    public void apply(XWPFDocument doc, List<DocItem> items, RuleSet ruleSet) {
        SectionConfig c = ruleSet.getSectionConfig();
        FormatRule h1 = ruleSet.rule("heading1");
        for (DocItem item : items) {
            // 前置/目录区的参考文献/附录/谢辞标题保持原样, 不被章节标题规则改动
            if (item.isFrontMatter()) {
                continue;
            }
            XWPFParagraph p = item.getParagraph();
            String text = p.getText() == null ? "" : p.getText().trim();
            if (XIE_CI.matcher(text).matches()) {
                formatSectionTitle(p, c.getXieCi(), alignOf(c.getXieCiAlign()), h1, c);
            } else if (CANKAO.matcher(text).matches()) {
                formatSectionTitle(p, c.getReferences(), alignOf(c.getReferencesAlign()), h1, c);
            } else if (FULU.matcher(text).matches()) {
                formatSectionTitle(p, c.getAppendix(), alignOf(c.getAppendixAlign()), h1, c);
            }
        }
    }

    /** 对齐方式: center/right 之外一律左对齐 */
    static ParagraphAlignment alignOf(String s) {
        return "center".equals(s) ? ParagraphAlignment.CENTER
                : "right".equals(s) ? ParagraphAlignment.RIGHT : ParagraphAlignment.LEFT;
    }

    /**
     * 套用固定章节标题: 文字 + 字体 + 复用一级标题规则的字号/加粗/段前段后/行距 + 对齐 + 另起一页 + 大纲级别.
     * ReferenceFormatter 也复用本方法, 保证两处对"参考文献"标题的落地完全一致。
     */
    static void formatSectionTitle(XWPFParagraph p, String text, ParagraphAlignment align,
                                   FormatRule h1, SectionConfig c) {
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }
        XWPFRun run = p.createRun();
        run.setText(text == null || text.isEmpty() ? "" : text);
        TextFormatter.setFont(run, c.getFont(), TextFormatter.DEFAULT_LATIN);
        double size = h1 != null && h1.getFontSize() != null && h1.getFontSize() > 0
                ? h1.getFontSize() : c.getFontSize();
        run.setFontSize(size);
        boolean bold = h1 != null && h1.getBold() != null ? h1.getBold() : c.isBold();
        run.setBold(bold);
        p.setAlignment(align);
        // 段前段后与行距跟随一级标题; 未配置时按 0 处理(原稿常带段前距, 会让标题位置乱飘)
        p.setSpacingBefore(h1 != null && h1.getSpaceBefore() != null ? h1.getSpaceBefore() * 20 : 0);
        p.setSpacingAfter(h1 != null && h1.getSpaceAfter() != null ? h1.getSpaceAfter() * 20 : 0);
        applyLineSpacing(p, h1);
        // 标题不缩进(原稿"参考文献"常带首行缩进/左缩进)
        clearIndent(p);
        if (c.isPageBreakBefore()) {
            ParagraphFormatter.setPageBreakBefore(p, true);
        }
        ParagraphFormatter.setOutlineLevel(p, c.getOutlineLevel());
    }

    /** 行距跟随一级标题规则: 固定值优先, 否则倍数 */
    private static void applyLineSpacing(XWPFParagraph p, FormatRule h1) {
        if (h1 == null) {
            return;
        }
        ParagraphFormatter.applyLineSpacing(p, h1);
    }

    /** 清掉首行缩进/左缩进, 让章节标题顶格或按对齐方式居中 */
    private static void clearIndent(XWPFParagraph p) {
        CTPPr pPr = p.getCTP().isSetPPr() ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
        CTInd ind = pPr.isSetInd() ? pPr.getInd() : pPr.addNewInd();
        ind.setFirstLine(BigInteger.ZERO);
        ind.setFirstLineChars(BigInteger.ZERO);
        ind.setLeft(BigInteger.ZERO);
        ind.setLeftChars(BigInteger.ZERO);
    }
}
