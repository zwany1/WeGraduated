package com.graduate.thesis.engine;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.engine.formatter.AbstractFormatter;
import com.graduate.thesis.engine.formatter.CaptionFormatter;
import com.graduate.thesis.engine.formatter.HeaderFooterFormatter;
import com.graduate.thesis.engine.formatter.HeadingFormatter;
import com.graduate.thesis.engine.formatter.PageFormatter;
import com.graduate.thesis.engine.formatter.ParagraphFormatter;
import com.graduate.thesis.engine.formatter.ReferenceFormatter;
import com.graduate.thesis.engine.formatter.SectionFormatter;
import com.graduate.thesis.engine.formatter.TextFormatter;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatRule;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 排版引擎: 读取规则 -> 分析文档 -> 修改格式 -> 生成新文档
 */
@Service
public class FormatEngine {

    private final StructureDetector structureDetector = new StructureDetector();

    /**
     * 执行排版
     *
     * @param source   源 docx
     * @param ruleSet  规则集
     * @param progress 进度回调(0-100)
     * @return 排版后的临时文件
     */
    public File format(File source, RuleSet ruleSet, IntConsumer progress) {
        File temp = null;
        try {
            temp = File.createTempFile("thesis_format_", ".docx");
        } catch (IOException e) {
            throw new BusinessException(500, "创建临时文件失败");
        }
        // 放宽 ZIP 压缩比限制: 论文可能内嵌字体(odttf), 压缩率极高被误判为炸弹
        ZipSecureFile.setMinInflateRatio(0.001);
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(source))) {
            // 超大文档保护: 段落数过多说明文档极其复杂, 避免排版时间过长/内存溢出
            if (doc.getParagraphs().size() > 8000) {
                throw new BusinessException(400, "文档内容过多(超过 8000 段)，请拆分后重试");
            }
            progress.accept(10);

            List<DocItem> items = structureDetector.detect(doc, ruleSet);
            progress.accept(25);

            PageFormatter.apply(doc, ruleSet.getPageConfig());
            progress.accept(40);

            new AbstractFormatter().apply(doc, ruleSet, items);
            new SectionFormatter().apply(doc);
            progress.accept(50);

            HeadingFormatter.apply(doc, items, ruleSet);
            applyBody(doc, items, ruleSet);
            progress.accept(65);

            new CaptionFormatter().apply(doc, items, ruleSet);
            new ReferenceFormatter().apply(doc, ruleSet.getReferenceConfig());
            progress.accept(85);

            HeaderFooterFormatter.apply(doc, ruleSet.getPageConfig());
            progress.accept(95);

            try (FileOutputStream fos = new FileOutputStream(temp)) {
                doc.write(fos);
            }
            progress.accept(100);
            return temp;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, friendlyError(e));
        }
    }

    /** 将排版异常转为用户可读的错误信息 */
    private String friendlyError(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String cls = cause.getClass().getSimpleName();
        String msg = cause.getMessage() == null ? "" : cause.getMessage();
        if (cls.contains("NullPointer")) {
            return "排版失败：模板配置不完整或文档存在特殊格式，请检查模板规则是否齐全后重试";
        }
        if (cls.contains("OfficeXmlFileException") || cls.contains("NotOfficeXmlFileException")
                || cls.contains("EncryptedDocumentException") || cls.contains("IllegalArgumentException")
                && msg.toLowerCase().contains("zip")) {
            return "排版失败：文件不是有效的 Word 文档，请确认文件未被损坏或不是其他格式改名";
        }
        if (cls.contains("OutOfMemory")) {
            return "排版失败：文档过大或过于复杂，超出处理能力，请尝试拆分文档";
        }
        if (cls.contains("InvalidFormatException")) {
            return "排版失败：Word 文档结构异常，请尝试用 Word 另存为 .docx 后重试";
        }
        String shortMsg = msg.length() > 120 ? msg.substring(0, 120) + "..." : msg;
        return "排版失败：处理文档时发生错误" + (shortMsg.isEmpty() ? "" : "（" + cls + ": " + shortMsg + "）");
    }

    private void applyBody(XWPFDocument doc, List<DocItem> items, RuleSet ruleSet) {
        FormatRule bodyRule = ruleSet.rule("body");
        if (bodyRule.getBold() == null) bodyRule.setBold(false);
        NumberUnifier.Style style = NumberUnifier.detectStyle(items);

        for (DocItem item : items) {
            if (item.isFrontMatter()) {
                continue; // 封面/摘要/目录等前置内容不改正文格式
            }
            if (item.getKind() == ParagraphKind.BODY) {
                NumberUnifier.apply(item.getParagraph(), style);
                TextFormatter.apply(item.getParagraph(), bodyRule);
                ParagraphFormatter.apply(item.getParagraph(), bodyRule);
            }
        }

        // 第二遍: 移除空段落(含分页符/图片/前置内容的除外, 第一章前保持原样)
        List<org.apache.poi.xwpf.usermodel.IBodyElement> bodyElements = doc.getBodyElements();
        for (int i = items.size() - 1; i >= 0; i--) {
            DocItem item = items.get(i);
            if (item.getKind() == ParagraphKind.EMPTY && !item.isFrontMatter()) {
                String pXml = item.getParagraph().getCTP().xmlText();
                if (pXml.contains("<w:br") || pXml.contains("<w:drawing")) continue;
                int pos = bodyElements.indexOf(item.getParagraph());
                if (pos >= 0) {
                    doc.removeBodyElement(pos);
                }
            }
        }
    }
}
