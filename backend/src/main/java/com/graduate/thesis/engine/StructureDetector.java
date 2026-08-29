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
            ParagraphKind kind = classify(text, containsImage, paragraph, doc, ruleSet); 
            // 目录结构(TOC域/PAGEREF/toc样式)一律保持原样, 不被标题/正文规则改动
            boolean tocStructure = isTocStructure(paragraph, doc);
            if (tocStructure) {
                kind = ParagraphKind.BODY;
            }
            DocItem item = new DocItem(paragraph, kind, text, containsImage);

            // 目录区检测: 只要文本是"目 录"(无论是否带样式), 即进入目录区.
            // 修复: 目录标题无 heading 样式时, 目录里的"第一章 xxx"被误识别为正文标题, 导致章节前内容被排版
            if (StructureHelper.isTocTitle(text)) {
                sawFront = true;
                inToc = true;
                kind = ParagraphKind.SECTION_TITLE;
                item = new DocItem(paragraph, kind, text, containsImage);
            } else if (inToc && !chapterStarted) {
                // 目录区结束: 遇到正文标题(heading 样式, 或匹配一级标题规则但非"页码结尾"的目录条目)
                // 目录结构(TOC域/toc样式)条目不触发关闭, 避免无页码目录条目被误判为正文标题提前关闭目录区
                if (!tocStructure && (StructureHelper.headingLevel(paragraph, doc) > 0 || isBodyChapterTitle(text, ruleSet))) {
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
                    || kind == ParagraphKind.HEADING1 || StructureHelper.isTocTitle(text))) {
                inAbstract = false;
            }

            if (kind == ParagraphKind.HEADING1) {
                if (!chapterStarted) {
                    // 排版起点: 带编号的正文章节标题, 且(有前置区段时)必须出现在摘要/目录之后.
                    // 避免封面/标题页的样式标题或签名日期被误判为第一章
                    boolean afterFront = !hasFrontStructure || sawFront;
                    // heading1 样式段落(编号在 numbering 属性、文本无数字)本身即正文章节信号,
                    // 与文本编号正则并列作为触发条件
                    if (afterFront && !StructureHelper.isTocEntry(text)
                            && (isNumberedChapter(text, ruleSet) || StructureHelper.headingLevel(paragraph, doc) == 1)) {
                        currentChapter = 1;
                        chapterStarted = true;
                        item.setChapterNo(currentChapter);
                    }
                } else {
                    currentChapter = StructureHelper.chapterNo(text, currentChapter);
                    item.setChapterNo(currentChapter);
                }
            } else if (kind == ParagraphKind.HEADING2) {
                java.util.regex.Matcher m = H2_CHAPTER.matcher(text);
                item.setChapterNo(m.find() ? Integer.parseInt(m.group(1)) : currentChapter);
            } else if (kind == ParagraphKind.HEADING3 || kind == ParagraphKind.HEADING4
                    || kind == ParagraphKind.HEADING5 || kind == ParagraphKind.FIGURE_CAPTION
                    || kind == ParagraphKind.TABLE_CAPTION || kind == ParagraphKind.IMAGE) {
                item.setChapterNo(currentChapter);
            }
            // 跟踪最近标题层级(含启发式判定后的更新)
            if (kind == ParagraphKind.HEADING1) lastHeadingLevel = 1;
            else if (kind == ParagraphKind.HEADING2) lastHeadingLevel = 2;
            else if (kind == ParagraphKind.HEADING3) lastHeadingLevel = 3;
            else if (kind == ParagraphKind.HEADING4) lastHeadingLevel = 4;
            else if (kind == ParagraphKind.HEADING5) lastHeadingLevel = 5;

            // 无编号标题启发式: 在已有标题之后, 极短且无标点的正文行 → 可能是下级标题
            // 例如 "绪论" 下面的 "研究背景" / "研究目的" 这种无编号短行
            if (kind == ParagraphKind.BODY && chapterStarted && lastHeadingLevel >= 1) {
                String trimmed = text.replaceAll("\\s+", "");
                if (isHeuristicSubHeading(trimmed, lastHeadingLevel)) {
                    int nextLevel = Math.min(lastHeadingLevel + 1, 5);
                    kind = headingKind(nextLevel);
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
        if (StructureHelper.isDateLike(text)) {
            return false;
        }
        return (ruleSet.getHeading1Pattern().matcher(text).matches() && StructureHelper.isHeadingText(text))
                || StructureHelper.isLikelyChapterTitle(text);
    }

    /**
     * 是否正文一级标题(用于结束目录区): 匹配模板一级正则或内置章节识别, 但排除以页码结尾的目录条目.
     */
    private boolean isBodyChapterTitle(String text, RuleSet ruleSet) {
        if (StructureHelper.isDateLike(text)) {
            return false;
        }
        boolean matched = (ruleSet.getHeading1Pattern().matcher(text).matches() && StructureHelper.isHeadingText(text))
                || StructureHelper.isLikelyChapterTitle(text);
        return matched && !StructureHelper.isTocEntry(text);
    }

    /**
     * 是否包含前置区段(摘要/Abstract/关键词/目录).
     * 若有, 正文第一章起点必须出现在其后, 防止封面标题/签名/日期被误判为第一章.
     */
    private boolean hasFrontStructure(XWPFDocument doc, RuleSet ruleSet) {
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
            if (StructureHelper.isTocTitle(text)
                    || ABSTRACT_TITLE.matcher(text).matches()
                    || EN_TITLE.matcher(text).matches()
                    || KEYWORDS.matcher(text).matches()
                    || EN_KEYWORDS.matcher(text).matches()) {
                return true;
            }
        }
        return false;
    }

    private ParagraphKind classify(String text, boolean containsImage, XWPFParagraph paragraph,
                                   XWPFDocument doc, RuleSet ruleSet) {
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
        if (COMMON_CHAPTER.matcher(text).matches() && !StructureHelper.isDateLike(text)) {
            return ParagraphKind.HEADING1;
        }
        // 签名/声明/日期区域保持原样(在样式判断之前拦截, 即使原文档该行带 Heading 样式也不改动)
        if (SIGNATURE.matcher(text).matches()) {
            return ParagraphKind.SECTION_TITLE;
        }
        // 样式标题优先: 标准 heading 样式是最可靠的章节信号
        int styleLevel = StructureHelper.headingLevel(paragraph, doc);
        if (styleLevel >= 1 && styleLevel <= 5) {
            return headingKind(styleLevel);
        }
        // 文本正则兜底(无样式但文本像标题); 加 isHeadingText 防护, 避免正文长句/章后承接词句子被误判为标题
        if (StructureHelper.isDateLike(text)) {
            // 日期/年份等不识别为标题
        } else if (ruleSet.getHeading5Pattern().matcher(text).matches() && StructureHelper.isHeadingText(text)) {
            return ParagraphKind.HEADING5;
        } else if (ruleSet.getHeading4Pattern().matcher(text).matches() && StructureHelper.isHeadingText(text)) {
            return ParagraphKind.HEADING4;
        } else if (ruleSet.getHeading3Pattern().matcher(text).matches() && StructureHelper.isHeadingText(text)) {
            return ParagraphKind.HEADING3;
        } else if (ruleSet.getHeading2Pattern().matcher(text).matches() && StructureHelper.isHeadingText(text)) {
            return ParagraphKind.HEADING2;
        } else if (ruleSet.getHeading1Pattern().matcher(text).matches() && StructureHelper.isHeadingText(text)) {
            return ParagraphKind.HEADING1;
        } else if (StructureHelper.isLikelyChapterTitle(text)) {
            // 内置自动匹配一级章节标题(第一章/1 绪论/一、绪论), 确保无论模板正则或样式如何都能找到排版起点
            return ParagraphKind.HEADING1;
        }
        if (StructureHelper.FIGURE_CAPTION.matcher(text).matches()) {
            return ParagraphKind.FIGURE_CAPTION;
        }
        if (StructureHelper.TABLE_CAPTION.matcher(text).matches()) {
            return ParagraphKind.TABLE_CAPTION;
        }
        // 公式段落(含 OMML 公式且无实质文字): 保持原样不套用正文/标题规则, 避免字体/缩进破坏公式
        if (containsFormula(paragraph) && text.replaceAll("\\s+", "").length() < 3) {
            return ParagraphKind.FORMULA;
        }
        return ParagraphKind.BODY;
    }

    private boolean isCaption(String text) {
        return StructureHelper.FIGURE_CAPTION.matcher(text).matches()
                || StructureHelper.TABLE_CAPTION.matcher(text).matches();
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
        if (currentLevel >= 5) return false;           // 已是五级标题，不再降级
        if (trimmed.length() < 2 || trimmed.length() > 18) return false; // 太短或太长
        if (trimmed.matches(".*[。；：！？、，）]")) return false; // 有句中/句末标点，是句子不是标题
        for (String prefix : HEURISTIC_BODY_PREFIXES) {
            if (trimmed.startsWith(prefix)) return false; // 以正文常见词开头
        }
        return true;
    }

    /** 标题级别 -> 段落类型 */
    private static ParagraphKind headingKind(int level) {
        switch (level) {
            case 1: return ParagraphKind.HEADING1;
            case 2: return ParagraphKind.HEADING2;
            case 3: return ParagraphKind.HEADING3;
            case 4: return ParagraphKind.HEADING4;
            case 5: return ParagraphKind.HEADING5;
            default: return ParagraphKind.BODY;
        }
    }

    private boolean containsImage(XWPFParagraph paragraph) {
        try {
            return paragraph.getCTP().xmlText().contains("<w:drawing");
        } catch (Exception e) {
            return false;
        }
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
