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

    // ===== 内置一级章节标题识别(不依赖文档样式与模板正则, 自动匹配"第一章"作为排版起点) =====
    /** 第一章 绪论 / 第二章 … */
    private static final Pattern CN_CHAPTER = Pattern.compile("^第[一二三四五六七八九十百千]+章\\s*.*");
    /** 1 绪论 / 1、绪论 / 1. 绪论 (数字+分隔符+中文/字母, 且整行简短) */
    private static final Pattern NUM_CHAPTER = Pattern.compile("^\\d{1,2}[\\s、．.]\\s*[\\u4e00-\\u9fa5A-Za-z].*");
    /** 一、绪论 / 一 绪论 (中文数字序号+分隔符+中文) */
    private static final Pattern CN_NUM_CHAPTER = Pattern.compile("^[一二三四五六七八九十]{1,3}[、．.]\\s*[\\u4e00-\\u9fa5].*");

    /**
     * 识别文档结构
     */
    public List<DocItem> detect(XWPFDocument doc, RuleSet ruleSet) {
        // 是否存在前置区段(摘要/Abstract/关键词/目录): 若存在, 排版起点(正文第一章)必须出现在它们之后,
        // 避免封面标题/签名/日期等被误判为第一章, 导致章节前内容被排版
        boolean hasFrontStructure = hasFrontStructure(doc, ruleSet);
        List<DocItem> items = new ArrayList<>();
        int currentChapter = 0;
        boolean chapterStarted = false;
        boolean sawFront = false;
        boolean inToc = false;
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
            boolean containsImage = containsImage(paragraph);
            ParagraphKind kind = classify(text, containsImage, paragraph, ruleSet);
            DocItem item = new DocItem(paragraph, kind, text, containsImage);

            // 目录区检测: 只要文本是"目 录"(无论是否带样式), 即进入目录区.
            // 修复: 目录标题无 heading 样式时, 目录里的"第一章 xxx"被误识别为正文标题, 导致章节前内容被排版
            if (isTocTitle(text)) {
                sawFront = true;
                inToc = true;
                kind = ParagraphKind.SECTION_TITLE;
                item = new DocItem(paragraph, kind, text, containsImage);
            } else if (inToc && !chapterStarted) {
                // 目录区结束: 遇到正文标题(heading 样式, 或匹配一级标题规则但非"页码结尾"的目录条目)
                if (headingLevelByStyle(paragraph) > 0 || isBodyChapterTitle(text, ruleSet)) {
                    inToc = false;
                } else {
                    kind = ParagraphKind.BODY; // 目录条目, 不识别为标题
                    item = new DocItem(paragraph, kind, text, containsImage);
                }
            }
            // 摘要/Abstract/关键词 视为前置区段标记(之后才允许出现正文第一章)
            if (kind == ParagraphKind.ABSTRACT_TITLE || kind == ParagraphKind.EN_TITLE
                    || kind == ParagraphKind.KEYWORDS || kind == ParagraphKind.EN_KEYWORDS) {
                sawFront = true;
            }

            if (kind == ParagraphKind.HEADING1) {
                if (!chapterStarted) {
                    // 排版起点: 带编号的正文章节标题, 且(有前置区段时)必须出现在摘要/目录之后.
                    // 避免封面/标题页的样式标题或签名日期被误判为第一章
                    boolean afterFront = !hasFrontStructure || sawFront;
                    if (afterFront && isNumberedChapter(text, ruleSet)) {
                        currentChapter = 1;
                        chapterStarted = true;
                        item.setChapterNo(currentChapter);
                    }
                } else {
                    currentChapter = chapterOf(text, currentChapter);
                    item.setChapterNo(currentChapter);
                }
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

    /**
     * 是否带编号的一级章节标题(用于触发排版起点): 匹配模板一级正则或内置章节识别, 排除日期/年份.
     */
    private boolean isNumberedChapter(String text, RuleSet ruleSet) {
        if (isDateLike(text)) {
            return false;
        }
        return ruleSet.getHeading1Pattern().matcher(text).matches() || isLikelyChapterTitle(text);
    }

    /**
     * 是否正文一级标题(用于结束目录区): 匹配模板一级正则或内置章节识别, 但排除以页码结尾的目录条目.
     */
    private boolean isBodyChapterTitle(String text, RuleSet ruleSet) {
        if (isDateLike(text)) {
            return false;
        }
        boolean matched = ruleSet.getHeading1Pattern().matcher(text).matches() || isLikelyChapterTitle(text);
        return matched && !isTocEntry(text);
    }

    /**
     * 目录条目特征: 以页码数字结尾(如 "第一章 绪论........5" / "第一章 绪论 5"), 与正文标题区分.
     */
    private static boolean isTocEntry(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        return t.matches(".*\\.{2,}\\s*\\d{1,4}\\s*$")      // 点线引导 + 页码
                || t.matches(".*[\\s\\u00A0，,．.、]\\d{1,4}\\s*$"); // 空格/标点 + 页码
    }

    private static boolean isTocTitle(String text) {
        return text.replace(" ", "").replace("\u00A0", "").equals("目录")
                || text.replace(" ", "").replace("\u00A0", "").equals("目  录")
                || text.replace(" ", "").replace("\u00A0", "").equals("目録");
    }

    /**
     * 是否包含前置区段(摘要/Abstract/关键词/目录).
     * 若有, 正文第一章起点必须出现在其后, 防止封面标题/签名/日期被误判为第一章.
     */
    private boolean hasFrontStructure(XWPFDocument doc, RuleSet ruleSet) {
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
            if (isTocTitle(text)
                    || ABSTRACT_TITLE.matcher(text).matches()
                    || EN_TITLE.matcher(text).matches()
                    || KEYWORDS.matcher(text).matches()
                    || EN_KEYWORDS.matcher(text).matches()) {
                return true;
            }
        }
        return false;
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
        // 前置/固定章节标题优先于样式判断: 避免"摘 要/Abstract/谢辞/参考文献"等带 Heading 样式的标题
        // 被误判成正文一级标题, 导致第一章之前的封面/摘要/目录被当作正文排版
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
        } else if (isLikelyChapterTitle(text)) {
            // 内置自动匹配一级章节标题(第一章/1 绪论/一、绪论), 确保无论模板正则或样式如何都能找到排版起点
            return ParagraphKind.HEADING1;
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

    /**
     * 内置自动识别一级章节标题: 不依赖文档样式与模板正则.
     * 支持 "第一章 绪论" / "1 绪论" / "1、绪论" / "一、绪论" 等常见形式,
     * 确保能找到排版起点(第一章), 其之前内容一律作为前置内容不排版.
     */
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
}
