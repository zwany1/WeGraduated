package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.model.CoverConfig;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 封面生成(按桂林信息科技学院规范):
 * 学校名/题目居中, 题目三号黑体加粗, 其余三号宋体, 日期居中;
 * 后接独创性声明与版权授权说明页(声明正文小四宋体, 各另起一页).
 */
public class CoverFormatter {

    private static final String NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    private static final String DECLARATION =
            "本人郑重声明：所呈交的学位论文，是本人在导师的指导下，独立进行研究工作所取得的成果。"
            + "除文中已经注明引用的内容外，本论文不含任何其他个人或集体已经发表或撰写过的作品成果。"
            + "对本文的研究做出重要贡献的个人和集体，均已在文中以明确方式标明。"
            + "本人完全意识到本声明的法律结果由本人承担。";

    private static final String COPYRIGHT =
            "本人完全了解桂林信息科技学院关于收集、保存、使用学位论文的以下规定："
            + "学校有权采用影印、缩印、扫描、数字化或其它手段保存论文；"
            + "学校有权提供本学位论文全文或者部分内容的阅览服务；"
            + "学校有权将学位论文的全部或部分内容编入有关数据库进行检索、交流；"
            + "学校有权向国家有关部门或者机构送交论文的复印件和电子版。";

    public void apply(org.apache.poi.xwpf.usermodel.XWPFDocument doc, CoverConfig cover) {
        if (cover == null || !cover.isEnabled()) {
            return;
        }
        List<XmlObject> blocks = new ArrayList<>();
        blocks.add(para("桂林信息科技学院", "宋体", 22, true, "center", 0, false));
        blocks.add(para("毕业设计(论文)说明书", "黑体", 16, true, "center", 0, false));
        blocks.add(para("", "宋体", 16, false, "left", 0, false));
        blocks.add(para("题    目：" + nvl(cover.getTitle()), "黑体", 16, true, "left", 0, false));
        blocks.add(para("学    院：" + nvl(cover.getCollege()), "宋体", 16, false, "left", 0, false));
        blocks.add(para("专    业：" + nvl(cover.getMajor()), "宋体", 16, false, "left", 0, false));
        blocks.add(para("学生姓名：" + nvl(cover.getStudentName()), "宋体", 16, false, "left", 0, false));
        blocks.add(para("学    号：" + nvl(cover.getStudentNo()), "宋体", 16, false, "left", 0, false));
        blocks.add(para("指导教师单位：" + nvl(cover.getTeacherUnit()), "宋体", 16, false, "left", 0, false));
        blocks.add(para("指导教师：" + nvl(cover.getTeacher()), "宋体", 16, false, "left", 0, false));
        blocks.add(para("职    称：" + nvl(cover.getTeacherTitle()), "宋体", 16, false, "left", 0, false));
        blocks.add(para("", "宋体", 16, false, "left", 0, false));
        blocks.add(para("题目类型：" + nvl(cover.getTopicType()), "宋体", 16, false, "left", 0, false));
        blocks.add(para("", "宋体", 16, false, "left", 0, false));
        blocks.add(para(nvl(cover.getDate()), "宋体", 16, false, "center", 0, false));

        // 独创性声明页
        blocks.add(para("独  创  性  声  明", "黑体", 16, true, "center", 0, true));
        blocks.add(para("", "宋体", 12, false, "left", 0, false));
        blocks.add(para(DECLARATION, "宋体", 12, false, "both", 2, false));
        blocks.add(para("", "宋体", 12, false, "left", 0, false));
        blocks.add(para("", "宋体", 12, false, "left", 0, false));
        blocks.add(para("学位论文作者签名：", "宋体", 12, false, "left", 0, false));
        blocks.add(para("日期：      年    月    日", "宋体", 12, false, "left", 0, false));

        // 版权授权页
        blocks.add(para("关于学位论文版权使用授权的说明", "黑体", 16, true, "center", 0, true));
        blocks.add(para("", "宋体", 12, false, "left", 0, false));
        blocks.add(para(COPYRIGHT, "宋体", 12, false, "both", 2, false));
        blocks.add(para("", "宋体", 12, false, "left", 0, false));
        blocks.add(para("", "宋体", 12, false, "left", 0, false));
        blocks.add(para("学位论文作者签名：", "宋体", 12, false, "left", 0, false));
        blocks.add(para("日期：      年    月    日", "宋体", 12, false, "left", 0, false));
        blocks.add(para("", "宋体", 12, false, "left", 0, false));
        blocks.add(para("导师签名：", "宋体", 12, false, "left", 0, false));
        blocks.add(para("日期：      年    月    日", "宋体", 12, false, "left", 0, false));

        insertBeforeFirst(doc, blocks);
    }

    private void insertBeforeFirst(org.apache.poi.xwpf.usermodel.XWPFDocument doc, List<XmlObject> blocks) {
        if (doc.getBodyElements().isEmpty()) {
            return;
        }
        org.w3c.dom.Node target = firstNode(doc);
        if (target == null) {
            return;
        }
        for (XmlObject block : blocks) {
            org.w3c.dom.Node imported = target.getOwnerDocument().importNode(toElementNode(block), true);
            target.getParentNode().insertBefore(imported, target);
        }
    }

    private org.w3c.dom.Node firstNode(org.apache.poi.xwpf.usermodel.XWPFDocument doc) {
        for (org.apache.poi.xwpf.usermodel.IBodyElement el : doc.getBodyElements()) {
            if (el instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph) {
                return ((org.apache.poi.xwpf.usermodel.XWPFParagraph) el).getCTP().getDomNode();
            }
            if (el instanceof org.apache.poi.xwpf.usermodel.XWPFTable) {
                return ((org.apache.poi.xwpf.usermodel.XWPFTable) el).getCTTbl().getDomNode();
            }
        }
        return null;
    }

    private static org.w3c.dom.Node toElementNode(XmlObject obj) {
        org.w3c.dom.Node node = obj.getDomNode();
        if (node.getNodeType() == org.w3c.dom.Node.DOCUMENT_NODE) {
            return ((org.w3c.dom.Document) node).getDocumentElement();
        }
        return node;
    }

    private static XmlObject para(String text, String font, int sizePt, boolean bold,
                                  String align, int firstLineChars, boolean pageBreakBefore) {
        StringBuilder pPr = new StringBuilder();
        if (pageBreakBefore) {
            pPr.append("<w:pageBreakBefore/>");
        }
        if (align != null && !align.isEmpty()) {
            pPr.append("<w:jc w:val=\"").append(align).append("\"/>");
        }
        if (firstLineChars > 0) {
            pPr.append("<w:ind w:firstLineChars=\"200\" w:firstLine=\"").append(firstLineChars * 240).append("\"/>");
        }
        String rPr = "<w:rFonts w:ascii=\"" + font + "\" w:hAnsi=\"" + font + "\" w:eastAsia=\"" + font + "\"/>"
                + (bold ? "<w:b/>" : "")
                + "<w:sz w:val=\"" + (sizePt * 2) + "\"/><w:szCs w:val=\"" + (sizePt * 2) + "\"/>";
        String xml = "<w:p xmlns:w=\"" + NS + "\">"
                + (pPr.length() > 0 ? "<w:pPr>" + pPr + "</w:pPr>" : "")
                + "<w:r><w:rPr>" + rPr + "</w:rPr><w:t xml:space=\"preserve\">" + escapeXml(text) + "</w:t></w:r></w:p>";
        try {
            return XmlObject.Factory.parse(xml);
        } catch (XmlException e) {
            throw new IllegalStateException("封面XML构造失败", e);
        }
    }

    private static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
