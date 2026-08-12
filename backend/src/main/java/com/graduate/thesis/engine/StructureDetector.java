package com.graduate.thesis.engine;

import com.graduate.thesis.engine.model.RuleSet;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文档结构识别器
 */
public class StructureDetector {

    private static final Pattern H2_CHAPTER = Pattern.compile("^(\\d+)\\.");
    private static final Pattern FIGURE_CAPTION = Pattern.compile("^图\\s*\\d+([-．.]\\d+)?.*");
    private static final Pattern TABLE_CAPTION = Pattern.compile("^表\\s*\\d+([-．.]\\d+)?.*");
    private static final Pattern ABSTRACT_TITLE = Pattern.compile("^摘\\s*要\\s*$");
    private static final Pattern KEYWORDS = Pattern.compile("^关键词\\s*[:：].*");
    private static final Pattern EN_TITLE = Pattern.compile("^\\s*Abstract\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EN_KEYWORDS = Pattern.compile("^Key\\s*words\\s*[:：].*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_TITLE = Pattern.compile(
            "^谢\\s*辞\\s*$|^参考文献\\s*$|^附\\s*录\\s*$");

    /**
     * 识别文档结构
     */
    public List<DocItem> detect(XWPFDocument doc, RuleSet ruleSet) {
        List<DocItem> items = new ArrayList<>();
        int currentChapter = 0;
        boolean chapterStarted = false;
        boolean inToc = false;
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
            boolean containsImage = containsImage(paragraph);
            ParagraphKind kind = classify(text, containsImage, paragraph, ruleSet);
            DocItem item = new DocItem(paragraph, kind, text, containsImage);

            // 目录区检测: "目 录" 之后到第一个正文标题前为目录区(目录条目不参与章节追踪)
            if (!chapterStarted && kind == ParagraphKind.HEADING1 && isTocTitle(text)) {
                inToc = true;
                kind = ParagraphKind.SECTION_TITLE;
                item = new DocItem(paragraph, kind, text, containsImage);
            } else if (inToc && !chapterStarted) {
                if (headingLevelByStyle(paragraph) > 0) {
                    inToc = false; // 到达正文第一个样式标题, 目录区结束
                } else {
                    kind = ParagraphKind.BODY; // 目录条目, 不识别为标题
                    item = new DocItem(paragraph, kind, text, containsImage);
                }
            }

            if (kind == ParagraphKind.HEADING1) {
                // 从正文第一章才开始计数
                currentChapter = chapterStarted ? chapterOf(text, currentChapter) : 1;
                chapterStarted = true;
                item.setChapterNo(currentChapter);
            } else if (kind == ParagraphKind.HEADING2) {
                java.util.regex.Matcher m = H2_CHAPTER.matcher(text);
                item.setChapterNo(m.find() ? Integer.parseInt(m.group(1)) : currentChapter);
            } else if (kind == ParagraphKind.HEADING3 || kind == ParagraphKind.FIGURE_CAPTION
                    || kind == ParagraphKind.TABLE_CAPTION || kind == ParagraphKind.IMAGE) {
                item.setChapterNo(currentChapter);
            }
            // 第一个正文一级标题之前的内容视为前置内容(封面/声明/摘要/目录), 不做格式修改
            item.setFrontMatter(!chapterStarted);
            items.add(item);
        }
        return items;
    }

    private static boolean isTocTitle(String text) {
        return text.replace(" ", "").replace("\u00A0", "").equals("目录")
                || text.replace(" ", "").replace("\u00A0", "").equals("目  录")
                || text.replace(" ", "").replace("\u00A0", "").equals("目録");
    }

    /**
     * 计算章节号: 有编号取编号(第X章/1 绪论/一、), 无编号标题按文档顺序递增
     */
    private int chapterOf(String text, int prevChapter) {
        int n = ChineseNumber.extract(text);
        if (n > 0) {
            return n;
        }
        return prevChapter + 1;
    }

    /**
     * 段落标题级别: 0=无, 1=一级, 2=二级, 3=三级
     * 优先用大纲级别(outlineLvl), 其次样式名
     */
    private int headingLevelByStyle(XWPFParagraph p) {
        try {
            CTPPr pPr = p.getCTP().getPPr();
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

    private ParagraphKind classify(String text, boolean containsImage, XWPFParagraph paragraph, RuleSet ruleSet) {
        if (text.isEmpty()) {
            return containsImage ? ParagraphKind.IMAGE : ParagraphKind.EMPTY;
        }
        if (containsImage && !isCaption(text)) {
            return ParagraphKind.IMAGE;
        }
        // 样式标题优先: 标准 heading 样式是最可靠的章节信号
        int styleLevel = headingLevelByStyle(paragraph);
        if (styleLevel == 1) {
            return ParagraphKind.HEADING1;
        }
        if (styleLevel == 2) {
            return ParagraphKind.HEADING2;
        }
        if (styleLevel == 3) {
            return ParagraphKind.HEADING3;
        }
        // 文本正则兜底(无样式但文本像标题)
        if (isDateLike(text)) {
            // 日期/年份等不识别为标题
        } else if (ruleSet.getHeading3Pattern().matcher(text).matches()) {
            return ParagraphKind.HEADING3;
        } else if (ruleSet.getHeading2Pattern().matcher(text).matches()) {
            return ParagraphKind.HEADING2;
        } else if (ruleSet.getHeading1Pattern().matcher(text).matches()) {
            return ParagraphKind.HEADING1;
        }
        if (ABSTRACT_TITLE.matcher(text).matches()) {
            return ParagraphKind.ABSTRACT_TITLE;
        }
        if (KEYWORDS.matcher(text).matches()) {
            return ParagraphKind.KEYWORDS;
        }
        if (EN_TITLE.matcher(text).matches()) {
            return ParagraphKind.EN_TITLE;
        }
        if (EN_KEYWORDS.matcher(text).matches()) {
            return ParagraphKind.EN_KEYWORDS;
        }
        if (SECTION_TITLE.matcher(text).matches()) {
            return ParagraphKind.SECTION_TITLE;
        }
        if (FIGURE_CAPTION.matcher(text).matches()) {
            return ParagraphKind.FIGURE_CAPTION;
        }
        if (TABLE_CAPTION.matcher(text).matches()) {
            return ParagraphKind.TABLE_CAPTION;
        }
        return ParagraphKind.BODY;
    }

    private boolean isCaption(String text) {
        return FIGURE_CAPTION.matcher(text).matches() || TABLE_CAPTION.matcher(text).matches();
    }

    private boolean containsImage(XWPFParagraph paragraph) {
        try {
            return paragraph.getCTP().xmlText().contains("<w:drawing");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 是否日期/年份类文本(避免被误当章节标题)
     */
    private static boolean isDateLike(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        if (java.util.regex.Pattern.matches("\\d+\\s*[年月日].*", t)) {
            return true;
        }
        if (java.util.regex.Pattern.matches("\\d{3,}", t)) {
            return true;
        }
        return false;
    }
}
