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
    private static final Pattern ABSTRACT_TITLE = Pattern.compile("^摘\\s*要\\s*[:：]?\\s*$");
    private static final Pattern KEYWORDS = Pattern.compile("^关键词\\s*[:：].*");
    private static final Pattern EN_TITLE = Pattern.compile("^\\s*Abstract\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EN_KEYWORDS = Pattern.compile("^Key\\s*words\\s*[:：].*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_TITLE = Pattern.compile(
            "^谢\\s*辞\\s*$|^参考文献\\s*$|^附\\s*录\\s*$");

    /** 签名/声明/日期区域: 保持原样, 不做任何格式修改(避免被正文缩进或标题加粗改动) */
    private static final Pattern SIGNATURE = Pattern.compile(
            ".*独创性声明.*|.*学位论文作者签名.*|.*作者签名\\s*[:：]?.*|.*指导教师签名\\s*[:：]?.*|^\\s*日\\s*期\\s*[:：]?.*|.*签名\\s*[:：]\\s*$");

    /** 无编号常见章名(绪论/引言/结论/致谢等) → 一级标题 */
    private static final Pattern COMMON_CHAPTER = Pattern.compile(
            "^\\s*(绪论|引言|前言|结论|致谢|致谢与展望|总结与展望|总结|结语|结论与展望)\\s*$");

    /** 无编号标题启发式排除前缀: 这些词开头的短句大概率是正文而非标题 */
    private static final String[] HEURISTIC_BODY_PREFIXES = {
            "本", "该", "这", "通过", "根据", "为了", "因此", "所以", "总之", "综上",
            "由", "在", "对", "为", "从", "使", "将", "被", "能", "会", "已", "可以",
            "应该", "需要", "实现", "设计", "分析", "本文", "此外", "同时", "另外",
            "然而", "但是", "并且", "以及", "以下", "如下", "以上", "首先", "其次", "最后"
    };

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
        int lastHeadingLevel = 0; // 最近一个标题的层级, 用于无编号标题启发式
        boolean chapterStarted = false;
        boolean sawFront = false;
        boolean inToc = false;
        boolean inAbstract = false;
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
            boolean containsImage = containsImage(paragraph);
            ParagraphKind kind = classify(text, containsImage, paragraph, ruleSet); 
            // 目录结构(TOC域/PAGEREF/toc样式)一律保持原样, 不被标题/正文规则改动
            boolean tocStructure = isTocStructure(paragraph, doc);
            if (tocStructure) {
                kind = ParagraphKind.BODY;
            }
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
                // 目录结构(TOC域/toc样式)条目不触发关闭, 避免无页码目录条目被误判为正文标题提前关闭目录区
                if (!tocStructure && (headingLevelByStyle(paragraph) > 0 || isBodyChapterTitle(text, ruleSet))) {
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
            // 摘要区段: 摘要/Abstract 标题触发, 遇到目录/章节标题/正文一级标题结束
            if (kind == ParagraphKind.ABSTRACT_TITLE || kind == ParagraphKind.EN_TITLE) {
                inAbstract = true;
            } else if (inAbstract && (kind == ParagraphKind.SECTION_TITLE
                    || kind == ParagraphKind.HEADING1 || isTocTitle(text))) {
                inAbstract = false;
            }

            if (kind == ParagraphKind.HEADING1) {
                if (!chapterStarted) {
                    // 排版起点: 带编号的正文章节标题, 且(有前置区段时)必须出现在摘要/目录之后.
                    // 避免封面/标题页的样式标题或签名日期被误判为第一章
                    boolean afterFront = !hasFrontStructure || sawFront;
                    // heading1 样式段落(编号在 numbering 属性、文本无数字)本身即正文章节信号,
                    // 与文本编号正则并列作为触发条件
                    if (afterFront && !isTocEntry(text)
                            && (isNumberedChapter(text, ruleSet) || headingLevelByStyle(paragraph) == 1)) {
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
            // 跟踪最近标题层级(含启发式判定后的更新)
            if (kind == ParagraphKind.HEADING1) lastHeadingLevel = 1;
            else if (kind == ParagraphKind.HEADING2) lastHeadingLevel = 2;
            else if (kind == ParagraphKind.HEADING3) lastHeadingLevel = 3;

            // 无编号标题启发式: 在已有标题之后, 极短且无标点的正文行 → 可能是下级标题
            // 例如 "绪论" 下面的 "研究背景" / "研究目的" 这种无编号短行
            if (kind == ParagraphKind.BODY && chapterStarted && lastHeadingLevel >= 1) {
                String trimmed = text.replaceAll("\\s+", "");
                if (isHeuristicSubHeading(trimmed, lastHeadingLevel)) {
                    int nextLevel = Math.min(lastHeadingLevel + 1, 3);
                    if (nextLevel == 1) kind = ParagraphKind.HEADING1;
                    else if (nextLevel == 2) kind = ParagraphKind.HEADING2;
                    else kind = ParagraphKind.HEADING3;
                    lastHeadingLevel = nextLevel;
                    item.setChapterNo(currentChapter);
                    item = new DocItem(paragraph, kind, text, containsImage);
                    item.setChapterNo(currentChapter);
                }
            }
            // 第一个正文一级标题之前的内容: 摘要区段套摘要规则, 其余(封面/声明/目录)不动
            if (inAbstract && !chapterStarted) {
                item.setAbstractSection(true);
            } else {
                item.setFrontMatter(!chapterStarted);
            }
            // 目录结构条目即使章节已开始也一律保持原样(防止目录区被提前关闭后误套用正文/标题规则)
            if (tocStructure) {
                item.setFrontMatter(true);
            }
            // 签名/声明/日期行一律保持原样, 即使章节已误开始也不被正文/标题规则改动(保留下划线/留白)
            if (SIGNATURE.matcher(text).matches()) {
                item.setFrontMatter(true);
            }
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
    public static boolean isTocEntry(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        // 目录条目以页码结尾: 行末为 1-4 位半角/全角数字(允许尾随空白).
        // 不限定前置分隔符形式, 避免全角数字/特殊点线/制表符等页码格式漏认,
        // 导致目录条目被误判为正文标题、提前关闭目录区、章节起点被误触发.
        return t.matches(".*[0-9０-９]{1,4}\\s*$");
    }

    public static boolean isTocTitle(String text) {
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
        // 含图片的段落:
        // - 纯图片(无文字或仅有少量编号) → IMAGE(保持原样不排版)
        // - 图片 + 多行/较长正文文字(如"说明表格内选中该表格后...") → BODY(文字仍需排版)
        if (containsImage) {
            if (!isCaption(text)) {
                String trimmed = text.replaceAll("\\s+", "");
                if (trimmed.length() >= 5) {
                    return ParagraphKind.BODY;
                }
                return ParagraphKind.IMAGE;
            }
            // 含题注文字 → 走后面的 FIGURE_CAPTION/TABLE_CAPTION 分支
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
        // 无编号常见章名(绪论/引言/结论/致谢等) → 一级标题
        if (COMMON_CHAPTER.matcher(text).matches() && !isDateLike(text)) {
            return ParagraphKind.HEADING1;
        }
        // 签名/声明/日期区域保持原样(在样式判断之前拦截, 即使原文档该行带 Heading 样式也不改动)
        if (SIGNATURE.matcher(text).matches()) {
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
        // 公式段落(含 OMML 公式且无实质文字): 保持原样不套用正文/标题规则, 避免字体/缩进破坏公式
        if (containsFormula(paragraph) && text.replaceAll("\\s+", "").length() < 3) {
            return ParagraphKind.FORMULA;
        }
        return ParagraphKind.BODY;
    }

    private boolean isCaption(String text) {
        return FIGURE_CAPTION.matcher(text).matches() || TABLE_CAPTION.matcher(text).matches();
    }

    /** 段落是否包含 OMML 公式 */
    private boolean containsFormula(XWPFParagraph paragraph) {
        try {
            return paragraph.getCTP().xmlText().contains("<m:oMath");
        } catch (Exception e) {
            return false;
        }
    }

    /** 无编号标题启发式: 在已有标题之后、极短行、无句中标点 → 疑似下级标题 */
    private boolean isHeuristicSubHeading(String trimmed, int currentLevel) {
        if (currentLevel >= 3) return false;           // 已经三级标题，不再降级
        if (trimmed.length() < 2 || trimmed.length() > 18) return false; // 太短或太长
        if (trimmed.matches(".*[。；：！？、，）]")) return false; // 有句中/句末标点，是句子不是标题
        for (String prefix : HEURISTIC_BODY_PREFIXES) {
            if (trimmed.startsWith(prefix)) return false; // 以正文常见词开头
        }
        return true;
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

    /**
     * 段落是否属于目录结构(Word 自动生成的目录条目): 含页码引用域(PAGEREF)或 toc 样式.
     * 用于无页码文本目录被误判为正文标题时, 仍按结构保护目录条目不被正文/标题规则改动.
     */
    public static boolean isTocStructure(XWPFParagraph p) {
        return isTocStructure(p, null);
    }

    /**
     * 段落是否属于目录结构(Word/WPS 自动生成的目录条目): 段落样式 name 以 toc 开头,
     * 或含页码引用域(PAGEREF)/_Toc 锚点超链接. WPS 的 toc 样式 styleId 为数字(如 18),
     * 仅 name 为 "toc N", 需经样式表 name 判定, 不能只看 pStyle 的 styleId.
     */
    public static boolean isTocStructure(XWPFParagraph p, XWPFDocument doc) {
        try {
            CTPPr pPr = p.getCTP().getPPr();
            if (pPr != null && pPr.isSetPStyle() && pPr.getPStyle().getVal() != null) {
                String sid = pPr.getPStyle().getVal();
                if (sid.toLowerCase().contains("toc") || isTocStyleName(doc, sid)) {
                    return true;
                }
            }
            String xml = p.getCTP().xmlText();
            if (xml != null) {
                if (xml.contains("PAGEREF")) {
                    return true;
                }
                if (xml.contains("w:anchor=\"_Toc")) {
                    return true;
                }
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    /** 查样式表: styleId 对应样式 name 是否以 toc 开头(WPS 数字 styleId 仅 name 为 "toc N") */
    private static boolean isTocStyleName(XWPFDocument doc, String styleId) {
        if (doc == null || styleId == null) {
            return false;
        }
        try {
            org.apache.poi.xwpf.usermodel.XWPFStyles styles = doc.getStyles();
            if (styles == null) {
                return false;
            }
            org.apache.poi.xwpf.usermodel.XWPFStyle st = styles.getStyle(styleId);
            if (st == null) {
                return false;
            }
            String name = st.getName();
            return name != null && name.toLowerCase().startsWith("toc");
        } catch (Exception e) {
            return false;
        }
    }
}
