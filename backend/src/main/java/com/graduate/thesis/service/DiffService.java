package com.graduate.thesis.service;

import com.graduate.thesis.dto.DiffItem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 排版差异分析(纯 Java):
 * 用 POI 对比排版前/后 docx 段落格式差异, 输出结构化变更(字段/变更前/变更后)
 */
@Service
public class DiffService {

    /** 显示用的文本摘要长度 */
    private static final int TEXT_SUMMARY = 30;

    /** 对比排版前/后 docx 段落格式差异, 输出结构化变更(字段/变更前/变更后) */
    public List<DiffItem> diff(File original, File formatted) {
        List<DiffItem> items = new ArrayList<>();
        List<XWPFParagraph> origParas = readParas(original);
        List<XWPFParagraph> fmtParas = readParas(formatted);
        Map<String, List<XWPFParagraph>> fmtIndex = new HashMap<>();
        // 同文本段落按出现顺序逐个消费, 避免跨段落错配
        // (如两行签名行 text 相同, 原文第2行误匹配到排版后第1行, 误报缩进变化)
        Map<String, Integer> fmtCursor = new HashMap<>();
        for (XWPFParagraph p : fmtParas) {
            String t = clean(p.getText());
            if (t.isEmpty()) continue;
            fmtIndex.computeIfAbsent(t, k -> new ArrayList<>()).add(p);
        }
        List<RawDiff> raw = new ArrayList<>();
        for (XWPFParagraph op : origParas) {
            String t = clean(op.getText());
            if (t.isEmpty()) continue;
            List<XWPFParagraph> cand = fmtIndex.get(t);
            if (cand == null || cand.isEmpty()) continue;
            int cursor = fmtCursor.getOrDefault(t, 0);
            if (cursor >= cand.size()) continue; // 该文本段落已全部匹配完, 不再跨段落错配
            XWPFParagraph fp = cand.get(cursor);
            fmtCursor.put(t, cursor + 1);
            List<String[]> fields = diffFields(op, fp);
            if (!fields.isEmpty()) {
                raw.add(new RawDiff(t, fields));
            }
        }
        int idx = 0;
        for (RawDiff rd : raw) {
            idx++;
            String type = rd.fields.get(0)[0];
            items.add(new DiffItem(truncate(rd.text), type, changeDesc(rd.fields), idx));
        }
        return items;
    }

    private List<XWPFParagraph> readParas(File f) {
        List<XWPFParagraph> list = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(f))) {
            list.addAll(doc.getParagraphs());
        } catch (Exception ignore) {
            // 读取失败则跳过
        }
        return list;
    }

    /** 对比两个段落格式, 返回结构化变更列表: 每项 [字段名, 变更前, 变更后] */
    private List<String[]> diffFields(XWPFParagraph a, XWPFParagraph b) {
        List<String[]> list = new ArrayList<>();
        XWPFRun ra = firstRun(a);
        XWPFRun rb = firstRun(b);
        if (ra != null && rb != null) {
            if (!Objects.equals(ra.getFontSize(), rb.getFontSize())) {
                list.add(new String[]{"字号", displaySize(ra.getFontSize()), displaySize(rb.getFontSize())});
            }
            if (!Objects.equals(fontOf(ra), fontOf(rb))) {
                list.add(new String[]{"字体", displayFont(fontOf(ra)), displayFont(fontOf(rb))});
            }
            if (ra.isBold() != rb.isBold()) {
                list.add(new String[]{"加粗", ra.isBold() ? "是" : "否", rb.isBold() ? "是" : "否"});
            }
        }
        if (!Objects.equals(a.getAlignment(), b.getAlignment())) {
            list.add(new String[]{"对齐", displayAlign(a.getAlignment()), displayAlign(b.getAlignment())});
        }
        if (lineSpacing(a) != lineSpacing(b)) {
            list.add(new String[]{"行距", displayLineSpacing(lineSpacing(a)), displayLineSpacing(lineSpacing(b))});
        }
        if (indentFirst(a) != indentFirst(b)) {
            list.add(new String[]{"首行缩进", displayIndent(indentFirst(a)), displayIndent(indentFirst(b))});
        }
        if (spaceBefore(a) != spaceBefore(b)) {
            list.add(new String[]{"段前距", displayPt(spaceBefore(a)), displayPt(spaceBefore(b))});
        }
        if (spaceAfter(a) != spaceAfter(b)) {
            list.add(new String[]{"段后距", displayPt(spaceAfter(a)), displayPt(spaceAfter(b))});
        }
        return list;
    }

    /** 生成变更描述: "【字号】12pt(五号)→12pt(小四); 【对齐】左对齐→两端对齐" */
    private String changeDesc(List<String[]> fields) {
        StringBuilder sb = new StringBuilder();
        for (String[] f : fields) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("【").append(f[0]).append("】").append(f[1]).append(" → ").append(f[2]);
        }
        return sb.toString();
    }

    private String displaySize(Integer pt) {
        if (pt == null) return "未设置";
        String name;
        switch (pt) {
            case 9: name = "小五"; break;
            case 10: name = "五号"; break;
            case 12: name = "小四"; break;
            case 14: name = "四号"; break;
            case 15: name = "小三"; break;
            case 16: name = "三号"; break;
            case 18: name = "小二"; break;
            case 22: name = "二号"; break;
            case 24: name = "小一"; break;
            case 26: name = "一号"; break;
            default: name = ""; break;
        }
        return pt + "pt" + (name.isEmpty() ? "" : "(" + name + ")");
    }

    private String displayFont(String name) {
        return (name == null || name.trim().isEmpty()) ? "未设置" : name.trim();
    }

    private String displayAlign(Object align) {
        if (align == null) return "未设置";
        String s = String.valueOf(align).trim().toLowerCase();
        switch (s) {
            case "left": return "左对齐";
            case "center": return "居中";
            case "right": return "右对齐";
            case "both": return "两端对齐";
            default: return align.toString();
        }
    }

    private String displayLineSpacing(double val) {
        if (val == 0) return "未设置";
        if (val == (int) val) return (int) val + "倍";
        return val + "倍";
    }

    private String displayIndent(int twips) {
        if (twips <= 0) return "无";
        int chars = Math.round(twips / 240f);
        return chars + "字符(" + twips + "twips)";
    }

    private String displayPt(int twips) {
        if (twips <= 0) return "无";
        int pt = Math.round(twips / 20f);
        return pt + "pt";
    }

    private XWPFRun firstRun(XWPFParagraph p) {
        for (XWPFRun r : p.getRuns()) {
            if (r.getText(0) != null && !r.getText(0).trim().isEmpty()) return r;
        }
        return null;
    }

    private String fontOf(XWPFRun r) {
        try {
            return r.getFontFamily() != null ? r.getFontFamily() : r.getFontName();
        } catch (Exception e) {
            return null;
        }
    }

    private double lineSpacing(XWPFParagraph p) {
        try {
            return p.getSpacingBetween();
        } catch (Exception e) {
            return 0;
        }
    }

    private int indentFirst(XWPFParagraph p) {
        try {
            return p.getIndentationFirstLine();
        } catch (Exception e) {
            try {
                return p.getIndentFromLeft();
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    private int spaceBefore(XWPFParagraph p) {
        try {
            return p.getSpacingBefore();
        } catch (Exception e) {
            return 0;
        }
    }

    private int spaceAfter(XWPFParagraph p) {
        try {
            return p.getSpacingAfter();
        } catch (Exception e) {
            return 0;
        }
    }

    private String clean(String s) {
        return s == null ? "" : s.replaceAll("\\s", "");
    }

    private String truncate(String s) {
        String t = clean(s);
        return t.length() > TEXT_SUMMARY ? t.substring(0, TEXT_SUMMARY) + "…" : t;
    }

    private static class RawDiff {
        final String text;
        final List<String[]> fields;

        RawDiff(String text, List<String[]> fields) {
            this.text = text;
            this.fields = fields;
        }
    }

}