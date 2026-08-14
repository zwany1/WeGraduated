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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 排版差异分析:
 * 1. 用 POI 对比排版前/后 docx 段落格式差异
 * 2. 用 PDFBox 在结果 PDF 中定位差异段落(页码 + 归一化 y 坐标)
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
        // 2. 逐段对比格式差异
        List<RawDiff> raw = new ArrayList<>();
        for (XWPFParagraph op : origParas) {
            String t = clean(op.getText());
            if (t.isEmpty()) continue;
            List<XWPFParagraph> cand = fmtIndex.get(t);
            if (cand == null || cand.isEmpty()) continue;
            XWPFParagraph fp = cand.get(0);
            String diffs = diffFormat(op, fp);
            if (!diffs.isEmpty()) {
                raw.add(new RawDiff(t, diffs));
            }
        }
        // 3. 提取结果 PDF 行文本坐标, 定位差异段落
        List<Line> lines = extractLines(pdf);
        for (RawDiff rd : raw) {
            Line hit = locate(rd.text, lines);
            if (hit != null) {
                items.add(new DiffItem(
                        truncate(rd.text),
                        rd.diffs,
                        hit.page,
                        round2(hit.y / pageHeight(pdf, hit.page)),
                        0.05));
            }
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

    /** 对比两个段落的格式差异, 返回中文差异列表 */
    private String diffFormat(XWPFParagraph a, XWPFParagraph b) {
        Set<String> diffs = new LinkedHashSet<>();
        // run 级: 取首个非空 run 的格式
        XWPFRun ra = firstRun(a);
        XWPFRun rb = firstRun(b);
        if (ra != null && rb != null) {
            if (!Objects.equals(ra.getFontSize(), rb.getFontSize())) diffs.add("字号");
            if (!Objects.equals(fontOf(ra), fontOf(rb))) diffs.add("字体");
            if (ra.isBold() != rb.isBold()) diffs.add("加粗");
        }
        // 段落级
        if (!Objects.equals(a.getAlignment(), b.getAlignment())) diffs.add("对齐");
        if (!Objects.equals(lineSpacing(a), lineSpacing(b))) diffs.add("行距");
        if (!Objects.equals(indentFirst(a), indentFirst(b))) diffs.add("缩进");
        if (!Objects.equals(spaceBefore(a), spaceBefore(b))) diffs.add("段前距");
        if (!Objects.equals(spaceAfter(a), spaceAfter(b))) diffs.add("段后距");
        return String.join(",", diffs);
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
        final String diffs;

        RawDiff(String text, String diffs) {
            this.text = text;
            this.diffs = diffs;
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
