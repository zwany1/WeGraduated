package com.graduate.thesis.engine;

import com.graduate.thesis.engine.model.RuleSet;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

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
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
            boolean containsImage = containsImage(paragraph);
            ParagraphKind kind = classify(text, containsImage, ruleSet);
            DocItem item = new DocItem(paragraph, kind, text, containsImage);

            if (kind == ParagraphKind.HEADING1) {
                currentChapter = ChineseNumber.extract(text);
                item.setChapterNo(currentChapter);
            } else if (kind == ParagraphKind.HEADING2) {
                java.util.regex.Matcher m = H2_CHAPTER.matcher(text);
                item.setChapterNo(m.find() ? Integer.parseInt(m.group(1)) : currentChapter);
            } else if (kind == ParagraphKind.HEADING3 || kind == ParagraphKind.FIGURE_CAPTION
                    || kind == ParagraphKind.TABLE_CAPTION || kind == ParagraphKind.IMAGE) {
                item.setChapterNo(currentChapter);
            }
            items.add(item);
        }
        return items;
    }

    private ParagraphKind classify(String text, boolean containsImage, RuleSet ruleSet) {
        if (text.isEmpty()) {
            return containsImage ? ParagraphKind.IMAGE : ParagraphKind.EMPTY;
        }
        if (containsImage && !isCaption(text)) {
            return ParagraphKind.IMAGE;
        }
        if (ruleSet.getHeading3Pattern().matcher(text).matches()) {
            return ParagraphKind.HEADING3;
        }
        if (ruleSet.getHeading2Pattern().matcher(text).matches()) {
            return ParagraphKind.HEADING2;
        }
        if (ruleSet.getHeading1Pattern().matcher(text).matches()) {
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
}
