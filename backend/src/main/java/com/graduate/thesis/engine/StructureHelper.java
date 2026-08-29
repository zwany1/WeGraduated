package com.graduate.thesis.engine;

import com.graduate.thesis.engine.model.RuleSet;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.util.regex.Pattern;

/**
 * 文档结构共享工具: 供 StructureDetector / CaptionFormatter / TocFormatter 复用,
 * 消除各处重复的目录识别、标题级别、章节号提取逻辑, 避免实现分歧.
 */
public final class StructureHelper {

    private StructureHelper() {
    }

    // ===== 题注识别 =====

    public static final Pattern FIGURE_CAPTION = Pattern.compile("^图\\s*\\d+([-．.]\\d+)?\\s*(.*)");
    public static final Pattern TABLE_CAPTION = Pattern.compile("^表\\s*\\d+([-．.]\\d+)?\\s*(.*)");

    // ===== 内置一级章节标题识别(第一章/1 绪论/一、绪论) =====

    private static final Pattern CN_CHAPTER = Pattern.compile("^第[一二三四五六七八九十百千]+章\\s*.*");
    private static final Pattern NUM_CHAPTER = Pattern.compile("^\\d{1,2}[\\s、．.]\\s*[\\u4e00-\\u9fa5A-Za-z].*");
    private static final Pattern CN_NUM_CHAPTER = Pattern.compile("^[一二三四五六七八九十]{1,3}[、．.]\\s*[\\u4e00-\\u9fa5].*");

    /**
     * 是否目录标题(目 录/目録/目录, 忽略空格与不间断空格)
     */
    public static boolean isTocTitle(String text) {
        if (text == null) {
            return false;
        }
        String t = text.replace(" ", "").replace("\u00A0", "");
        return t.equals("目录") || t.equals("目録") || t.equals("目錄");
    }

    /**
     * 目录条目特征: 以页码数字结尾.
     * 收紧判定: 页码前需有 点线前导(....5) 或 空白/标点 分隔(… 5), 避免把
     * "共分为5部分" 这类正文短行误判为目录条目.
     */
    public static boolean isTocEntry(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        // 点线前导: "第一章 绪论........5"
        if (t.matches(".*\\.{2,}\\s*[0-9０-９]{1,4}\\s*$")) {
            return true;
        }
        // 空白/标点分隔 + 数字结尾: "第一章 绪论 5" / "第一章 绪论，5"
        return t.matches(".*[\\s\\u00A0，,．.、:：]\\s*[0-9０-９]{1,4}\\s*$");
    }

    /**
     * 段落标题级别: 0=非标题, 1..5=一至五级.
     * 优先大纲级别(outlineLvl), 其次样式名(含 WPS 数字 styleId: 2→H1 … 6→H5).
     */
    public static int headingLevel(XWPFParagraph p) {
        try {
            CTPPr pPr = p.getCTP().getPPr();
            if (pPr != null) {
                if (pPr.isSetOutlineLvl()) {
                    int lvl = pPr.getOutlineLvl().getVal().intValue();
                    if (lvl >= 0 && lvl <= 4) {
                        return lvl + 1;
                    }
                }
                if (pPr.isSetPStyle() && pPr.getPStyle().getVal() != null) {
                    String style = pPr.getPStyle().getVal().toLowerCase();
                    int byName = headingLevelByName(style);
                    if (byName > 0) {
                        return byName;
                    }
                    return headingLevelByStyleId(style);
                }
            }
        } catch (Exception ignore) {
        }
        return 0;
    }

    /**
     * 文档感知的标题级别: 先查样式表 styleId 对应的样式名(Heading 1/标题 1/toc 等),
     * 命中则按名称判定(避免 WPS 数字 styleId 与用户自定义样式撞号); 未命中回退到按 styleId 文本判定.
     */
    public static int headingLevel(XWPFParagraph p, org.apache.poi.xwpf.usermodel.XWPFDocument doc) {
        try {
            CTPPr pPr = p.getCTP().getPPr();
            if (pPr != null) {
                if (pPr.isSetOutlineLvl()) {
                    int lvl = pPr.getOutlineLvl().getVal().intValue();
                    if (lvl >= 0 && lvl <= 4) {
                        return lvl + 1;
                    }
                }
                if (pPr.isSetPStyle() && pPr.getPStyle().getVal() != null) {
                    String styleId = pPr.getPStyle().getVal();
                    String name = styleName(doc, styleId);
                    if (name != null) {
                        int byName = headingLevelByName(name.toLowerCase());
                        if (byName > 0) {
                            return byName;
                        }
                    }
                    return headingLevelByStyleId(styleId.toLowerCase());
                }
            }
        } catch (Exception ignore) {
        }
        return 0;
    }

    /** 样式名匹配标题级别("heading 1"/"标题 1"/"toc 1" 等) */
    private static int headingLevelByName(String s) {
        if (s == null) {
            return 0;
        }
        if (s.contains("heading5") || s.contains("标题5") || s.contains("heading 5") || s.contains("标题 5")) return 5;
        if (s.contains("heading4") || s.contains("标题4") || s.contains("heading 4") || s.contains("标题 4")) return 4;
        if (s.contains("heading3") || s.contains("标题3") || s.contains("heading 3") || s.contains("标题 3")) return 3;
        if (s.contains("heading2") || s.contains("标题2") || s.contains("heading 2") || s.contains("标题 2")) return 2;
        if (s.contains("heading1") || s.contains("标题1") || s.contains("heading 1") || s.contains("标题 1")) return 1;
        return 0;
    }

    /** WPS 数字 styleId 兜底: 2→H1 … 6→H5(仅在样式名无法判定时使用) */
    private static int headingLevelByStyleId(String style) {
        if (style == null) {
            return 0;
        }
        if (style.equals("6")) return 5;
        if (style.equals("5")) return 4;
        if (style.equals("4")) return 3;
        if (style.equals("3")) return 2;
        if (style.equals("2")) return 1;
        return 0;
    }

    /** 查样式表: styleId 对应样式的 name */
    private static String styleName(org.apache.poi.xwpf.usermodel.XWPFDocument doc, String styleId) {
        if (doc == null || styleId == null) {
            return null;
        }
        try {
            org.apache.poi.xwpf.usermodel.XWPFStyles styles = doc.getStyles();
            if (styles == null) {
                return null;
            }
            org.apache.poi.xwpf.usermodel.XWPFStyle st = styles.getStyle(styleId);
            return st == null ? null : st.getName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 是否一级标题: 样式一级, 或文本匹配一级标题正则/内置章节识别(排除日期/年份等非标题)
     */
    public static boolean isHeading1(XWPFParagraph p, String text, RuleSet ruleSet) {
        int level = headingLevel(p);
        if (level == 1) {
            return true;
        }
        if (level > 1) {
            return false;
        }
        if (isDateLike(text)) {
            return false;
        }
        if (ruleSet.getHeading1Pattern().matcher(text).matches() && isHeadingText(text)) {
            return true;
        }
        return isLikelyChapterTitle(text);
    }

    /**
     * 标题候选文本校验: 标题通常简短、不含句子标点、且"第X章"后不紧跟正文承接词.
     * 用于在标题正则匹配之外再加一层防护, 避免把
     * "第一章是绪论，主要说明…" / "第一章作为整篇论文的开头…" 这类正文句子误判为标题.
     */
    public static boolean isHeadingText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        if (t.length() > 40) {
            return false;
        }
        // 句子标点(全角逗号/句号/叹号/问号/分号): 标题极少出现, 正文句子必然出现
        if (t.matches(".*[，。！？；].*")) {
            return false;
        }
        // "第X章"后紧跟正文承接词(是/作为/主要/介绍…) → 正文句子, 如 "第一章是绪论"
        if (CN_CHAPTER_BODY.matcher(t).find()) {
            return false;
        }
        return true;
    }

    /** 正文句子特征: "第X章"后紧跟正文承接词(省略空格), 如 第一章是… / 第一章作为… / 第一章主要… */
    private static final Pattern CN_CHAPTER_BODY = Pattern.compile(
            "^第[一二三四五六七八九十百千]+章\\s*[是作为主要将会要介绍讨论围绕讲述重点本章从在该中这其内]");

    /**
     * 内置自动识别一级章节标题(第一章/1 绪论/一、绪论)
     */
    public static boolean isLikelyChapterTitle(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        if (t.length() > 40) {
            return false;
        }
        // 正文句子(第X章+承接词)不视为标题
        if (CN_CHAPTER_BODY.matcher(t).find()) {
            return false;
        }
        return CN_CHAPTER.matcher(t).matches()
                || NUM_CHAPTER.matcher(t).matches()
                || CN_NUM_CHAPTER.matcher(t).matches();
    }

    /**
     * 是否日期/年份类文本(避免被误当章节标题)
     */
    public static boolean isDateLike(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        if (Pattern.matches("\\d+\\s*[年月日].*", t)) {
            return true;
        }
        if (Pattern.matches("\\d{3,}", t)) {
            return true;
        }
        return false;
    }

    /**
     * 章节号: 有编号取编号(排除年月日), 无编号递增
     */
    public static int chapterNo(String text, int prev) {
        int n = extractChapter(text);
        if (n > 0) {
            return n;
        }
        return prev + 1;
    }

    /**
     * 提取章节号, 排除日期/年份文本(如 "2026 年 5 月 23 日" -> 0)
     */
    public static int extractChapter(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String t = text.trim();
        if (Pattern.matches("\\d+\\s*[年月日].*", t)) {
            return 0;
        }
        return ChineseNumber.extract(text);
    }

    /** 是否图表题注文本 */
    public static boolean isFigureCaptionText(String text) {
        return text != null && FIGURE_CAPTION.matcher(text.trim()).matches();
    }

    /** 是否表格题注文本 */
    public static boolean isTableCaptionText(String text) {
        return text != null && TABLE_CAPTION.matcher(text.trim()).matches();
    }
}