package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.ParagraphKind;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatRule;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.util.List;

/**
 * 标题格式化: 应用一级/二级/三级标题规则
 */
public final class HeadingFormatter {

    private HeadingFormatter() {
    }

    public static void apply(XWPFDocument doc, List<DocItem> items, RuleSet ruleSet) {
        for (DocItem item : items) {
            if (item.isFrontMatter()) {
                continue; // 封面/摘要/目录等前置内容不改标题格式
            }
            FormatRule rule = ruleFor(item.getKind(), ruleSet);
            if (rule != null) {
                TextFormatter.apply(item.getParagraph(), rule);
                ParagraphFormatter.apply(item.getParagraph(), rule);
                if (item.getKind() == ParagraphKind.HEADING1) {
                    ParagraphFormatter.setPageBreakBefore(item.getParagraph());
                }
            }
            int outlineLvl = outlineLevelOf(item.getKind());
            if (outlineLvl >= 0) {
                ParagraphFormatter.setOutlineLevel(item.getParagraph(), outlineLvl);
            }
        }
    }

    private static FormatRule ruleFor(ParagraphKind kind, RuleSet ruleSet) {
        switch (kind) {
            case HEADING1:
                return ruleSet.rule("heading1");
            case HEADING2:
                return ruleSet.rule("heading2");
            case HEADING3:
                return ruleSet.rule("heading3");
            default:
                return null;
        }
    }

    private static int outlineLevelOf(ParagraphKind kind) {
        switch (kind) {
            case HEADING1:
                return 0;
            case HEADING2:
                return 1;
            case HEADING3:
                return 2;
            default:
                return -1;
        }
    }
}
