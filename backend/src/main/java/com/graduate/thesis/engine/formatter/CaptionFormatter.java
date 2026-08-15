package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.ChineseNumber;
import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.ParagraphKind;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatRule;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图表编号格式化: 图/表按 章节号-序号 自动编号并排版题注
 */
public class CaptionFormatter {

    private static final Pattern FIGURE_CAPTION = Pattern.compile("^图\\s*\\d+([-．.]\\d+)?\\s*(.*)");
    private static final Pattern TABLE_CAPTION = Pattern.compile("^表\\s*\\d+([-．.]\\d+)?\\s*(.*)");
    // 内置一级章节标题识别(与 StructureDetector 一致, 用于表格题注章节号追踪)
    private static final Pattern CN_CHAPTER = Pattern.compile("^第[一二三四五六七八九十百千]+章\\s*.*");
    private static final Pattern NUM_CHAPTER = Pattern.compile("^\\d{1,2}[\\s、．.]\\s*[\\u4e00-\\u9fa5A-Za-z].*");
    private static final Pattern CN_NUM_CHAPTER = Pattern.compile("^[一二三四五六七八九十]{1,3}[、．.]\\s*[\\u4e00-\\u9fa5].*");

    public void apply(XWPFDocument doc, List<DocItem> items, RuleSet ruleSet) {
        Map<XWPFParagraph, DocItem> byParagraph = new HashMap<>();
        for (DocItem item : items) {
            byParagraph.put(item.getParagraph(), item);
        }

        FormatRule figureRule = ruleSet.rule("figure");
        FormatRule tableRule = ruleSet.rule("table");
        boolean figureEnabled = figureRule == null || !Boolean.FALSE.equals(figureRule.getCaptionEnabled());
        boolean tableEnabled = tableRule == null || !Boolean.FALSE.equals(tableRule.getCaptionEnabled());

        int figIndex = 0;
        int tblIndex = 0;
        int lastChapter = -1;
        int lastFigChapter = -1;
        int lastTableChapter = -1;
        boolean inToc = false;

        List<IBodyElement> elements = doc.getBodyElements();
        for (int i = 0; i < elements.size(); i++) {
            IBodyElement el = elements.get(i);
            if (el instanceof XWPFParagraph) {
                XWPFParagraph p = (XWPFParagraph) el;
                String text = p.getText() == null ? "" : p.getText().trim();

                // 目录区: 跳过目录标题与目录条目的章节追踪
                if (isTocTitle(text)) {
                    inToc = true;
                    continue;
                }
                if (inToc) {
                    if (headingLevel(p) > 0) {
                        inToc = false;
                    } else {
                        continue;
                    }
                }

                if (isHeading1(p, text, ruleSet)) {
                    lastChapter = chapterNo(text, lastChapter);
                    continue;
                }

                DocItem item = byParagraph.get(p);
                if (item == null) {
                    continue;
                }
                if (item.isFrontMatter()) {
                    continue; // 封面/摘要/目录等前置内容不改图表题注
                }
                ParagraphKind kind = item.getKind();
                if (kind == ParagraphKind.IMAGE) {
                    if (!figureEnabled) {
                        continue;
                    }
                    boolean nextIsCaption = i + 1 < elements.size()
                            && elements.get(i + 1) instanceof XWPFParagraph
                            && isFigureCaptionText(((XWPFParagraph) elements.get(i + 1)).getText());
                    boolean prevIsCaption = i > 0
                            && elements.get(i - 1) instanceof XWPFParagraph
                            && isFigureCaptionText(((XWPFParagraph) elements.get(i - 1)).getText());
                    // 图前或图后已有题注: 不再自动插入(题注段由 FIGURE_CAPTION 分支重编号)
                    if (!nextIsCaption && !prevIsCaption) {
                        if (item.getChapterNo() != lastFigChapter) {
                            figIndex = 0;
                            lastFigChapter = item.getChapterNo();
                        }
                        figIndex++;
                        String caption = buildCaption(figureRule, item.getChapterNo(), figIndex, "");
                        insertParagraphAfter(doc, p, caption, figureRule);
                    }
                } else if (kind == ParagraphKind.FIGURE_CAPTION) {
                    if (!figureEnabled) {
                        continue;
                    }
                    // 已有题注: 保留原编号, 只统一格式(位置/字体), 不重写数字
                    applyCaptionStyle(p, figureRule);
                }
            } else if (el instanceof XWPFTable) {
                if (!tableEnabled || lastChapter < 0) {
                    continue; // 前置内容(第一章前)的表格不处理
                }
                XWPFTable table = (XWPFTable) el;
                IBodyElement prev = i > 0 ? elements.get(i - 1) : null;
                IBodyElement next = i + 1 < elements.size() ? elements.get(i + 1) : null;
                boolean prevIsCaption = prev instanceof XWPFParagraph
                        && isTableCaptionText(((XWPFParagraph) prev).getText());
                boolean nextIsCaption = next instanceof XWPFParagraph
                        && isTableCaptionText(((XWPFParagraph) next).getText());
                int tableChapter = lastChapter < 0 ? 0 : lastChapter;
                if (tableChapter != lastTableChapter) {
                    tblIndex = 0;
                    lastTableChapter = tableChapter;
                }
                if (prevIsCaption) {
                    // 表前已有题注: 保留原编号, 只统一格式
                    applyCaptionStyle((XWPFParagraph) prev, tableRule);
                } else if (nextIsCaption) {
                    // 表后已有题注: 保留原编号, 只统一格式
                    applyCaptionStyle((XWPFParagraph) next, tableRule);
                } else {
                    // 无题注: 自动补编号
                    tblIndex++;
                    String caption = buildCaption(tableRule, tableChapter, tblIndex, "");
                    insertParagraphBefore(doc, table, caption, tableRule);
                }
            }
        }
    }

    private static void insertParagraphAfter(XWPFDocument doc, XWPFParagraph anchor, String caption, FormatRule rule) {
        insertAfterNode(anchor.getCTP(), buildCaptionXml(rule, caption));
    }

    private static void insertParagraphBefore(XWPFDocument doc, XWPFTable table, String caption, FormatRule rule) {
        insertBeforeNode(table.getCTTbl(), buildCaptionXml(rule, caption));
    }

    /**
     * 在 anchor 节点之后插入新段落(DOM 方式, 绕开 POI insertNewParagraph 死循环 bug)
     */
    private static void insertAfterNode(org.apache.xmlbeans.XmlObject anchor, XmlObject newPara) {
        org.w3c.dom.Node node = anchor.getDomNode();
        org.w3c.dom.Node parent = node.getParentNode();
        org.w3c.dom.Node imported = parent.getOwnerDocument().importNode(toElementNode(newPara), true);
        parent.insertBefore(imported, node.getNextSibling());
    }

    /**
     * 在 target 节点之前插入新段落
     */
    private static void insertBeforeNode(org.apache.xmlbeans.XmlObject target, XmlObject newPara) {
        org.w3c.dom.Node node = target.getDomNode();
        org.w3c.dom.Node parent = node.getParentNode();
        org.w3c.dom.Node imported = parent.getOwnerDocument().importNode(toElementNode(newPara), true);
        parent.insertBefore(imported, node);
    }

    private static org.w3c.dom.Node toElementNode(XmlObject obj) {
        org.w3c.dom.Node node = obj.getDomNode();
        if (node.getNodeType() == org.w3c.dom.Node.DOCUMENT_NODE) {
            return ((org.w3c.dom.Document) node).getDocumentElement();
        }
        return node;
    }

    private static XmlObject buildCaptionXml(FormatRule rule, String caption) {
        String cn = (rule != null && rule.getFont() != null) ? rule.getFont() : "宋体";
        String latin = (rule != null && rule.getFontLatin() != null && !rule.getFontLatin().isEmpty())
                ? rule.getFontLatin() : TextFormatter.DEFAULT_LATIN;
        int sizeHalfPoints = (rule != null && rule.getFontSize() != null && rule.getFontSize() > 0)
                ? rule.getFontSize() * 2 : 21;
        String xml = "<w:p xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:pPr><w:jc w:val=\"center\"/>"
                + "<w:spacing w:line=\"360\" w:lineRule=\"auto\"/></w:pPr>"
                + "<w:r><w:rPr><w:rFonts w:ascii=\"" + latin + "\" w:hAnsi=\"" + latin
                + "\" w:eastAsia=\"" + cn + "\"/>"
                + "<w:sz w:val=\"" + sizeHalfPoints + "\"/><w:szCs w:val=\"" + sizeHalfPoints + "\"/></w:rPr>"
                + "<w:t>" + escapeXml(caption) + "</w:t></w:r></w:p>";
        try {
            return XmlObject.Factory.parse(xml);
        } catch (XmlException e) {
            throw new IllegalStateException("题注XML构造失败", e);
        }
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static boolean isFigureCaptionText(String text) {
        return text != null && FIGURE_CAPTION.matcher(text.trim()).matches();
    }

    private static boolean isTableCaptionText(String text) {
        return text != null && TABLE_CAPTION.matcher(text.trim()).matches();
    }

    private static String extractTitle(String text, boolean figure) {
        if (text == null) {
            return "";
        }
        Matcher m = (figure ? FIGURE_CAPTION : TABLE_CAPTION).matcher(text.trim());
        return m.matches() && m.group(2) != null ? m.group(2).trim() : "";
    }

    private static String buildCaption(FormatRule rule, int chapter, int no, String title) {
        String pattern = (rule != null && rule.getNumberingPattern() != null && !rule.getNumberingPattern().isEmpty())
                ? rule.getNumberingPattern()
                : (rule != null && "table".equals(rule.getRuleType()) ? "表{chapter}.{no}" : "图{chapter}.{no}");
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

    private static void applyCaptionStyle(XWPFParagraph paragraph, FormatRule rule) {
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingBetween(1.5, LineSpacingRule.AUTO);
        int sizePt = (rule != null && rule.getFontSize() != null && rule.getFontSize() > 0)
                ? rule.getFontSize() : 10;
        for (XWPFRun run : paragraph.getRuns()) {
            String font = (rule != null && rule.getFont() != null) ? rule.getFont() : "宋体";
            TextFormatter.setFont(run, font);
            run.setFontSize(sizePt);
        }
    }

    private static boolean isTocTitle(String text) {
        String t = text == null ? "" : text.replace(" ", "").replace("\u00A0", "");
        return t.equals("目录") || t.equals("目録");
    }

    /**
     * 标题级别: 优先大纲级别/样式, 返回 0=非标题 1=一级 2=二级 3=三级
     */
    private static int headingLevel(XWPFParagraph p) {
        try {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr pPr = p.getCTP().getPPr();
            if (pPr != null) {
                if (pPr.isSetOutlineLvl()) {
                    int lvl = pPr.getOutlineLvl().getVal().intValue();
                    if (lvl >= 0 && lvl <= 3) {
                        return lvl + 1;
                    }
                }
                if (pPr.isSetPStyle() && pPr.getPStyle().getVal() != null) {
                    String style = pPr.getPStyle().getVal().toLowerCase();
                    if (style.contains("heading1") || style.equals("2") || style.contains("heading 1")) return 1;
                    if (style.contains("heading2") || style.equals("3") || style.contains("heading 2")) return 2;
                    if (style.contains("heading3") || style.equals("4") || style.contains("heading 3")) return 3;
                }
            }
        } catch (Exception ignore) {
        }
        return 0;
    }

    /**
     * 是否一级标题: 样式一级, 或文本匹配一级标题正则/内置章节识别(排除日期/年份等非标题)
     */
    private static boolean isHeading1(XWPFParagraph p, String text, RuleSet ruleSet) {
        if (headingLevel(p) == 1) {
            return true;
        }
        if (headingLevel(p) > 1) {
            return false;
        }
        // 排除日期/年份/纯数字页眉等(如 "2026 年 5 月 23 日")
        if (isDateLike(text)) {
            return false;
        }
        if (ruleSet.getHeading1Pattern().matcher(text).matches()) {
            return true;
        }
        return isLikelyChapterTitle(text);
    }

    /** 内置自动识别一级章节标题(第一章/1 绪论/一、绪论) */
    private static boolean isLikelyChapterTitle(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        if (t.length() > 40) {
            return false;
        }
        return CN_CHAPTER.matcher(t).matches()
                || NUM_CHAPTER.matcher(t).matches()
                || CN_NUM_CHAPTER.matcher(t).matches();
    }

    /**
     * 是否日期/年份类文本(避免被误当章节标题)
     */
    private static boolean isDateLike(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        // 含 年/月/日 且 数字在前
        if (java.util.regex.Pattern.matches("\\d+\\s*[年月日].*", t)) {
            return true;
        }
        // 纯数字(可能是页码)
        if (java.util.regex.Pattern.matches("\\d{3,}", t)) {
            return true;
        }
        return false;
    }

    /**
     * 章节号: 有编号取编号(排除年月日), 无编号递增
     */
    private static int chapterNo(String text, int prev) {
        int n = extractChapter(text);
        if (n > 0) {
            return n;
        }
        return prev + 1;
    }

    /**
     * 提取章节号, 排除日期/年份文本(如 "2026 年 5 月 23 日" -> 0)
     */
    private static int extractChapter(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String t = text.trim();
        // 日期/年份: 数字紧邻 年月日 不视为章节
        if (java.util.regex.Pattern.matches("\\d+\\s*[年月日].*", t)) {
            return 0;
        }
        return ChineseNumber.extract(text);
    }
}
