package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.StructureHelper;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * 图文同段拆分: 把"图片 + 题注文字"塞在同一段落里的段落拆成
 * 「图片段」+「题注段」两段(图片独立成段, 题注紧随其后)。
 *
 * 原稿常把题注直接写在图片同一个段落里(段落高度可达 176px), 后果是:
 * 题注无法按题注规则对齐、图片与题注会被当成一整块、题注有时显示在图片上方。
 * 拆分后由结构识别重新把题注认作 FIGURE_CAPTION, 后续才能正常编号与居中。
 *
 * 注意: 拆分是在 DOM 层直接插入节点, POI 的 bodyElements 缓存不会刷新,
 * 因此调用方必须在拆分后重新打开文档(FormatEngine 会做)。
 */
public final class CaptionSplitter {

    private CaptionSplitter() {
    }

    /**
     * @return 被拆分的段落数(0 表示文档无需重载)
     */
    public static int split(XWPFDocument doc) {
        int count = 0;
        for (IBodyElement el : new ArrayList<>(doc.getBodyElements())) {
            if (!(el instanceof XWPFParagraph)) {
                continue;
            }
            XWPFParagraph p = (XWPFParagraph) el;
            String text = p.getText() == null ? "" : p.getText().trim();
            if (text.isEmpty() || !isCaptionText(text)) {
                continue;
            }
            if (hasSectionBreak(p)) {
                continue; // 带分节符的段落不动, 避免破坏分节
            }
            if (!hasDrawing(p)) {
                continue;
            }
            if (splitOne(p)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isCaptionText(String text) {
        return StructureHelper.FIGURE_CAPTION.matcher(text).matches()
                || StructureHelper.TABLE_CAPTION.matcher(text).matches();
    }

    private static boolean hasSectionBreak(XWPFParagraph p) {
        try {
            CTPPr pPr = p.getCTP().getPPr();
            return pPr != null && pPr.isSetSectPr();
        } catch (Exception e) {
            return true; // 取不到就保守跳过
        }
    }

    /** 段落直接子节点里是否含图片 */
    private static boolean hasDrawing(XWPFParagraph p) {
        return hasDrawingChild(p.getCTP().getDomNode());
    }

    private static boolean hasDrawingChild(Node pNode) {
        for (Node run : childElements(pNode)) {
            if (containsLocalName(run, "drawing") || containsLocalName(run, "pict")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 拆分单个段落: 复制段落节点, 副本只保留文字 run, 原段落只保留图片 run.
     */
    private static boolean splitOne(XWPFParagraph p) {
        Node pNode = p.getCTP().getDomNode();
        Node parent = pNode.getParentNode();
        if (parent == null) {
            return false;
        }
        Node captionNode = pNode.cloneNode(true);
        // 副本(题注段): 去掉图片 run
        for (Node run : childElements(captionNode)) {
            if (containsLocalName(run, "drawing") || containsLocalName(run, "pict")) {
                captionNode.removeChild(run);
            }
        }
        // 原段落(图片段): 去掉纯文字 run
        for (Node run : childElements(pNode)) {
            if (containsLocalName(run, "drawing") || containsLocalName(run, "pict")) {
                continue;
            }
            if (containsLocalName(run, "t") || containsLocalName(run, "br")) {
                pNode.removeChild(run);
            }
        }
        // 两侧都必须还剩 run, 否则说明拆不出来(整段只有图片或只有文字)
        if (!containsLocalName(captionNode, "t") || !hasDrawingChild(pNode)) {
            return false;
        }
        parent.insertBefore(captionNode, pNode.getNextSibling());
        return true;
    }

    private static List<Node> childElements(Node parent) {
        List<Node> out = new ArrayList<>();
        if (parent == null) {
            return out;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && "r".equals(localName(n))) {
                out.add(n);
            }
        }
        return out;
    }

    private static boolean containsLocalName(Node node, String wanted) {
        if (node == null) {
            return false;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE && wanted.equals(localName(node))) {
            return true;
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (containsLocalName(children.item(i), wanted)) {
                return true;
            }
        }
        return false;
    }

    private static String localName(Node n) {
        String ln = n.getLocalName();
        if (ln != null) {
            return ln;
        }
        String name = n.getNodeName();
        int idx = name.indexOf(':');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }
}
