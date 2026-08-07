package com.graduate.thesis.engine;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 编号统一化: 先统计文档正文段首括号编号的主流风格(阿拉伯（1）或中文（一）),
 * 再将该风格应用于所有正文段首编号, 避免（1）（二）（3）…混用.
 * 处理跨run的编号(如WPS拆分(和二)到不同run).
 */
public final class NumberUnifier {

    public enum Style { ARABIC, CHINESE }

    private static final Pattern LEADING_SEQ = Pattern.compile(
            "^(\\s*)([（(])([一二三四五六七八九十]+|\\d+)([）)])(\\s*)(.*)$");
    private static final Pattern CHINESE = Pattern.compile("[一二三四五六七八九十]+");

    private NumberUnifier() {
    }

    public static Style detectStyle(List<DocItem> items) {
        int chinese = 0;
        int arabic = 0;
        for (DocItem item : items) {
            if (item.getKind() != ParagraphKind.BODY) {
                continue;
            }
            String text = item.getText();
            if (text == null) {
                continue;
            }
            Matcher m = LEADING_SEQ.matcher(text);
            if (m.matches()) {
                if (CHINESE.matcher(m.group(3)).matches()) {
                    chinese++;
                } else {
                    arabic++;
                }
            }
        }
        return chinese > arabic ? Style.CHINESE : Style.ARABIC;
    }

    public static void apply(XWPFParagraph paragraph, Style style) {
        if (paragraph == null || style == null) {
            return;
        }
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) {
            return;
        }
        StringBuilder concat = new StringBuilder();
        List<RunInfo> infos = new ArrayList<>();
        for (XWPFRun run : runs) {
            CTR ctr = run.getCTR();
            String text = (ctr != null && ctr.sizeOfTArray() > 0) ? ctr.getTArray(0).getStringValue() : run.text();
            if (text == null) text = "";
            int start = concat.length();
            concat.append(text);
            infos.add(new RunInfo(run, ctr, start, start + text.length()));
        }
        String full = concat.toString();
        Matcher m = LEADING_SEQ.matcher(full);
        if (!m.matches()) {
            return;
        }
        String no = m.group(3);
        boolean isChinese = CHINESE.matcher(no).matches();
        String converted = no;
        if (isChinese && style == Style.ARABIC) {
            converted = String.valueOf(ChineseNumber.parse(no));
        } else if (!isChinese && style == Style.CHINESE) {
            converted = ChineseNumber.toChinese(Integer.parseInt(no));
        }
        if (converted.equals(no)) {
            return;
        }
        int noStart = m.start(3);
        int noEnd = m.end(3);
        RunInfo target = null;
        for (RunInfo info : infos) {
            if (info.start <= noStart && info.end > noStart) {
                target = info;
                break;
            }
        }
        if (target == null) {
            return;
        }
        CTR ctr = target.ctr;
        if (ctr == null || ctr.sizeOfTArray() == 0) {
            return;
        }
        CTText t = ctr.getTArray(0);
        String orig = t.getStringValue();
        if (orig == null) orig = "";
        int localStart = noStart - target.start;
        int localEnd = noEnd - target.start;
        if (localStart >= 0 && localEnd <= orig.length()) {
            String newText = orig.substring(0, localStart) + converted + orig.substring(localEnd);
            t.setStringValue(newText);
        }
    }

    private static class RunInfo {
        final XWPFRun run;
        final CTR ctr;
        final int start;
        final int end;

        RunInfo(XWPFRun run, CTR ctr, int start, int end) {
            this.run = run;
            this.ctr = ctr;
            this.start = start;
            this.end = end;
        }
    }
}
