package com.graduate.thesis.engine;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.engine.formatter.AbstractFormatter;
import com.graduate.thesis.engine.formatter.CaptionFormatter;
import com.graduate.thesis.engine.formatter.CoverFormatter;
import com.graduate.thesis.engine.formatter.HeaderFooterFormatter;
import com.graduate.thesis.engine.formatter.HeadingFormatter;
import com.graduate.thesis.engine.formatter.PageFormatter;
import com.graduate.thesis.engine.formatter.ParagraphFormatter;
import com.graduate.thesis.engine.formatter.SectionFormatter;
import com.graduate.thesis.engine.formatter.TextFormatter;
import com.graduate.thesis.engine.formatter.TocFormatter;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatRule;
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
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(source))) {
            progress.accept(10);

            List<DocItem> items = structureDetector.detect(doc, ruleSet);
            progress.accept(25);

            PageFormatter.apply(doc, ruleSet.getPageConfig());
            progress.accept(32);

            new CoverFormatter().apply(doc, ruleSet.getCoverConfig());
            progress.accept(40);

            new AbstractFormatter().apply(doc, ruleSet);
            new SectionFormatter().apply(doc);
            progress.accept(50);

            HeadingFormatter.apply(doc, items, ruleSet);
            applyBody(doc, items, ruleSet);
            progress.accept(65);

            new CaptionFormatter().apply(doc, items, ruleSet);
            if (ruleSet.isGenerateToc()) {
                new TocFormatter().apply(doc, items, ruleSet);
            }
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
            throw new BusinessException(500, "文档处理失败: " + e.getMessage());
        }
    }

    private void applyBody(XWPFDocument doc, List<DocItem> items, RuleSet ruleSet) {
        FormatRule bodyRule = ruleSet.rule("body");
        if (bodyRule.getBold() == null) bodyRule.setBold(false);
        NumberUnifier.Style style = NumberUnifier.detectStyle(items);

        for (DocItem item : items) {
            if (item.getKind() == ParagraphKind.BODY) {
                NumberUnifier.apply(item.getParagraph(), style);
                TextFormatter.apply(item.getParagraph(), bodyRule);
                ParagraphFormatter.apply(item.getParagraph(), bodyRule);
            }
        }

        // 第二遍: 移除空段落(含分页符/图片的除外)
        List<org.apache.poi.xwpf.usermodel.IBodyElement> bodyElements = doc.getBodyElements();
        for (int i = items.size() - 1; i >= 0; i--) {
            DocItem item = items.get(i);
            if (item.getKind() == ParagraphKind.EMPTY) {
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
