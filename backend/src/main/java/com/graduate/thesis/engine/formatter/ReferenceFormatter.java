package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.model.ReferenceConfig;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参考文献格式化: 标题另起一页 + 条目重排序号 + 作者截断 + 删除DOI + 悬挂缩进
 *
 * 规则(默认按学校规范, 部分可由 ReferenceConfig 配置):
 *   - 标题: 黑体四号、顶格、另起一页
 *   - 条目: 中文字体(默认宋体)、西文 Times New Roman、五号
 *   - 序号: 兼容 [1] / (1) / 1. / 1、 / 1) 等编号形式, 重排时沿用文档原有编号风格
 *   - 悬挂缩进与行距: 由 ReferenceConfig.hangingIndentTwips / lineSpacing 配置
 *   - 作者: 最多保留 maxAuthors 人, 超出中文加"等", 英文加" et al"
 *   - DOI 之后的内容删除
 */
public class ReferenceFormatter {

    // GB/T 7714 文献类型标识(含复合型如 J/OL, DB/OL, EB/OL, M/CD)
    private static final String TYPE_CODE = "(?:\\[[A-Z]{1,2}(?:/[A-Z]{1,2})?\\])";

    // 参考文献条目编号前缀: [1] / (1) / 1. / 1、 / 1)
    private static final Pattern REF_NUM = Pattern.compile(
            "^(?:\\[(\\d+)\\]|\\((\\d+)\\)|(\\d+)[.、)])(.*)$");
    // 是否为条目行(行首带编号)
    private static final Pattern REF_ITEM_START = Pattern.compile(
            "^\\s*(?:\\[\\d+\\]|\\(\\d+\\)|\\d+[.、)])\\s*");
    // DOI 匹配
    private static final Pattern DOI_PATTERN = Pattern.compile(
            "(doi\\s*[:：]\\s*10\\.\\S+|10\\.\\d{4,}/\\S+.*)$", Pattern.CASE_INSENSITIVE);

    public void apply(XWPFDocument doc, ReferenceConfig config) {
        if (config == null || !config.isEnabled()) {
            return;
        }
        // 1. 找"参考文献"标题段
        List<IBodyElement> elements = doc.getBodyElements();
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

        // 2. 标题: 另起一页 + 黑体四号顶格
        XWPFParagraph title = (XWPFParagraph) elements.get(refIdx);
        addPageBreakBefore(title);
        formatTitle(title, config);

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

        // 4. 处理标题后的条目(遇到附录/致谢等后续章节标题即停止, 避免误伤其内容)
        int no = 1;
        for (int i = refIdx + 1; i < elements.size(); i++) {
            IBodyElement el = elements.get(i);
            if (!(el instanceof XWPFParagraph)) {
                continue;
            }
            XWPFParagraph p = (XWPFParagraph) el;
            String text = p.getText() == null ? "" : p.getText().trim();
            if (text.isEmpty()) {
                continue;
            }
            if (AFTER_REF_SECTION.matcher(text).matches()) {
                break;
            }
            Matcher m = REF_NUM.matcher(text);
            if (!m.matches()) {
                continue;
            }
            String content = (m.group(4) == null ? "" : m.group(4)).trim();
            // 若后续段落是该条目的续行(无序号), 追加并清空续行段, 避免排版后内容重复
            StringBuilder sb = new StringBuilder(content);
            while (i + 1 < elements.size() && elements.get(i + 1) instanceof XWPFParagraph) {
                String next = ((XWPFParagraph) elements.get(i + 1)).getText().trim();
                if (next.isEmpty() || REF_ITEM_START.matcher(next).matches()
                        || AFTER_REF_SECTION.matcher(next).matches()) {
                    break;
                }
                sb.append(next);
                i++;
                setItemText((XWPFParagraph) elements.get(i), "", config);
            }
            String cleaned = formatItem(sb.toString(), config);
            String label = config.isRenumber() ? formatNumber(no, numberingStyle) + "  " : "";
            setItemText(p, label + cleaned, config);
            applyHangingIndent(p, config);
            no++;
        }
    }

    /** 参考文献之后的章节标题(附录/致谢/攻读学位期间), 处理到此为止 */
    private static final Pattern AFTER_REF_SECTION =
            Pattern.compile("^(附\\s*录|致\\s*谢|攻读(硕士|博士|学位)期间|个人简历|学(术)?论文(发表|完成)情况).*$");

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

    private void formatTitle(XWPFParagraph p, ReferenceConfig config) {
        setParagraphText(p, config.getTitle());
        for (XWPFRun run : p.getRuns()) {
            TextFormatter.setFont(run, config.getTitleFont(), "Times New Roman");
            run.setFontSize(config.getTitleFontSize());
            run.setBold(true);
        }
        // 顶格: 左对齐、无缩进
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setIndentationLeft(0);
        p.setFirstLineIndent(0);
        p.setSpacingAfter(12);
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

    private void setParagraphText(XWPFParagraph p, String text) {
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }
        p.createRun().setText(text);
    }

    private void addPageBreakBefore(XWPFParagraph p) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr pPr =
                p.getCTP().isSetPPr() ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
        if (!pPr.isSetPageBreakBefore()) {
            pPr.addNewPageBreakBefore();
        }
    }

    private boolean isRefTitle(String text) {
        String t = text == null ? "" : text.replace(" ", "").replace("\u00A0", "");
        return t.equals("参考文献") || t.equals("References");
    }
}