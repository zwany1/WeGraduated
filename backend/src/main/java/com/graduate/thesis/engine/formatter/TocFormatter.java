package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.ParagraphKind;
import com.graduate.thesis.engine.model.RuleSet;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPrGeneral;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;

import java.math.BigInteger;
import java.util.List;

/**
 * 目录生成(按桂林信息科技学院规范):
 * 在第一个一级标题前插入"目  录"标题(三号黑体居中) + TOC 域(Word 中 F9 更新),
 * 目录行距1.5倍, 目录文字西文用 Times New Roman; 目录另起一页, 正文另起一页.
 */
public class TocFormatter {

    private static final String NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    public void apply(XWPFDocument doc, List<DocItem> items, RuleSet ruleSet) {
        XWPFParagraph firstH1 = null;
        for (DocItem item : items) {
            if (item.getKind() == ParagraphKind.HEADING1) {
                firstH1 = item.getParagraph();
                break;
            }
        }
        if (firstH1 == null) {
            return;
        }
        addTocStyles(doc);
        insertToc(doc, firstH1);
    }

    private void insertToc(XWPFDocument doc, XWPFParagraph anchor) {
        XmlObject title = parseXml(
                "<w:p xmlns:w=\"" + NS + "\"><w:pPr><w:pageBreakBefore/>"
                        + "<w:spacing w:line=\"360\" w:lineRule=\"auto\"/><w:jc w:val=\"center\"/></w:pPr>"
                        + "<w:r><w:rPr><w:rFonts w:ascii=\"黑体\" w:hAnsi=\"黑体\" w:eastAsia=\"黑体\"/>"
                        + "<w:b/><w:sz w:val=\"32\"/><w:szCs w:val=\"32\"/></w:rPr>"
                        + "<w:t>目    录</w:t></w:r></w:p>");
        XmlObject field = parseXml(
                "<w:p xmlns:w=\"" + NS + "\"><w:pPr><w:spacing w:line=\"360\" w:lineRule=\"auto\"/></w:pPr>"
                        + "<w:fldSimple w:instr=\" TOC \\o &quot;1-3&quot; \\h \\z \\u \">"
                        + "<w:r><w:rPr><w:rFonts w:ascii=\"Times New Roman\" w:hAnsi=\"Times New Roman\""
                        + " w:eastAsia=\"宋体\"/><w:sz w:val=\"24\"/></w:rPr>"
                        + "<w:t>【目录】请在 Word 中 Ctrl+A 后按 F9 更新域生成目录</w:t></w:r></w:fldSimple></w:p>");
        insertBeforeNode(anchor.getCTP().getDomNode(), title);
        insertBeforeNode(anchor.getCTP().getDomNode(), field);
        ParagraphFormatter.setPageBreakBefore(anchor);
    }

    private static void insertBeforeNode(org.w3c.dom.Node target, XmlObject newPara) {
        org.w3c.dom.Node parent = target.getParentNode();
        org.w3c.dom.Node imported = parent.getOwnerDocument().importNode(toElementNode(newPara), true);
        parent.insertBefore(imported, target);
    }

    private static org.w3c.dom.Node toElementNode(XmlObject obj) {
        org.w3c.dom.Node node = obj.getDomNode();
        if (node.getNodeType() == org.w3c.dom.Node.DOCUMENT_NODE) {
            return ((org.w3c.dom.Document) node).getDocumentElement();
        }
        return node;
    }

    private static XmlObject parseXml(String xml) {
        try {
            return XmlObject.Factory.parse(xml);
        } catch (XmlException e) {
            throw new IllegalStateException("目录XML构造失败", e);
        }
    }

    /**
     * ΪĿ¼���� toc1-3 ��ʽ: ���� Times New Roman, Ŀ¼�о�1.5��
     */
    private void addTocStyles(XWPFDocument doc) {
        XWPFStyles styles = resolveStyles(doc);
        if (styles == null) {
            return;
        }
        CTStyles cts = readCtStyles(styles);
        if (cts == null) {
            return;
        }
        addTocStyle(cts, "toc1", 28, "����");
        addTocStyle(cts, "toc2", 24, "����");
        addTocStyle(cts, "toc3", 24, "����");
    }

    private XWPFStyles resolveStyles(XWPFDocument doc) {
        XWPFStyles styles = doc.getStyles();
        if (styles != null) {
            return styles;
        }
        try {
            org.apache.poi.openxml4j.opc.OPCPackage pkg = doc.getPackage();
            org.apache.poi.openxml4j.opc.PackagePartName pn =
                    org.apache.poi.openxml4j.opc.PackagingURIHelper.createPartName("/word/styles.xml");
            if (pkg.containPart(pn)) {
                org.apache.poi.openxml4j.opc.PackagePart part = pkg.getPart(pn);
                CTStyles cts = CTStyles.Factory.parse(part.getInputStream());
                XWPFStyles stylesByPart = new XWPFStyles(part);
                setCtStylesField(stylesByPart, cts);
                return stylesByPart;
            }
        } catch (Exception e) {
            return null;
        }
        try {
            return doc.createStyles();
        } catch (Exception e) {
            return null;
        }
    }

    private void setCtStylesField(XWPFStyles styles, CTStyles cts) {
        try {
            java.lang.reflect.Field field = XWPFStyles.class.getDeclaredField("ctStyles");
            field.setAccessible(true);
            field.set(styles, cts);
        } catch (Exception ignore) {
            // ��ӳ���Ϸ��ܣ���Զ�Ҳ��.
        }
    }

    private CTStyles readCtStyles(XWPFStyles styles) {
        try {
            java.lang.reflect.Field field = XWPFStyles.class.getDeclaredField("ctStyles");
            field.setAccessible(true);
            return (CTStyles) field.get(styles);
        } catch (Exception e) {
            return null;
        }
    }

    private void addTocStyle(CTStyles cts, String styleId, int sizeHalfPoints, String cnFont) {
        for (CTStyle existing : cts.getStyleArray()) {
            if (styleId.equals(existing.getStyleId())) {
                fillTocStyle(existing, sizeHalfPoints, cnFont);
                return;
            }
        }
        CTStyle ct = cts.addNewStyle();
        ct.setStyleId(styleId);
        ct.setType(STStyleType.PARAGRAPH);
        fillTocStyle(ct, sizeHalfPoints, cnFont);
    }

    private void fillTocStyle(CTStyle ct, int sizeHalfPoints, String cnFont) {
        if (!ct.isSetName()) {
            ct.addNewName().setVal("toc " + ct.getStyleId().substring(3));
        }
        CTRPr rPr = ct.isSetRPr() ? ct.getRPr() : ct.addNewRPr();
        CTFonts fonts = rPr.sizeOfRFontsArray() > 0 ? rPr.getRFontsArray(0) : rPr.addNewRFonts();
        fonts.setAscii("Times New Roman");
        fonts.setHAnsi("Times New Roman");
        fonts.setCs("Times New Roman");
        fonts.setEastAsia(cnFont);
        if (rPr.sizeOfSzArray() == 0) {
            rPr.addNewSz().setVal(BigInteger.valueOf(sizeHalfPoints));
        }
        if (rPr.sizeOfSzCsArray() == 0) {
            rPr.addNewSzCs().setVal(BigInteger.valueOf(sizeHalfPoints));
        }
        CTPPrGeneral pPr = ct.isSetPPr() ? ct.getPPr() : ct.addNewPPr();
        if (!pPr.isSetSpacing()) {
            CTSpacing spacing = pPr.addNewSpacing();
            spacing.setLine(BigInteger.valueOf(360));
            spacing.setLineRule(STLineSpacingRule.AUTO);
        }
    }
}
