package com.graduate.thesis.engine;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/**
 * 文档结构识别结果项
 */
public class DocItem {

    private final XWPFParagraph paragraph;
    private final ParagraphKind kind;
    private final String text;
    /** 所在章节号(从一级标题提取, 0 表示未识别到章节) */
    private int chapterNo;
    /** 该段是否包含图片 */
    private final boolean containsImage;
    /** 是否为前置内容(第一个正文一级标题之前, 如封面/声明/摘要/目录) */
    private boolean frontMatter;

    public DocItem(XWPFParagraph paragraph, ParagraphKind kind, String text, boolean containsImage) {
        this.paragraph = paragraph;
        this.kind = kind;
        this.text = text;
        this.containsImage = containsImage;
    }

    public XWPFParagraph getParagraph() {
        return paragraph;
    }

    public ParagraphKind getKind() {
        return kind;
    }

    public String getText() {
        return text;
    }

    public int getChapterNo() {
        return chapterNo;
    }

    public void setChapterNo(int chapterNo) {
        this.chapterNo = chapterNo;
    }

    public boolean isContainsImage() {
        return containsImage;
    }

    public boolean isFrontMatter() {
        return frontMatter;
    }

    public void setFrontMatter(boolean frontMatter) {
        this.frontMatter = frontMatter;
    }
}
