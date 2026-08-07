package com.graduate.thesis.engine.formatter;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.util.regex.Pattern;

/**
 * 固定章节页格式化(按桂林信息科技学院规范):
 * 谢辞 黑体四号居中("谢辞"两字中间空两格)、另起一页;
 * 参考文献 黑体四号顶格、另起一页; 附录 黑体四号居中("附录"空两格)、另起一页.
 */
public class SectionFormatter {

    private static final Pattern XIE_CI = Pattern.compile("^谢\\s*辞\\s*$");
    private static final Pattern CANKAO = Pattern.compile("^参考文献\\s*$");
    private static final Pattern FULU = Pattern.compile("^附\\s*录\\s*$");

    public void apply(XWPFDocument doc) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            String text = p.getText() == null ? "" : p.getText().trim();
            if (XIE_CI.matcher(text).matches()) {
                setSectionTitle(p, "谢  辞", ParagraphAlignment.CENTER);
            } else if (CANKAO.matcher(text).matches()) {
                setSectionTitle(p, "参考文献", ParagraphAlignment.LEFT);
            } else if (FULU.matcher(text).matches()) {
                setSectionTitle(p, "附  录", ParagraphAlignment.CENTER);
            }
        }
    }

    private static void setSectionTitle(XWPFParagraph p, String text, ParagraphAlignment align) {
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }
        XWPFRun run = p.createRun();
        run.setText(text);
        TextFormatter.setFont(run, "黑体", TextFormatter.DEFAULT_LATIN);
        run.setFontSize(14);
        run.setBold(true);
        p.setAlignment(align);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        ParagraphFormatter.setPageBreakBefore(p);
    }
}
