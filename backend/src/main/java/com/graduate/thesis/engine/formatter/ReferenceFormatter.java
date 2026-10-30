package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.ParagraphKind;
import com.graduate.thesis.engine.StructureHelper;
import com.graduate.thesis.engine.model.ReferenceConfig;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.engine.model.SectionConfig;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参考文献格式化: 标题另起一页 + 条目重排序号 + 作者截断 + 删除DOI + 悬挂缩进
 *
 * 规则(默认按学校规范, 部分可由 ReferenceConfig 配置):
 *   - 标题: 复用一级标题规则(由 SectionFormatter 统一落地), 另起一页
 *   - 条目: 中文字体(默认宋体)、西文 Times New Roman、五号
 *   - 序号: 兼容 [1] / (1) / 1. / 1、 / 1) 等编号形式, 重排时沿用文档原有编号风格
 *   - 悬挂缩进与行距: 由 ReferenceConfig.hangingIndentTwips / lineSpacing 配置
 *   - 作者: 最多保留 maxAuthors 人, 超出中文加"等", 英文加" et al"
 *   - DOI 之后的内容删除
 *
 * 作用域(重要): 文献区只覆盖"从参考文献标题起、连续的一段文献条目块"。
 * 一旦遇到标题级段落、附录/致谢等章节标题、或第一个不像文献条目的段落即结束,
 * 绝不把其后的正文当条目处理(否则会把正文多段粘成一段并吞掉图片)。
 */
public class ReferenceFormatter {

    // GB/T 7714 文献类型标识(含复合型如 J/OL, DB/OL, EB/OL, M/CD)
    private static final String TYPE_CODE = "(?:\\[[A-Z]{1,2}(?:/[A-Z]{1,2})?\\])";

    // 参考文献条目编号前缀: [1] / (1) / 1. / 1、 / 1)
    private static final Pattern REF_NUM = Pattern.compile(
            "^(?:\\[(\\d+)\\]|\\((\\d+)\\)|(\\d+)[.、)])(.*)$");
    // 是否为条目行(行首带编号)
    private static final Pattern REF_ITEM_START = Pattern.compile(
            "^\\s*(?:\\[\\d+\\]|\\(\\d+\\)|\\d+[.、)]).*");
    // 无编号文献条目特征: 含 GB/T 7714 文献类型标识, 如 [J] [M] [C] [D] [P] [S] [EB/OL]
    private static final Pattern REF_TYPE_HINT = Pattern.compile(".*" + TYPE_CODE + ".*");
    // DOI / URL
    private static final Pattern DOI_PATTERN = Pattern.compile(
            "(doi\\s*[:：]\\s*10\\.\\S+|10\\.\\d{4,}/\\S+.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_HINT = Pattern.compile("(https?://|www\\.|\\bdoi\\b|10\\.\\d{4,}/)",
            Pattern.CASE_INSENSITIVE);
    // 出版年
    private static final Pattern YEAR = Pattern.compile("(?:19|20)\\d{2}");
    // 中文作者开头: "李玉飞." / "于明春，胡增超."
    private static final Pattern CN_AUTHOR_START = Pattern.compile(
            "^[\\u4e00-\\u9fa5]{2,4}\\s*[.，,、]");
    // 英文作者开头: "Cuiyun Z ." / "Gedye R," — 单词 + 首字母缩写
    private static final Pattern EN_AUTHOR_START = Pattern.compile(
            "^[A-Z][A-Za-z'\\-]+\\s+[A-Z]\\b");
    // 起止页: ":133-135" / "：32-35" / " 121-123."
    private static final Pattern PAGES = Pattern.compile("[:：]\\s*\\d{1,5}\\s*[-–—]\\s*\\d{1,5}");
    // 行末终止标点: 出现说明上一条已经写完, 下一段不再是续行
    private static final Pattern TERMINAL = Pattern.compile(".*[。.．]\\s*$");

    /** 参考文献之后的章节标题(附录/致谢/攻读学位期间), 处理到此为止 */
    private static final Pattern AFTER_REF_SECTION =
            Pattern.compile("^(附\\s*录|致\\s*谢|攻读(硕士|博士|学位)期间|个人简历|学(术)?论文(发表|完成)情况).*$");

    /** 是否为"参考文献"标题文本(供报告统计复用) */
    public static boolean isReferenceTitle(String text) {
        String t = text == null ? "" : text.replace(" ", "").replace("\u00A0", "");
        return t.equals("参考文献") || t.equals("References");
    }

    /** 是否为文献区之后的固定章节(附录/致谢等), 供报告统计复用 */
    public static boolean isAfterRefSection(String text) {
        return text != null && AFTER_REF_SECTION.matcher(text.trim()).matches();
    }

    public void apply(XWPFDocument doc, List<DocItem> items, RuleSet ruleSet) {
        ReferenceConfig config = ruleSet.getReferenceConfig();
        if (config == null || !config.isEnabled()) {
            return;
        }
        List<IBodyElement> elements = doc.getBodyElements();
        Map<XWPFParagraph, DocItem> itemMap = indexItems(items);
        int refIdx = -1;
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) instanceof XWPFParagraph) {
                String text = ((XWPFParagraph) elements.get(i)).getText().trim();
                if (isRefTitle(text)) {
                    refIdx = i;
                    break;
                }
            }
        }
        if (refIdx < 0) {
            return;
        }

        // 2. 标题: 另起一页 + 复用一级标题规则(字号/加粗/段前后/行距), 仅对齐方式由章节配置决定
        XWPFParagraph title = (XWPFParagraph) elements.get(refIdx);
        formatTitle(title, config, ruleSet);

        // 3. 探测首个条目的编号风格, 重排时沿用
        String numberingStyle = "bracket";
        for (int i = refIdx + 1; i < elements.size(); i++) {
            if (elements.get(i) instanceof XWPFParagraph) {
                String t = ((XWPFParagraph) elements.get(i)).getText().trim();
                String style = detectNumbering(t);
                if (style != null) {
                    numberingStyle = style;
                    break;
                }
            }
        }

        // 4. 逐条处理: 条目块以"连续文献条目"为界, 遇到标题/非文献段落即结束
        int no = 1;
        int i = refIdx + 1;
        while (i < elements.size()) {
            IBodyElement el = elements.get(i);
            if (!(el instanceof XWPFParagraph)) {
                break; // 表格出现, 文献区结束
            }
            XWPFParagraph p = (XWPFParagraph) el;
            String text = p.getText() == null ? "" : p.getText().trim();
            if (text.isEmpty()) {
                i++;
                continue;
            }
            if (isSectionBoundary(p, text, itemMap)) {
                break;
            }
            if (!looksLikeCitation(text)) {
                break; // 第一个不像文献条目的段落: 文献区到此为止(正文不再被当成条目)
            }

            // 收集本条目的续行: 仅当"上一条还没写完(不以句末标点结尾)"且"本段不像新条目/不含图片公式"时才合并
            StringBuilder sb = new StringBuilder(stripNumberIfPresent(text));
            boolean open = !isTerminated(text);
            int j = i + 1;
            while (open && j < elements.size() && elements.get(j) instanceof XWPFParagraph) {
                XWPFParagraph next = (XWPFParagraph) elements.get(j);
                String nextText = next.getText() == null ? "" : next.getText().trim();
                if (nextText.isEmpty()) {
                    j++;
                    continue;
                }
                if (!isContinuation(next, nextText, itemMap)) {
                    break;
                }
                sb.append(nextText);
                setItemText(next, "", config); // 续行已并走, 清空避免内容重复
                open = !isTerminated(nextText);
                j++;
            }

            String cleaned = formatItem(sb.toString(), config);
            String label = config.isRenumber() ? formatNumber(no, numberingStyle) + "  " : "";
            setItemText(p, label + cleaned, config);
            applyHangingIndent(p, config);
            no++;
            i = Math.max(j, i + 1);
        }
    }

    /** 段落 -> 识别结果, 供作用域与续行判定复用 */
    private static Map<XWPFParagraph, DocItem> indexItems(List<DocItem> items) {
        Map<XWPFParagraph, DocItem> map = new HashMap<>();
        if (items != null) {
            for (DocItem item : items) {
                if (item.getParagraph() != null) {
                    map.put(item.getParagraph(), item);
                }
            }
        }
        return map;
    }

    /**
     * 是否文献区结束边界: 标题级段落(一至五级)、摘要/关键词/目录标题、附录/致谢等固定章节,
     * 或明确含分节符的段落。
     */
    private static boolean isSectionBoundary(XWPFParagraph p, String text,
                                             Map<XWPFParagraph, DocItem> itemMap) {
        DocItem item = itemMap.get(p);
        if (item != null) {
            switch (item.getKind()) {
                case HEADING1:
                case HEADING2:
                case HEADING3:
                case HEADING4:
                case HEADING5:
                case ABSTRACT_TITLE:
                case EN_TITLE:
                case KEYWORDS:
                case EN_KEYWORDS:
                    return true;
                default:
                    break;
            }
        }
        if (StructureHelper.isTocTitle(text) || AFTER_REF_SECTION.matcher(text).matches()) {
            return true;
        }
        // 无识别结果的兜底: 有标题样式/大纲级别, 或形如章标题
        return StructureHelper.headingLevel(p) > 0 || StructureHelper.isLikelyChapterTitle(text);
    }

    /** 能否作为上一条的续行(独立文献条目/标题/带图片或公式的段落一律不合并) */
    private static boolean isContinuation(XWPFParagraph p, String text,
                                          Map<XWPFParagraph, DocItem> itemMap) {
        if (containsObject(p)) {
            return false; // 含图片/公式的段落绝不并入文献条目(否则图片会被丢弃)
        }
        if (REF_ITEM_START.matcher(text).matches() || looksLikeCitation(text)) {
            return false;
        }
        DocItem item = itemMap.get(p);
        if (item != null && (item.getKind() != ParagraphKind.BODY || item.isFrontMatter())) {
            return false;
        }
        // 正文长句、带句读的段落不并入
        if (text.length() > 120 || text.matches(".*[。；！？].*")) {
            return false;
        }
        return true;
    }

    /** 段落是否含图片/公式/对象 */
    private static boolean containsObject(XWPFParagraph p) {
        try {
            String xml = p.getCTP().xmlText();
            return xml.contains("<w:drawing") || xml.contains("<w:pict")
                    || xml.contains("<w:object") || xml.contains("<m:oMath");
        } catch (Exception e) {
            return true; // 取不到 XML 时保守处理: 不合并
        }
    }

    /** 该段落是否已写完(以句末标点结尾) */
    private static boolean isTerminated(String text) {
        return text != null && TERMINAL.matcher(text).matches();
    }

    /** 去掉行首编号, 保留正文 */
    private static String stripNumberIfPresent(String text) {
        Matcher m = REF_NUM.matcher(text);
        if (m.matches() && m.group(4) != null) {
            return m.group(4).trim();
        }
        return text.trim();
    }

    /**
     * 是否像一条文献著录(打分判定, 阈值 3):
     * 文献类型标识 +2、DOI/URL +2、出版年 +1、作者开头 +1、起止页 +1、行首编号 +1。
     * 用于把"参考文献标题之后的正文段落"挡在文献区之外。
     */
    public static boolean looksLikeCitation(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        if (t.length() < 12) {
            return false;
        }
        int score = 0;
        if (REF_TYPE_HINT.matcher(t).matches()) {
            score += 2;
        }
        if (URL_HINT.matcher(t).find()) {
            score += 2;
        }
        if (YEAR.matcher(t).find()) {
            score += 1;
        }
        if (CN_AUTHOR_START.matcher(t).find() || EN_AUTHOR_START.matcher(t).find()) {
            score += 1;
        }
        if (PAGES.matcher(t).find()) {
            score += 1;
        }
        if (REF_ITEM_START.matcher(t).matches()) {
            score += 1;
        }
        return score >= 3;
    }

    /** 识别条目编号风格, 非条目行返回 null */
    private static String detectNumbering(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        if (text.matches("^\\s*\\[\\d+\\].*")) return "bracket";
        if (text.matches("^\\s*\\(\\d+\\).*")) return "paren";
        if (text.matches("^\\s*\\d+\\..*")) return "dot";
        if (text.matches("^\\s*\\d+、.*")) return "dun";
        if (text.matches("^\\s*\\d+\\).*")) return "closeParen";
        return null;
    }

    /** 按编号风格格式化序号 */
    private static String formatNumber(int no, String style) {
        switch (style) {
            case "paren": return "(" + no + ")";
            case "dot": return no + ".";
            case "dun": return no + "、";
            case "closeParen": return no + ")";
            default: return "[" + no + "]";
        }
    }

    /**
     * 条目内容处理: 作者截断 + 删除DOI
     */
    private String formatItem(String content, ReferenceConfig config) {
        String s = content;
        // 删除 DOI 及其后内容
        if (config.isRemoveDoi()) {
            Matcher dm = DOI_PATTERN.matcher(s);
            if (dm.find()) {
                s = s.substring(0, dm.start());
            }
            // 去掉末尾残留的标点/空格
            s = s.replaceAll("[,，;；。\\s]+$", "");
        }
        // 作者截断
        if (config.getMaxAuthors() > 0) {
            s = truncateAuthors(s, config.getMaxAuthors());
        }
        return s.trim();
    }

    /**
     * 作者截断: 最多保留 maxAuthors 人, 超出的删掉并加"等"/" et al"
     */
    private String truncateAuthors(String s, int max) {
        int cut = findAuthorEnd(s);
        if (cut <= 0) {
            return s;
        }
        String authorPart = s.substring(0, cut);
        String rest = s.substring(cut);
        boolean isChinese = containsCJK(authorPart);
        String[] authors = splitAuthors(authorPart);
        if (authors.length <= max) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            if (i > 0) sb.append(", ");
            sb.append(authors[i]);
        }
        sb.append(isChinese ? "等" : " et al");
        // rest 若以 "et al" 开头则去掉(英文截断时 authorPart 已含 et al)
        String tail = rest.trim();
        if (!isChinese && tail.toLowerCase().startsWith("et al")) {
            tail = tail.substring(5).trim();
        }
        return sb.toString() + (tail.isEmpty() ? "" : (tail.startsWith(",") || tail.startsWith("，") ? " " + tail.substring(1).trim() : " " + tail));
    }

    private boolean containsCJK(String s) {
        for (char c : s.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) return true;
        }
        return false;
    }

    private String[] splitAuthors(String authorPart) {
        // 中文: 按顿号/逗号; 英文: 按逗号 (英文作者 "Gedye R, Smith F, Westaway K," 逗号分隔)
        String[] parts = authorPart.split("[,，、]");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                list.add(t);
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * 找作者部分结束位置:
     * 1) 遇到 GB/T 7714 文献类型标识 [M]/[J]/[D]/[R]/[EB/OL] 等 之前(兼容 中文"作者. 标题" 与 英文"Author. Title")
     * 2) 无标识时(英文期刊无类型码): 到 "et al" 或 首个大写字母开头的标题词 之前
     */
    private int findAuthorEnd(String s) {
        Matcher m = Pattern.compile(TYPE_CODE).matcher(s);
        if (m.find()) {
            int start = m.start();
            int dot = s.lastIndexOf('.', start);
            if (dot >= 0 && dot < start) {
                return dot + 1;
            }
            return start;
        }
        // 无类型标识: 找 "et al" 位置
        int etAl = s.toLowerCase().indexOf("et al");
        if (etAl >= 0) {
            return etAl;
        }
        // 找第一个 "词.大写" 模式(标题词前), 例如 "Synthesis.Tetrahedron Lett"
        Matcher cap = Pattern.compile("\\.\\s*[A-Z][a-z]{2,}").matcher(s);
        if (cap.find() && cap.start() > 2) {
            return cap.start() + 1;
        }
        return 0;
    }

    /**
     * 标题: 复用一级标题规则的字号/加粗/段前后/行距, 只保留"另起一页 + 各自对齐方式"的差异。
     * 标题文字优先用 ReferenceConfig 自定义值, 未自定义时沿用章节配置里的"参考文献"字样。
     */
    private void formatTitle(XWPFParagraph p, ReferenceConfig config, RuleSet ruleSet) {
        SectionConfig c = ruleSet.getSectionConfig();
        String want = config.getTitle();
        if (want == null || want.trim().isEmpty() || "参考文献".equals(want.trim())) {
            want = c.getReferences();
        }
        SectionFormatter.formatSectionTitle(p, want, SectionFormatter.alignOf(c.getReferencesAlign()),
                ruleSet.rule("heading1"), c);
    }

    private void setItemText(XWPFParagraph p, String text, ReferenceConfig config) {
        // 清空原 run
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }
        XWPFRun run = p.createRun();
        run.setText(text);
        // 中文 eastAsia, 西文 Times New Roman
        TextFormatter.setFont(run, config.getItemFont(), config.getItemFontLatin());
        run.setFontSize(config.getItemFontSize());
    }

    /**
     * 悬挂缩进: 序号与文字空两格, 换行第二行对齐序号后(缩进宽度与行距由配置控制)
     */
    private void applyHangingIndent(XWPFParagraph p, ReferenceConfig config) {
        p.setAlignment(ParagraphAlignment.LEFT);
        int indent = config.getHangingIndentTwips() > 0 ? config.getHangingIndentTwips() : 640;
        p.setIndentationLeft(indent);
        p.setFirstLineIndent(-indent);
        p.setSpacingBetween(config.getLineSpacing());
    }

    private boolean isRefTitle(String text) {
        return isReferenceTitle(text);
    }
}
