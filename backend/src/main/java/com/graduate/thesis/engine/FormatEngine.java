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
import com.graduate.thesis.engine.formatter.TocFormatter;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatRule;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
            if (doc.getParagraphs().size() > 20000) {
                throw new BusinessException(400, "文档内容过多(超过 20000 段)，请拆分后重试");
            }
            progress.accept(10);

            List<DocItem> items = structureDetector.detect(doc, ruleSet);
            progress.accept(25);

            PageFormatter.apply(doc, ruleSet.getPageConfig());
            progress.accept(40);

            new AbstractFormatter().apply(doc, ruleSet, items);
            new SectionFormatter().apply(doc, items);
            progress.accept(50);

            HeadingFormatter.apply(doc, items, ruleSet);
            applyBody(doc, items, ruleSet);
            progress.accept(65);

            new CaptionFormatter().apply(doc, items, ruleSet);
            new ReferenceFormatter().apply(doc, ruleSet.getReferenceConfig());
            new TocFormatter().apply(doc, items, ruleSet);
            progress.accept(85);

            HeaderFooterFormatter.apply(doc, ruleSet.getPageConfig());
            progress.accept(95);

            // 渲染规范化: 让排版结果对浏览器渲染(docx-preview)更友好
            normalizeForRendering(doc);
            progress.accept(98);

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
            if (item.isFrontMatter() || item.isAbstractSection()) {
                continue; // 封面/目录等纯前置及摘要区段(摘要由 AbstractFormatter 处理)不改正文格式
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
            if (item.getKind() == ParagraphKind.EMPTY && !item.isFrontMatter() && !item.isAbstractSection()) {
                String pXml = item.getParagraph().getCTP().xmlText();
                if (pXml.contains("<w:br") || pXml.contains("<w:drawing")) continue;
                int pos = bodyElements.indexOf(item.getParagraph());
                if (pos >= 0) {
                    doc.removeBodyElement(pos);
                }
            }
        }
    }

    /**
     * 渲染规范化: 让排版结果对前端 docx-preview(浏览器渲染)更友好, 与 Word 打开效果保持一致:
     * 1) 相邻且格式完全一致的 run 合并为单个 run —— WPS 等文档常把文字拆成大量短 run,
     *    docx-preview 将每个 run 渲染为独立 span, 两端对齐时会在 span 之间插入巨大间距
     *    (表现为"字距被撑开/文字散架"), 合并后彻底消除;
     * 2) 给没有显式 w:sz 的 run 补充字号 —— docx-preview 只读 w:sz 而忽略 w:szCs,
     *    缺少 w:sz 的 run 渲染不可控; 字号优先取段落标记 w:sz, 缺省小四(12pt).
     * 覆盖正文段落与表格单元格内的段落.
     */
    private void normalizeForRendering(XWPFDocument doc) {
        List<XWPFParagraph> paras = new ArrayList<>();
        for (IBodyElement el : doc.getBodyElements()) {
            if (el instanceof XWPFParagraph) {
                paras.add((XWPFParagraph) el);
            } else if (el instanceof XWPFTable) {
                for (XWPFTableRow row : ((XWPFTable) el).getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        paras.addAll(cell.getParagraphs());
                    }
                }
            }
        }
        for (XWPFParagraph p : paras) {
            mergeAdjacentRuns(p);
            ensureRunSize(p);
        }
    }

    /** 合并相邻且 rPr 完全一致、且只含纯文本(无换行/制表/图片/域)的 run */
    private void mergeAdjacentRuns(XWPFParagraph p) {
        if (p == null) return;
        List<XWPFRun> runs = p.getRuns();
        for (int i = 0; i < runs.size() - 1; ) {
            XWPFRun a = runs.get(i);
            XWPFRun b = runs.get(i + 1);
            if (canMerge(a, b)) {
                mergeInto(a, b);
                p.removeRun(i + 1);
                runs = p.getRuns();
            } else {
                i++;
            }
        }
    }

    private boolean canMerge(XWPFRun a, XWPFRun b) {
        if (a == null || b == null) return false;
        CTR ca = a.getCTR();
        CTR cb = b.getCTR();
        if (hasStructure(ca) || hasStructure(cb)) return false;
        String ra = ca.isSetRPr() ? ca.getRPr().xmlText() : "";
        String rb = cb.isSetRPr() ? cb.getRPr().xmlText() : "";
        return ra.equals(rb);
    }

    /** run 是否含会影响纯文本合并的结构(换行/制表/图片/域/对象等) */
    private boolean hasStructure(CTR ctr) {
        if (ctr == null) return true;
        String x = ctr.xmlText();
        return x.contains("<w:br") || x.contains("<w:tab") || x.contains("<w:drawing")
                || x.contains("<w:fldChar") || x.contains("<w:instrText") || x.contains("<w:delText")
                || x.contains("<w:object") || x.contains("<w:pict") || x.contains("<w:footnoteReference")
                || x.contains("<w:endnoteReference");
    }

    /** 把 b 的文本并入 a, 保留 a 的格式
     *  注意: 不能用 XWPFRun.setText() —— POI 5.x 的 setText 是"追加新 w:t"而非替换, 会造成文本重复;
     *  必须在 XML 层直接改写首个 w:t 的值并清除多余的 w:t. */
    private void mergeInto(XWPFRun a, XWPFRun b) {
        CTR ca = a.getCTR();
        CTR cb = b.getCTR();
        String text = runText(ca) + runText(cb);
        if (ca.sizeOfTArray() == 0) {
            ca.addNewT().setStringValue(text);
        } else {
            ca.getTArray(0).setStringValue(text);
            while (ca.sizeOfTArray() > 1) {
                ca.removeT(1);
            }
        }
    }

    private String runText(CTR ctr) {
        StringBuilder sb = new StringBuilder();
        if (ctr != null) {
            for (CTText t : ctr.getTArray()) {
                String v = t.getStringValue();
                if (v != null) sb.append(v);
            }
        }
        return sb.toString();
    }

    /** 为没有 w:sz 的 run 补充字号(poi 高层 API), 避免 docx-preview 忽略 szCs 导致渲染异常 */
    private void ensureRunSize(XWPFParagraph p) {
        if (p == null) return;
        // 段落标记(ppr/rPr)字号: 半磅换算为 pt; 缺省小四(12pt)
        double markPt = -1;
        String pXml = p.getCTP().xmlText();
        int s = pXml.indexOf("<w:pPr>");
        if (s >= 0) {
            int e = pXml.indexOf("</w:pPr>", s);
            if (e > s) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("<w:sz w:val=\"(\\d+)\"").matcher(pXml.substring(s, e));
                if (m.find()) markPt = Double.parseDouble(m.group(1)) / 2.0;
            }
        }
        final double sizePt = markPt > 0 ? markPt : 12.0;
        for (XWPFRun r : p.getRuns()) {
            if (r.getCTR().xmlText().contains("<w:sz ")) continue; // 已有显式 w:sz
            r.setFontSize(sizePt);
        }
    }
}
