package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.ParagraphKind;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatRule;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.util.List;

/**
 * 标题格式化: 应用一至五级标题规则
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

    /**
     * 取标题规则。
     *
     * 模板只配到某一级为止是常态(校规通常只规定到三级), 未配置的层级必须回退,
     * 否则会拿到 RuleSet 里那份"左对齐 + 1.5 倍行距 + 首行缩进"的兜底默认值,
     * 与正文的固定行距对不齐(四级/五级标题半套格式的根因)。
     * 回退顺序: 本级规则 -> 正文规则, 保持"未配置即不特殊处理"。
     */
    private static FormatRule ruleFor(ParagraphKind kind, RuleSet ruleSet) {
        String type = ruleTypeOf(kind);
        if (type == null) {
            return null;
        }
        FormatRule rule = ruleSet.getRules() == null ? null : ruleSet.getRules().get(type);
        if (rule != null) {
            return rule;
        }
        return ruleSet.rule("body");
    }

    private static String ruleTypeOf(ParagraphKind kind) {
        switch (kind) {
            case HEADING1:
                return "heading1";
            case HEADING2:
                return "heading2";
            case HEADING3:
                return "heading3";
            case HEADING4:
                return "heading4";
            case HEADING5:
                return "heading5";
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
            case HEADING4:
                return 3;
            case HEADING5:
                return 4;
            default:
                return -1;
        }
    }
}
