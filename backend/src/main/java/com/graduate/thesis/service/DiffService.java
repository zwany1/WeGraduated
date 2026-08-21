package com.graduate.thesis.service;

import com.graduate.thesis.dto.DiffItem;
import org.apache.poi.openxml4j.util.ZipSecureFile;
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
        // 放宽 ZIP 压缩比限制: 论文 docx 可能高压缩(内嵌字体), 被误判为炸弹导致读取失败(diff 全空)
        ZipSecureFile.setMinInflateRatio(0.001);
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
            items.add(new DiffItem(truncate(rd.text), type, changeDesc(rd.fields), idx, 0, 0.0, 0.05));
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
                list.add(new String[]{"字号", display(ra.getFontSize()), display(rb.getFontSize())});
            }
            if (!Objects.equals(fontOf(ra), fontOf(rb))) {
                list.add(new String[]{"字体", display(fontOf(ra)), display(fontOf(rb))});
            }
            if (ra.isBold() != rb.isBold()) {
                list.add(new String[]{"加粗", ra.isBold() ? "加粗" : "常规", rb.isBold() ? "加粗" : "常规"});
            }
        }
        if (!Objects.equals(a.getAlignment(), b.getAlignment())) {
            list.add(new String[]{"对齐", display(a.getAlignment()), display(b.getAlignment())});
        }
        if (lineSpacing(a) != lineSpacing(b)) {
            list.add(new String[]{"行距", display(lineSpacing(a)), display(lineSpacing(b))});
        }
        if (indentFirst(a) != indentFirst(b)) {
            list.add(new String[]{"缩进", display(indentFirst(a)), display(indentFirst(b))});
        }
        if (spaceBefore(a) != spaceBefore(b)) {
            list.add(new String[]{"段前距", display(spaceBefore(a)), display(spaceBefore(b))});
        }
        if (spaceAfter(a) != spaceAfter(b)) {
            list.add(new String[]{"段后距", display(spaceAfter(a)), display(spaceAfter(b))});
        }
        return list;
    }

    /** 生成变更描述: "字号 12→14; 行距 1.5→1.25" */
    private String changeDesc(List<String[]> fields) {
        StringBuilder sb = new StringBuilder();
        for (String[] f : fields) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(f[0]).append(' ').append(f[1]).append("→").append(f[2]);
        }
        return sb.toString();
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

    /** 空值/0 显示为 "-" */
    private String display(Object v) {
        if (v == null) return "-";
        if (v instanceof Number && ((Number) v).doubleValue() == 0) return "-";
        return String.valueOf(v).trim();
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