package com.graduate.thesis.service;

import com.graduate.thesis.dto.DiffItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
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
 * 排版差异分析(纯 Java, 不依赖 LibreOffice):
 * 1. 用 POI 对比排版前/后 docx 段落格式差异, 输出结构化变更(字段/变更前后值)
 * 2. pdf 为可选增强(用于页码定位), 无缓存/不可用时差异仍正常返回, 页码为 0
 */
@Service
public class DiffService {

    /** 显示用的文本摘要长度 */
    private static final int TEXT_SUMMARY = 30;
    /** PDF 中搜索匹配用的片段长度(去空白) */
    private static final int MATCH_LEN = 15;

    public List<DiffItem> diff(File original, File formatted, File pdf) {
        List<DiffItem> items = new ArrayList<>();
        // 1. 读取两文档, 按段落文本索引排版后段落
        List<XWPFParagraph> origParas = readParas(original);
        List<XWPFParagraph> fmtParas = readParas(formatted);
        Map<String, List<XWPFParagraph>> fmtIndex = new HashMap<>();
        for (XWPFParagraph p : fmtParas) {
            String t = clean(p.getText());
            if (t.isEmpty()) continue;
            fmtIndex.computeIfAbsent(t, k -> new ArrayList<>()).add(p);
        }
        // 2. 逐段对比格式差异, 收集结构化字段变更
        List<RawDiff> raw = new ArrayList<>();
        for (XWPFParagraph op : origParas) {
            String t = clean(op.getText());
            if (t.isEmpty()) continue;
            List<XWPFParagraph> cand = fmtIndex.get(t);
            if (cand == null || cand.isEmpty()) continue;
            List<String[]> fields = diffFields(op, cand.get(0));
            if (!fields.isEmpty()) {
                raw.add(new RawDiff(t, fields));
            }
        }
        // 3. 可选: 提取结果 PDF 行文本坐标定位页码(失败/无缓存则页码为 0, 前端走文本定位)
        List<Line> lines = null;
        if (pdf != null && pdf.exists()) {
            try {
                lines = extractLines(pdf);
            } catch (Exception ignore) {
                lines = null;
            }
        }
        int idx = 0;
        for (RawDiff rd : raw) {
            idx++;
            int page = 0;
            double y = 0;
            if (lines != null) {
                Line hit = locate(rd.text, lines);
                if (hit != null) {
                    page = hit.page;
                    y = round2(hit.y / pageHeight(pdf, hit.page));
                }
            }
            String type = rd.fields.get(0)[0];
            items.add(new DiffItem(truncate(rd.text), type, changeDesc(rd.fields), idx, page, y, 0.05));
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

    /** 提取 PDF 每行文本及纵向坐标 */
    private List<Line> extractLines(File pdf) {
        List<Line> lines = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(pdf)) {
            PDFTextStripper st = new PDFTextStripper() {
                private float lastY = Float.NaN;
                private final StringBuilder line = new StringBuilder();
                private float lineTop = 0;
                private int pageNo = 0;

                @Override
                protected void startPage(PDPage page) throws java.io.IOException {
                    super.startPage(page);
                    pageNo = getCurrentPageNo();
                    lastY = Float.NaN;
                    line.setLength(0);
                    lineTop = 0;
                }

                @Override
                protected void endPage(PDPage page) throws java.io.IOException {
                    flush();
                    super.endPage(page);
                }

                @Override
                protected void processTextPosition(TextPosition tp) {
                    float y = tp.getYDirAdj();
                    if (Float.isNaN(lastY) || Math.abs(y - lastY) > 3f) {
                        flush();
                        line.append(tp.getUnicode());
                        lineTop = y;
                    } else {
                        line.append(tp.getUnicode());
                    }
                    lastY = y;
                }

                private void flush() {
                    if (line.length() > 0) {
                        String t = line.toString().replaceAll("\\s", "");
                        if (!t.isEmpty()) {
                            lines.add(new Line(pageNo, lineTop, t));
                        }
                        line.setLength(0);
                    }
                }
            };
            st.setSortByPosition(true);
            st.getText(doc);
        } catch (Exception ignore) {
            // PDF 解析失败则返回空
        }
        return lines;
    }

    private Line locate(String text, List<Line> lines) {
        String needle = text.replaceAll("\\s", "");
        if (needle.length() > MATCH_LEN) needle = needle.substring(0, MATCH_LEN);
        if (needle.isEmpty()) return null;
        for (Line l : lines) {
            if (l.text.contains(needle)) return l;
        }
        return null;
    }

    private double pageHeight(File pdf, int pageNo) {
        try (PDDocument doc = PDDocument.load(pdf)) {
            if (pageNo >= 1 && pageNo <= doc.getNumberOfPages()) {
                PDPage page = doc.getPage(pageNo - 1);
                return page.getMediaBox().getHeight();
            }
        } catch (Exception ignore) {
        }
        return 842.0;
    }

    private String clean(String s) {
        return s == null ? "" : s.replaceAll("\\s", "");
    }

    private String truncate(String s) {
        String t = clean(s);
        return t.length() > TEXT_SUMMARY ? t.substring(0, TEXT_SUMMARY) + "…" : t;
    }

    private double round2(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static class RawDiff {
        final String text;
        final List<String[]> fields;

        RawDiff(String text, List<String[]> fields) {
            this.text = text;
            this.fields = fields;
        }
    }

    private static class Line {
        final int page;
        final float y;
        final String text;

        Line(int page, float y, String text) {
            this.page = page;
            this.y = y;
            this.text = text;
        }
    }
}
