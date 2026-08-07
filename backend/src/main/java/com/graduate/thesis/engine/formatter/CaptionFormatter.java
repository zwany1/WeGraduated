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

        List<IBodyElement> elements = doc.getBodyElements();
        for (int i = 0; i < elements.size(); i++) {
            IBodyElement el = elements.get(i);
            if (el instanceof XWPFParagraph) {
                XWPFParagraph p = (XWPFParagraph) el;
                String text = p.getText() == null ? "" : p.getText().trim();

                if (ruleSet.getHeading1Pattern().matcher(text).matches()) {
                    lastChapter = ChineseNumber.extract(text);
                    continue;
                }

                DocItem item = byParagraph.get(p);
                if (item == null) {
                    continue;
                }
                ParagraphKind kind = item.getKind();
                if (kind == ParagraphKind.IMAGE) {
                    if (!figureEnabled) {
                        continue;
                    }
                    boolean nextIsCaption = i + 1 < elements.size()
                            && elements.get(i + 1) instanceof XWPFParagraph
                            && isFigureCaptionText(((XWPFParagraph) elements.get(i + 1)).getText());
                    if (!nextIsCaption) {
                        if (item.getChapterNo() != lastChapter) {
                            figIndex = 0;
                            lastChapter = item.getChapterNo();
                        }
                        figIndex++;
                        String caption = buildCaption(figureRule, item.getChapterNo(), figIndex, "");
                        insertParagraphAfter(doc, p, caption, figureRule);
                    }
                } else if (kind == ParagraphKind.FIGURE_CAPTION) {
                    if (!figureEnabled) {
                        continue;
                    }
                    if (item.getChapterNo() != lastChapter) {
                        figIndex = 0;
                        lastChapter = item.getChapterNo();
                    }
                    figIndex++;
                    String title = extractTitle(text, true);
                    String caption = buildCaption(figureRule, item.getChapterNo(), figIndex, title);
                    setCaptionText(p, caption);
                    applyCaptionStyle(p, figureRule);
                }
            } else if (el instanceof XWPFTable) {
                if (!tableEnabled) {
                    continue;
                }
                XWPFTable table = (XWPFTable) el;
                IBodyElement prev = i > 0 ? elements.get(i - 1) : null;
                boolean prevIsCaption = prev instanceof XWPFParagraph
                        && isTableCaptionText(((XWPFParagraph) prev).getText());
                if (lastChapter < 0) {
                    lastChapter = 0;
                }
                tblIndex++;
                if (prevIsCaption) {
                    String title = extractTitle(((XWPFParagraph) prev).getText(), false);
                    String caption = buildCaption(tableRule, lastChapter, tblIndex, title);
                    setCaptionText((XWPFParagraph) prev, caption);
                    applyCaptionStyle((XWPFParagraph) prev, tableRule);
                } else {
                    String caption = buildCaption(tableRule, lastChapter, tblIndex, "");
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
        String pattern = (rule != null && rule.getNumberingPattern() != null)
                ? rule.getNumberingPattern() : "图{chapter}-{no}";
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
}
