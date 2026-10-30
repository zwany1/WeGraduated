package com.graduate.thesis.service;

import com.graduate.thesis.dto.DiffItem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPrGeneral;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHpsMeasure;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 排版差异分析(纯 Java).
 *
 * <p><b>口径</b>: 比较"段落最终生效的格式", 而不是"谁显式写了哪个属性".
 * 原稿(PDF 转 Word)大量依赖样式继承, 产物则习惯把值写成直接格式 —— 直接比属性会产出海量伪差异
 * (如"字号 -1pt → 12pt(小四)"、"字体 未设置 → 宋体"). 所以按
 * <i>直接格式 → 段落样式链(basedOn) → Normal 样式</i> 解析出实际值再比.
 *
 * <p>另外补了三类"内容级差异": 非空段落数 / 图片数 / 表格数 —— 引擎吞段、丢图时逐段格式比对
 * 完全发现不了(文本对不上会被直接跳过), 而这恰恰是最需要报警的损坏.
 */
@Service
public class DiffService {

    /** 显示用的文本摘要长度 */
    private static final int TEXT_SUMMARY = 30;

    /** 样式 basedOn 链的最大追溯深度, 防止脏数据成环 */
    private static final int STYLE_DEPTH = 8;

    /** 排障开关: 打开后把每段的解析结果打到 stderr(只给测试用, 生产保持 false) */
    public static boolean DEBUG = false;

    /** 对比排版前/后 docx, 输出结构化差异(内容级 + 逐段格式) */
    public List<DiffItem> diff(File original, File formatted) {
        List<DiffItem> items = new ArrayList<>();
        try (XWPFDocument od = new XWPFDocument(new FileInputStream(original));
             XWPFDocument fd = new XWPFDocument(new FileInputStream(formatted))) {
            contentDiffs(od, fd, items);
            paraDiffs(od, fd, items);
        } catch (Exception ignore) {
            // 读不动就返回已算出的部分, 不能因为比对失败影响整个接口
        }
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setIndex(i + 1);
        }
        return items;
    }

    // ==================== 内容级差异 ====================

    /** 段落数 / 图片数 / 表格数 —— 吞段、丢图这类损坏只有在这一层才能发现 */
    private void contentDiffs(XWPFDocument od, XWPFDocument fd, List<DiffItem> items) {
        int op = countTextParas(od);
        int fp = countTextParas(fd);
        if (op != fp) {
            String tail = fp < op
                    ? "(减少 " + (op - fp) + " 段: 可能被合并或删除, 建议人工核对)"
                    : "(增加 " + (fp - op) + " 段)";
            items.add(new DiffItem("（整篇文档）", "内容", "非空段落 " + op + " → " + fp + " " + tail, 0));
        }
        int oi = countImages(od);
        int fi = countImages(fd);
        if (oi != fi) {
            items.add(new DiffItem("（整篇文档）", "内容",
                    "图片 " + oi + " → " + fi + (fi < oi ? " (丢失 " + (oi - fi) + " 张)" : ""), 0));
        }
        int ot = od.getTables().size();
        int ft = fd.getTables().size();
        if (ot != ft) {
            items.add(new DiffItem("（整篇文档）", "内容", "表格 " + ot + " → " + ft, 0));
        }
    }

    /** 图片按"文档里的引用次数"统计, 不能用 getAllPictures().size():
     *  media 目录可能含未被引用的孤儿文件(实测有 1 个), 会把"没丢"误报成"丢了 1 张" */
    private int countImages(XWPFDocument doc) {
        int n = 0;
        for (XWPFParagraph p : allParas(doc)) {
            for (XWPFRun r : p.getRuns()) {
                try {
                    n += r.getEmbeddedPictures().size();
                } catch (Exception ignore) {
                    // 某些畸形 run 读不出来就跳过, 不影响总数级判断
                }
            }
        }
        return n;
    }

    private int countTextParas(XWPFDocument doc) {
        int n = 0;
        for (XWPFParagraph p : allParas(doc)) {
            if (!clean(p.getText()).isEmpty()) n++;
        }
        return n;
    }

    /** 正文段落 + 表格单元格内段落(三线表等表格格式的改动只有算进来才看得见) */
    private List<XWPFParagraph> allParas(XWPFDocument doc) {
        List<XWPFParagraph> list = new ArrayList<>(doc.getParagraphs());
        for (XWPFTable t : doc.getTables()) {
            for (XWPFTableRow r : t.getRows()) {
                for (XWPFTableCell c : r.getTableCells()) {
                    list.addAll(c.getParagraphs());
                }
            }
        }
        return list;
    }

    // ==================== 逐段格式 ====================

    private void paraDiffs(XWPFDocument od, XWPFDocument fd, List<DiffItem> items) {
        Map<String, List<XWPFParagraph>> fmtIndex = new HashMap<>();
        for (XWPFParagraph p : allParas(fd)) {
            String t = clean(p.getText());
            if (t.isEmpty()) continue;
            fmtIndex.computeIfAbsent(t, k -> new ArrayList<>()).add(p);
        }
        // 同文本段落按出现顺序逐个消费, 避免跨段落错配(如两行相同的签名行)
        Map<String, Integer> cursor = new HashMap<>();
        for (XWPFParagraph op : allParas(od)) {
            String t = clean(op.getText());
            if (t.isEmpty()) continue;
            List<XWPFParagraph> cand = fmtIndex.get(t);
            if (cand == null || cand.isEmpty()) continue;
            int c = cursor.getOrDefault(t, 0);
            if (c >= cand.size()) continue;
            cursor.put(t, c + 1);
            List<String[]> fields = diffFields(od, op, fd, cand.get(c));
            if (!fields.isEmpty()) {
                items.add(new DiffItem(truncate(t), fields.get(0)[0], changeDesc(fields), 0));
            }
        }
    }

    /** 对比两段最终生效的格式, 返回 [字段名, 变更前, 变更后] */
    private List<String[]> diffFields(XWPFDocument od, XWPFParagraph a, XWPFDocument fd, XWPFParagraph b) {
        List<String[]> list = new ArrayList<>();
        Fmt fa = resolve(od, a);
        Fmt fb = resolve(fd, b);
        if (DEBUG) {
            System.err.println("[DIFF] " + clean(a.getText()).substring(0, Math.min(14, clean(a.getText()).length()))
                    + " | A(size=" + fa.size + ",font=" + fa.font + ",align=" + fa.align + ",line=" + fa.line + ")"
                    + " | B(size=" + fb.size + ",font=" + fb.font + ",align=" + fb.align + ",line=" + fb.line + ")");
        }

        if (fa.size != null && fb.size != null && !sameNum(fa.size, fb.size)) {
            list.add(new String[]{"字号", displaySize(fa.size), displaySize(fb.size)});
        }
        if (fa.font != null && fb.font != null && !fa.font.equals(fb.font)) {
            list.add(new String[]{"字体", fa.font, fb.font});
        }
        if (fa.bold != null && fb.bold != null && !fa.bold.equals(fb.bold)) {
            list.add(new String[]{"加粗", fa.bold ? "是" : "否", fb.bold ? "是" : "否"});
        }
        if (fa.line != null && fb.line != null && !fa.line.equals(fb.line)) {
            list.add(new String[]{"行距", fa.line, fb.line});
        }
        if (!fa.align.equals(fb.align)) {
            list.add(new String[]{"对齐", fa.align, fb.align});
        }
        if (fa.firstLine != fb.firstLine) {
            list.add(new String[]{"首行缩进", displayIndent(fa.firstLine), displayIndent(fb.firstLine)});
        }
        if (fa.before != fb.before) {
            list.add(new String[]{"段前距", displayPt(fa.before), displayPt(fb.before)});
        }
        if (fa.after != fb.after) {
            list.add(new String[]{"段后距", displayPt(fa.after), displayPt(fb.after)});
        }
        return list;
    }

    /** 段落最终生效的格式 */
    private static final class Fmt {
        Double size;
        String font;
        Boolean bold;
        String line;
        String align;
        int firstLine;
        int before;
        int after;
    }

    private Fmt resolve(XWPFDocument doc, XWPFParagraph p) {
        Fmt f = new Fmt();
        List<CTRPr> rps = runChain(doc, p, firstRun(p));
        List<Pv> pvs = paraValues(doc, p);

        f.size = sizeOf(rps);
        f.font = fontOf(rps);
        f.bold = boldOf(rps);
        f.line = firstLineDesc(pvs);
        f.align = firstAlign(pvs);
        f.firstLine = firstInt(pvs, 0);
        f.before = Math.max(0, firstInt(pvs, 1));
        f.after = Math.max(0, firstInt(pvs, 2));
        return f;
    }

    // ---------- 段落级度量: 各级 pPr 先取成值, 再按字段取第一个"设置了"的值 ----------
    // 直接格式的 pPr 是 CTPPr, 样式里的 pPr 是 CTPPrGeneral, 二者在 xmlbeans 里是平行类型,
    // 不能放进同一个 List, 所以先把度量拆成值对象。

    /** 一级 pPr 的度量快照; null 表示该级没设这个属性 */
    private static final class Pv {
        String line;
        String align;
        Integer firstLine;
        Integer before;
        Integer after;

        Pv(CTSpacing sp, CTInd ind, CTJc jc) {
            if (sp != null) {
                int lineTwips = intOfObj(sp.getLine());
                if (lineTwips > 0) {
                    double v = lineTwips;
                    STLineSpacingRule.Enum rule = sp.getLineRule();
                    if (rule == STLineSpacingRule.EXACT) {
                        this.line = num2(v / 20d) + "pt(固定)";
                    } else if (rule == STLineSpacingRule.AT_LEAST) {
                        this.line = num2(v / 20d) + "pt(最小)";
                    } else {
                        this.line = num2(v / 240d) + "倍";
                    }
                } else if (lineTwips == 0) {
                    // w:line="0" 在 Word 里就是单倍行距, 不是"0 倍"
                    this.line = "单倍";
                }
                this.before = intOfObj(sp.getBefore());
                this.after = intOfObj(sp.getAfter());
            }
            if (ind != null) {
                int hanging = intOfObj(ind.getHanging());
                if (hanging >= 0) {
                    this.firstLine = -hanging; // 悬挂缩进记成负数
                } else {
                    int fl = intOfObj(ind.getFirstLine());
                    if (fl >= 0) this.firstLine = fl;
                }
            }
            if (jc != null && jc.getVal() != null) {
                this.align = alignName(String.valueOf(jc.getVal()));
            }
        }

        /** 直接格式(w:pPr) */
        static Pv of(CTPPr pp) {
            return new Pv(pp.getSpacing(), pp.getInd(), pp.getJc());
        }

        /** 样式里的 pPr */
        static Pv of(CTPPrGeneral pp) {
            return new Pv(pp.getSpacing(), pp.getInd(), pp.getJc());
        }
    }

    private List<Pv> paraValues(XWPFDocument doc, XWPFParagraph p) {
        List<Pv> out = new ArrayList<>();
        try {
            if (p.getCTP() != null && p.getCTP().isSetPPr()) {
                out.add(Pv.of(p.getCTP().getPPr()));
            }
        } catch (Exception ignore) {
            // 直接格式读不到就往下走样式链
        }
        for (XWPFStyle s : styleChain(doc, p)) {
            try {
                CTPPrGeneral pp = s.getCTStyle() == null ? null : s.getCTStyle().getPPr();
                if (pp != null) out.add(Pv.of(pp));
            } catch (Exception ignore) {
                // 单个样式读失败不影响其它层级
            }
        }
        return out;
    }

    private String firstLineDesc(List<Pv> pvs) {
        for (Pv v : pvs) if (v.line != null) return v.line;
        return null;
    }

    private String firstAlign(List<Pv> pvs) {
        for (Pv v : pvs) if (v.align != null) return v.align;
        return "左对齐";
    }

    /** idx: 0=首行缩进, 1=段前距, 2=段后距 */
    private int firstInt(List<Pv> pvs, int idx) {
        for (Pv v : pvs) {
            Integer x = idx == 0 ? v.firstLine : idx == 1 ? v.before : v.after;
            if (x != null) return x;
        }
        return 0;
    }

    private static String alignName(String raw) {
        String s = raw == null ? "" : raw.toLowerCase();
        switch (s) {
            case "left":
            case "start": return "左对齐";
            case "center": return "居中";
            case "right":
            case "end": return "右对齐";
            case "both": return "两端对齐";
            case "distribute": return "分散对齐";
            default: return raw;
        }
    }

    // ---------- run 级格式 ----------

    private Double sizeOf(List<CTRPr> rps) {
        for (CTRPr rp : rps) {
            if (rp == null) continue;
            try {
                List<CTHpsMeasure> szs = rp.getSzList();
                if (szs != null && !szs.isEmpty()) {
                    CTHpsMeasure sz = szs.get(0);
                    if (sz != null && sz.getVal() != null) return doubleOfObj(sz.getVal(), 0d) / 2d;
                }
            } catch (Exception ignore) {
                // 该级读不到就看下一级
            }
        }
        return null;
    }

    /** 字体: 中文优先取 eastAsia, 没有则回落 ascii —— 中文论文的主体是 eastAsia */
    private String fontOf(List<CTRPr> rps) {
        for (CTRPr rp : rps) {
            String v = fontAttr(rp, true);
            if (v != null) return v;
        }
        for (CTRPr rp : rps) {
            String v = fontAttr(rp, false);
            if (v != null) return v;
        }
        return null;
    }

    private String fontAttr(CTRPr rp, boolean eastAsia) {
        if (rp == null) return null;
        try {
            List<CTFonts> fonts = rp.getRFontsList();
            if (fonts == null || fonts.isEmpty()) return null;
            CTFonts rf = fonts.get(0);
            if (rf == null) return null;
            String v = eastAsia ? rf.getEastAsia() : rf.getAscii();
            return (v == null || v.trim().isEmpty()) ? null : v.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean boldOf(List<CTRPr> rps) {
        for (CTRPr rp : rps) {
            if (rp == null) continue;
            try {
                List<CTOnOff> bs = rp.getBList();
                if (bs != null && !bs.isEmpty()) {
                    CTOnOff b = bs.get(0);
                    Object v = b == null ? null : b.getVal();
                    if (v == null) return Boolean.TRUE; // <w:b/> 不带 val 即 true
                    String s = String.valueOf(v);
                    return !("0".equals(s) || "false".equalsIgnoreCase(s) || "off".equalsIgnoreCase(s));
                }
            } catch (Exception ignore) {
                // 该级读不到就看下一级
            }
        }
        return null;
    }

    // ==================== 样式链 ====================

    private List<CTRPr> runChain(XWPFDocument doc, XWPFParagraph p, XWPFRun r) {
        List<CTRPr> list = new ArrayList<>();
        try {
            if (r != null && r.getCTR() != null && r.getCTR().isSetRPr()) {
                list.add(r.getCTR().getRPr());
            }
        } catch (Exception ignore) {
            // 直接格式读不到就往下走样式链
        }
        for (XWPFStyle s : styleChain(doc, p)) {
            try {
                if (s.getCTStyle() != null && s.getCTStyle().isSetRPr()) {
                    list.add(s.getCTStyle().getRPr());
                }
            } catch (Exception ignore) {
                // 单个样式读失败不影响其它层级
            }
        }
        return list;
    }

    /** 段落样式 → basedOn 链 → Normal 样式 */
    private List<XWPFStyle> styleChain(XWPFDocument doc, XWPFParagraph p) {
        Map<String, XWPFStyle> seen = new LinkedHashMap<>();
        XWPFStyles styles;
        try {
            styles = doc.getStyles();
        } catch (Exception e) {
            return new ArrayList<>();
        }
        if (styles == null) return new ArrayList<>();
        String sid = null;
        try {
            sid = p.getStyleID();
        } catch (Exception ignore) {
            // 没有样式就走默认
        }
        int guard = 0;
        while (sid != null && guard++ < STYLE_DEPTH && !seen.containsKey(sid)) {
            XWPFStyle s;
            try {
                s = styles.getStyle(sid);
            } catch (Exception e) {
                break;
            }
            if (s == null) break;
            seen.put(sid, s);
            try {
                sid = s.getBasisStyleID();
            } catch (Exception e) {
                break;
            }
        }
        // 文档默认样式(XWPFDefaultParagraphStyle)不暴露 CTPPr/CTRPr, 用 Word 约定的 Normal 兜底
        for (String id : new String[]{"Normal", "a"}) {
            try {
                XWPFStyle s = styles.getStyle(id);
                if (s != null) {
                    seen.put(id, s);
                    break;
                }
            } catch (Exception ignore) {
                // 没有就算了
            }
        }
        return new ArrayList<>(seen.values());
    }

    private XWPFRun firstRun(XWPFParagraph p) {
        for (XWPFRun r : p.getRuns()) {
            String t = r.getText(0);
            if (t != null && !t.trim().isEmpty()) return r;
        }
        return null;
    }

    // ==================== 显示 ====================

    private String changeDesc(List<String[]> fields) {
        StringBuilder sb = new StringBuilder();
        for (String[] f : fields) {
            if (sb.length() > 0) sb.append('\n');
            sb.append('【').append(f[0]).append('】').append(f[1]).append(" → ").append(f[2]);
        }
        return sb.toString();
    }

    private String displaySize(double pt) {
        String name = cnSize(pt);
        return num2(pt) + "pt" + (name == null ? "" : "(" + name + ")");
    }

    /** pt → 中文字号名(10.5pt 才是五号, 不能用整数猜) */
    private static String cnSize(double pt) {
        double[] pts = {7.5, 9, 10.5, 12, 14, 15, 16, 18, 22, 24, 26, 36, 42};
        String[] names = {"六号", "小五", "五号", "小四", "四号", "小三", "三号", "小二", "二号", "小一", "一号", "小初", "初号"};
        for (int i = 0; i < pts.length; i++) {
            if (Math.abs(pts[i] - pt) < 0.05) return names[i];
        }
        return null;
    }

    private String displayIndent(int twips) {
        if (twips == 0) return "无";
        if (twips < 0) return "悬挂 " + num2(-twips / 240d) + " 字符";
        return num2(twips / 240d) + " 字符";
    }

    private String displayPt(int twips) {
        if (twips <= 0) return "无";
        return num2(twips / 20d) + "pt";
    }

    /** xmlbeans 有些度量属性是 Object(可能是 BigInteger/String), 统一解析; 不可解析返回 -1 */
    private static int intOfObj(Object v) {
        if (v == null) return -1;
        try {
            return new BigDecimal(String.valueOf(v).trim()).intValue();
        } catch (Exception e) {
            return -1;
        }
    }

    private static double doubleOfObj(Object v, double dft) {
        if (v == null) return dft;
        try {
            return new BigDecimal(String.valueOf(v).trim()).doubleValue();
        } catch (Exception e) {
            return dft;
        }
    }

    private static boolean sameNum(Double a, Double b) {
        return a != null && b != null && Math.abs(a - b) < 0.05;
    }

    /** 去掉多余的小数尾巴: 12.0 → 12, 1.6667 → 1.67 */
    private static String num2(double v) {
        return new BigDecimal(String.valueOf(Math.round(v * 100) / 100d)).stripTrailingZeros().toPlainString();
    }

    private String clean(String s) {
        return s == null ? "" : s.replaceAll("\\s", "");
    }

    private String truncate(String s) {
        String t = clean(s);
        return t.length() > TEXT_SUMMARY ? t.substring(0, TEXT_SUMMARY) + "…" : t;
    }
}
