package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.ParagraphKind;
import com.graduate.thesis.engine.StructureHelper;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatRule;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图表编号格式化: 图/表按 章节号-序号 自动编号并排版题注
 *
 * 编号准则:
 *   - 章号只取"当前所在章的一级标题"的显式编号; 参考文献/附录/致谢等非正文章节不参与递增,
 *     取不到时沿用上一有效章号(并封顶), 避免出现"表10-1"这种不存在的章号;
 *   - 已有题注按当前章号重编号, 同时保留原题名(不丢"PLC选型对比表"这类文字);
 *   - 对象旁边没有题注时, 先尝试复用文档里游离的题注题名; 连题名都没有则不生成"裸编号",
 *     只在报告里记为"未编号图表", 由作者补写题名。
 */
public class CaptionFormatter {

    private static final Pattern FIGURE_CAPTION = StructureHelper.FIGURE_CAPTION;
    private static final Pattern TABLE_CAPTION = StructureHelper.TABLE_CAPTION;

    /** 章号上限: 防止把非章节标题的序号累加成不存在的章号 */
    private static final int MAX_CHAPTER = 99;

    /** 本次题注处理统计(写入排版报告) */
    public static class Stats {
        public int uncaptionedFigures;
        public int uncaptionedTables;
        /** 题注文字存在但没有紧邻图表(被空行/段落隔开), 已按正文保留 */
        public int orphanCaptions;
        /** 编号因章节顺序变化而变化的题注数量 */
        public int renumberedCaptions;
        /** 改号样例, 最多 5 条, 形如「图3-1 → 图5-1」 */
        public List<String> captionRenumberSamples = new ArrayList<>();
    }

    public Stats apply(XWPFDocument doc, List<DocItem> items, RuleSet ruleSet) {
        Stats stats = new Stats();
        Map<XWPFParagraph, DocItem> byParagraph = new HashMap<>();
        for (DocItem item : items) {
            byParagraph.put(item.getParagraph(), item);
        }

        FormatRule figureRule = ruleSet.rule("figure");
        FormatRule tableRule = ruleSet.rule("table");
        boolean figureEnabled = figureRule == null || !Boolean.FALSE.equals(figureRule.getCaptionEnabled());
        boolean tableEnabled = tableRule == null || !Boolean.FALSE.equals(tableRule.getCaptionEnabled());

        List<IBodyElement> elements = doc.getBodyElements();
        // 先整篇扫一遍一级标题, 得到"每个位置所属的章号", 避免编号依赖遍历中途才出现的章标题
        // (打乱稿里正文块可能排在章标题之前, 边走边猜会得到一半旧编号一半新编号)
        int[] chapterAt = computeChapters(elements, ruleSet);

        int figIndex = 0;
        int tblIndex = 0;
        int lastFigChapter = -1;
        int lastTableChapter = -1;
        boolean inToc = false;

        for (int i = 0; i < elements.size(); i++) {
            IBodyElement el = elements.get(i);
            if (el instanceof XWPFParagraph) {
                XWPFParagraph p = (XWPFParagraph) el;
                String text = p.getText() == null ? "" : p.getText().trim();

                // 目录区: 跳过目录标题与目录条目的章节追踪
                if (StructureHelper.isTocTitle(text)) {
                    inToc = true;
                    continue;
                }
                if (inToc) {
                    if (StructureHelper.headingLevel(p) > 0
                            || (StructureHelper.isHeading1(p, text, ruleSet) && !StructureHelper.isTocEntry(text))) {
                        inToc = false;
                    } else {
                        continue;
                    }
                }

                if (StructureHelper.isHeading1(p, text, ruleSet)) {
                    continue; // 章号已由 computeChapters 预先算好
                }

                DocItem item = byParagraph.get(p);
                if (item == null) {
                    continue;
                }
                if (item.isFrontMatter()) {
                    continue; // 封面/摘要/目录等前置内容不改图表题注
                }
                ParagraphKind kind = item.getKind();
                int chapter = chapterAt[i];
                if (kind == ParagraphKind.IMAGE) {
                    if (!figureEnabled) {
                        continue;
                    }
                    boolean nextIsCaption = i + 1 < elements.size()
                            && elements.get(i + 1) instanceof XWPFParagraph
                            && StructureHelper.isFigureCaptionText(((XWPFParagraph) elements.get(i + 1)).getText());
                    boolean prevIsCaption = i > 0
                            && elements.get(i - 1) instanceof XWPFParagraph
                            && StructureHelper.isFigureCaptionText(((XWPFParagraph) elements.get(i - 1)).getText());
                    // 图前或图后已有题注: 不再自动插入(题注段由 FIGURE_CAPTION 分支重编号)
                    if (!nextIsCaption && !prevIsCaption) {
                        // 无题注时不再生成"图7-1"这种只有编号没有题名的裸题注(无法被引用),
                        // 只记入报告由作者补写题名
                        stats.uncaptionedFigures++;
                    }
                } else if (kind == ParagraphKind.FIGURE_CAPTION) {
                    if (!figureEnabled) {
                        continue;
                    }
                    // 与图片不相邻的"题注样"段落(正文里一句图号说明)不算题注, 不参与编号
                    if (!adjacentToFigure(elements, i)) {
                        stats.orphanCaptions++;
                        continue;
                    }
                    String title = extractTitle(text, true);
                    int[] nums = extractCaptionNumbers(text, true);
                    if (chapter > 0 && nums[0] > 0) {
                        // 按当前章号重编号, 保留原题名
                        if (chapter != lastFigChapter) {
                            figIndex = 0;
                            lastFigChapter = chapter;
                        }
                        figIndex++;
                        if (!containsEmbeddedObject(p)) {
                            noteRenumber(stats, "图", nums[0], nums[1], chapter, figIndex);
                            setCaptionText(p, buildCaption(figureRule, chapter, figIndex, title));
                        }
                    } else if (chapter > 0) {
                        if (chapter != lastFigChapter) {
                            figIndex = 0;
                            lastFigChapter = chapter;
                        }
                        figIndex = Math.max(figIndex, nums[1]);
                    }
                    applyCaptionStyle(p, figureRule);
                } else if (kind == ParagraphKind.TABLE_CAPTION) {
                    // 表题注的编号由表格分支统一处理; 这里只统计"与表格不相邻"的游离题注
                    if (!adjacentToTable(elements, i)) {
                        stats.orphanCaptions++;
                    }
                }
            } else if (el instanceof XWPFTable) {
                if (!tableEnabled) {
                    continue; // 前置内容(第一章前)的表格不处理
                }
                XWPFTable table = (XWPFTable) el;
                IBodyElement prev = i > 0 ? elements.get(i - 1) : null;
                IBodyElement next = i + 1 < elements.size() ? elements.get(i + 1) : null;
                boolean prevIsCaption = prev instanceof XWPFParagraph
                        && StructureHelper.isTableCaptionText(((XWPFParagraph) prev).getText());
                boolean nextIsCaption = next instanceof XWPFParagraph
                        && StructureHelper.isTableCaptionText(((XWPFParagraph) next).getText());
                int tableChapter = chapterAt[i];
                if (tableChapter <= 0) {
                    continue; // 还没有章号可用: 保留原稿题注不动
                }
                if (tableChapter != lastTableChapter) {
                    tblIndex = 0;
                    lastTableChapter = tableChapter;
                }
                if (prevIsCaption) {
                    renumberTableCaption((XWPFParagraph) prev, tableRule, tableChapter, ++tblIndex, stats);
                } else if (nextIsCaption) {
                    renumberTableCaption((XWPFParagraph) next, tableRule, tableChapter, ++tblIndex, stats);
                } else {
                    // 无题注: 不生成只有编号没有题名的裸题注, 只记入报告由作者补题名
                    stats.uncaptionedTables++;
                }
            }
        }
        return stats;
    }

    /** 记录一次题注改号(新旧编号相同则忽略)。
     *  正文里的「如图3-1所示」不会跟着变, 所以要出报告提示作者核对交叉引用。 */
    private void noteRenumber(Stats stats, String kind, int oldChapter, int oldNo, int newChapter, int newNo) {
        if (oldChapter == newChapter && oldNo == newNo) {
            return;
        }
        stats.renumberedCaptions++;
        if (stats.captionRenumberSamples.size() < 5) {
            stats.captionRenumberSamples.add(kind + oldChapter + "-" + oldNo + " → " + kind + newChapter + "-" + newNo);
        }
    }

    /** 已有表题注: 按当前章号重编号并保留原题名 */
    private void renumberTableCaption(XWPFParagraph p, FormatRule rule, int chapter, int no, Stats stats) {
        String text = p.getText() == null ? "" : p.getText().trim();
        int[] nums = extractCaptionNumbers(text, false);
        if (chapter > 0 && nums[0] > 0 && !containsEmbeddedObject(p)) {
            String title = extractTitle(text, false);
            noteRenumber(stats, "表", nums[0], nums[1], chapter, no);
            setCaptionText(p, buildCaption(rule, chapter, no, title));
        }
        applyCaptionStyle(p, rule);
    }

    /**
     * 预先算出"每个 body 元素所属的章号": 遇到一级标题就更新当前章号, 其余元素沿用当前章号。
     * 章号只取一级标题里的显式编号; 参考文献/附录/致谢等非正文章节不递增; 尚未出现章标题的位置为 0。
     */
    private static int[] computeChapters(List<IBodyElement> elements, RuleSet ruleSet) {
        int[] result = new int[elements.size()];
        int current = 0;
        boolean inToc = false;
        for (int i = 0; i < elements.size(); i++) {
            IBodyElement el = elements.get(i);
            if (el instanceof XWPFParagraph) {
                XWPFParagraph p = (XWPFParagraph) el;
                String text = p.getText() == null ? "" : p.getText().trim();
                if (StructureHelper.isTocTitle(text)) {
                    inToc = true;
                } else if (inToc) {
                    if (StructureHelper.headingLevel(p) > 0
                            || (StructureHelper.isHeading1(p, text, ruleSet) && !StructureHelper.isTocEntry(text))) {
                        inToc = false;
                    }
                }
                if (!inToc && StructureHelper.isHeading1(p, text, ruleSet)) {
                    current = nextChapter(text, current);
                }
            }
            result[i] = current;
        }
        return result;
    }

    /**
     * 章号推算: 有一级标题里的显式编号就用它; 参考文献/附录/致谢等非正文章节不递增;
     * 其余无编号章标题(如"绪论")按顺序递增。
     */
    static int nextChapter(String text, int prev) {
        int explicit = StructureHelper.extractChapter(text);
        if (explicit > 0) {
            return Math.min(explicit, MAX_CHAPTER);
        }
        if (isNonChapterSection(text)) {
            return prev;
        }
        return Math.min(prev + 1, MAX_CHAPTER);
    }

    /** 非正文章节(参考文献/附录/致谢/声明/摘要/目录等), 不参与章号递增 */
    private static boolean isNonChapterSection(String text) {
        if (text == null) {
            return true;
        }
        String t = text.replaceAll("\\s+", "");
        return t.startsWith("参考文献") || t.startsWith("附录") || t.startsWith("附録")
                || t.startsWith("致谢") || t.startsWith("谢辞")
                || t.startsWith("摘要") || t.equalsIgnoreCase("Abstract")
                || t.startsWith("Abstract") || t.equals("目录") || t.equals("目録")
                || t.startsWith("独创性声明") || t.startsWith("学位论文")
                || t.startsWith("攻读") || t.startsWith("个人简历")
                || t.startsWith("学术论文") || t.startsWith("学位期间");
    }

    /** 段落里是否嵌了图片/公式(有则不改写文字, 免得破坏对象) */
    private static boolean containsEmbeddedObject(XWPFParagraph p) {
        try {
            String xml = p.getCTP().xmlText();
            return xml.contains("<w:drawing") || xml.contains("<w:pict") || xml.contains("<m:oMath");
        } catch (Exception e) {
            return true;
        }
    }

    private static String extractTitle(String text, boolean figure) {
        if (text == null) {
            return "";
        }
        Matcher m = (figure ? FIGURE_CAPTION : TABLE_CAPTION).matcher(text.trim());
        return m.matches() && m.group(2) != null ? m.group(2).trim() : "";
    }

    /**从"图1-1 标题" / "表2.3 标题"中提取 [章节号, 序号], 用于顺延计数器 */
    private static int[] extractCaptionNumbers(String text, boolean figure) {
        Matcher m = (figure ? FIGURE_CAPTION : TABLE_CAPTION).matcher(text.trim());
        if (!m.matches()) {
            return new int[]{0, 0};
        }
        // 提取数字部分: 去掉"图"/"表"前缀和后续标题文字
        String afterPrefix = text.trim().replaceFirst("^图\\s*", "").replaceFirst("^表\\s*", "");
        // 按 [-．. ] 切分: "1-1 标题" → ["1","1 标题"] / "1 标题" → ["1","标题"]
        String[] parts = afterPrefix.split("[-．.\\s]+", 3);
        int chapter = 0;
        int no = 0;
        if (parts.length >= 1) {
            try { chapter = Integer.parseInt(parts[0].trim()); } catch (Exception ignore) {}
        }
        if (parts.length >= 2) {
            // 题注常常写成「图5-1程序执行流程图」(编号后没有空格), 此时 parts[1] 是 "1程序执行流程图",
            // 直接 parseInt 会失败并得到 0 —— 取开头的连续数字即可
            Matcher dm = Pattern.compile("^(\\d+)").matcher(parts[1].trim());
            if (dm.find()) {
                try { no = Integer.parseInt(dm.group(1)); } catch (Exception ignore) {}
            }
        }
        return new int[]{chapter, no};
    }

    private static String buildCaption(FormatRule rule, int chapter, int no, String title) {
        String pattern = (rule != null && rule.getNumberingPattern() != null && !rule.getNumberingPattern().isEmpty())
                ? rule.getNumberingPattern()
                : null;
        // 配了 "auto" 或其它不含占位符的格式时回落到默认格式, 否则题注会变成"auto 标题"这种没有编号的文字
        if (pattern == null || !pattern.contains("{chapter}") || !pattern.contains("{no}")) {
            pattern = (rule != null && "table".equals(rule.getRuleType())) ? "表{chapter}-{no}" : "图{chapter}-{no}";
        }
        String caption = pattern
                .replace("{chapter}", String.valueOf(chapter))
                .replace("{no}", String.valueOf(no));
        if (title != null && !title.isEmpty()) {
            caption += " " + title;
        }
        return caption;
    }

    private static void setCaptionText(XWPFParagraph paragraph, String text) {
        for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
        XWPFRun run = paragraph.createRun();
        run.setText(text);
    }

    /** 规则是否使用"固定值"行距(如 20 磅) */
    private static boolean isExactSpacing(FormatRule rule) {
        return rule != null && "exact".equalsIgnoreCase(rule.getLineSpacingType())
                && rule.getLineSpacingExact() != null && rule.getLineSpacingExact() > 0;
    }

    /** 按规则生成 w:spacing 片段: 固定值用 pt*20(twips), 倍数用 mult*240 */
    private static String spacingXml(FormatRule rule) {
        if (isExactSpacing(rule)) {
            return "<w:spacing w:line=\"" + (rule.getLineSpacingExact() * 20) + "\" w:lineRule=\"exact\"/>";
        }
        float mult = (rule != null && rule.getLineSpacing() != null && rule.getLineSpacing() > 0)
                ? rule.getLineSpacing() : 1.5f;
        return "<w:spacing w:line=\"" + Math.round(mult * 240) + "\" w:lineRule=\"auto\"/>";
    }

    private static void applyCaptionStyle(XWPFParagraph paragraph, FormatRule rule) {
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        // 题注居中前先清掉原稿的首行/左缩进, 否则"居中"会偏
        clearIndent(paragraph);
        if (isExactSpacing(rule)) {
            // 校规普遍要求"行间距取固定值"(如桂林信息科技学院 20 磅), 不能一律写死 1.5 倍
            paragraph.setSpacingBetween(rule.getLineSpacingExact(), LineSpacingRule.EXACT);
        } else {
            float mult = (rule != null && rule.getLineSpacing() != null && rule.getLineSpacing() > 0)
                    ? rule.getLineSpacing() : 1.5f;
            paragraph.setSpacingBetween(mult, LineSpacingRule.AUTO);
        }
        double sizePt = (rule != null && rule.getFontSize() != null && rule.getFontSize() > 0)
                ? rule.getFontSize() : 10;
        for (XWPFRun run : paragraph.getRuns()) {
            String font = (rule != null && rule.getFont() != null) ? rule.getFont() : "宋体";
            TextFormatter.setFont(run, font);
            run.setFontSize(sizePt);
        }
    }

    private static void clearIndent(XWPFParagraph paragraph) {
        CTPPr pPr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        CTInd ind = pPr.isSetInd() ? pPr.getInd() : pPr.addNewInd();
        ind.setFirstLine(BigInteger.ZERO);
        ind.setFirstLineChars(BigInteger.ZERO);
        ind.setLeft(BigInteger.ZERO);
        ind.setLeftChars(BigInteger.ZERO);
    }

    private static boolean adjacentToFigure(List<IBodyElement> elements, int i) {
        return (i > 0 && containsDrawing(elements.get(i - 1)))
                || (i + 1 < elements.size() && containsDrawing(elements.get(i + 1)));
    }

    private static boolean adjacentToTable(List<IBodyElement> elements, int i) {
        return (i > 0 && elements.get(i - 1) instanceof XWPFTable)
                || (i + 1 < elements.size() && elements.get(i + 1) instanceof XWPFTable);
    }

    private static boolean containsDrawing(IBodyElement el) {
        if (!(el instanceof XWPFParagraph)) {
            return false;
        }
        try {
            String xml = ((XWPFParagraph) el).getCTP().xmlText();
            return xml.contains("<w:drawing") || xml.contains("<w:pict");
        } catch (Exception e) {
            return false;
        }
    }
}
