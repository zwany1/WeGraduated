package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.ParagraphKind;
import com.graduate.thesis.engine.StructureDetector;
import com.graduate.thesis.engine.StructureHelper;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.engine.model.TocConfig;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPrGeneral;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabStop;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabs;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabTlc;

import java.math.BigInteger;
import java.util.List;

/**
 * 目录格式化:
 * 自动目录(含 TOC 域/PAGEREF/toc 样式)按 tocConfig 改 hyperlink 内条目 run 的字体字号;
 * 纯文本目录按 tocConfig 套各级字体/字号/行距; 无目录时不自动插入, 保持原文档.
 */
public class TocFormatter {

    private static final String NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TocFormatter.class);

    public void apply(XWPFDocument doc, List<DocItem> items, RuleSet ruleSet) {
        TocConfig tc = ruleSet.getTocConfig();
        // 制表位位置: 文本区右边界(页宽 - 左右页边距), 兼容 A4/A3/B5 等不同版心
        long tabPos = Math.max(ruleSet.getPageConfig().textWidthTwips(), 4000L);
        boolean hasAutoToc = false;
        for (XWPFParagraph p : doc.getParagraphs()) {
            if (StructureDetector.isTocStructure(p, doc)) {
                hasAutoToc = true;
                break;
            }
        }
        // 处理纯文本目录条目
        formatManualToc(items, tc, tabPos);

        log.info("[TocFormatter] apply: hasAutoToc={}, toc1.fontSize={}, toc2.fontSize={}, toc3.fontSize={}, lineSpacing={}, leader={}, tabPos={}",
                hasAutoToc, tc.getToc1().getFontSize(), tc.getToc2().getFontSize(), tc.getToc3().getFontSize(), tc.getLineSpacing(), tc.getLeader(), tabPos);

        // 改 toc1-3 样式定义, 让 Word 渲染目录条目时用配置样式
        addTocStyles(doc, tc, tabPos);

        // 自动目录: 按条目级别改 run 字体字号
        if (hasAutoToc) {
            formatAutoTocEntries(doc, tc, tabPos);
        }
    }

    /**
     * 自动目录条目: 按段落 toc 样式级别取对应级配置, 改 run 字体字号.
     * Word 自动目录的条目文本嵌在段落 hyperlink 内, POI getRuns 不含,
     * 需走 CTP 取 hyperlink 的 CTR 才能改到实际显示的 run.
     */
    private void formatAutoTocEntries(XWPFDocument doc, TocConfig tc, long tabPos) {
        if (tc == null) {
            return;
        }
        int changed = 0;
        int totalRuns = 0;
        Integer firstSz = null;
        for (XWPFParagraph p : doc.getParagraphs()) {
            if (!StructureDetector.isTocStructure(p, doc)) {
                continue;
            }
            TocConfig.Level lvl = levelForTocText(p.getText(), tc);
            if (lvl == null) {
                continue;
            }
            CTP ctp = p.getCTP();
            // 顶层 run
            for (CTR ctr : ctp.getRList()) {
                applyTocCtr(ctr, lvl);
                totalRuns++;
                firstSz = readSz(ctr, firstSz);
            }
            // hyperlink 内 run (Word/WPS 自动目录条目文字嵌在此)
            for (CTHyperlink hl : ctp.getHyperlinkList()) {
                for (CTR ctr : hl.getRList()) {
                    applyTocCtr(ctr, lvl);
                    totalRuns++;
                    firstSz = readSz(ctr, firstSz);
                }
            }
            applyTocParagraphProps(p, tc, tabPos);
            changed++;
        }
        log.info("[TocFormatter] formatAutoTocEntries: changed={} paragraphs, runs={}, firstSz={}", changed, totalRuns, firstSz);
    }

    private static Integer readSz(CTR ctr, Integer current) {
        if (current != null) {
            return current;
        }
        try {
            CTRPr rPr = ctr.getRPr();
            if (rPr != null && rPr.sizeOfSzArray() > 0) {
                return ((BigInteger) rPr.getSzArray(0).getVal()).intValue();
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /** 直写 CTR 字体/字号(中西文分离), 覆盖 hyperlink 内嵌的目录条目 run */
    private static void applyTocCtr(CTR ctr, TocConfig.Level lvl) {
        if (lvl == null || ctr == null) {
            return;
        }
        CTRPr rPr = ctr.isSetRPr() ? ctr.getRPr() : ctr.addNewRPr();
        CTFonts fonts = rPr.sizeOfRFontsArray() > 0 ? rPr.getRFontsArray(0) : rPr.addNewRFonts();
        fonts.setEastAsia(lvl.getFont());
        fonts.setAscii(lvl.getFontLatin());
        fonts.setHAnsi(lvl.getFontLatin());
        fonts.setCs(lvl.getFontLatin());
        int sz = Math.max(lvl.getFontSize(), 1) * 2;
        if (rPr.sizeOfSzArray() == 0) {
            rPr.addNewSz();
        }
        rPr.getSzArray(0).setVal(BigInteger.valueOf(sz));
        if (rPr.sizeOfSzCsArray() == 0) {
            rPr.addNewSzCs();
        }
        rPr.getSzCsArray(0).setVal(BigInteger.valueOf(sz));
    }

    /** 自动目录条目段落: 直写行距与制表位前导符到 pPr(排版后即终态, 不依赖样式定义与域刷新) */
    private static void applyTocParagraphProps(XWPFParagraph p, TocConfig tc, long tabPos) {
        CTPPr pPr = p.getCTP().isSetPPr() ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
        int line = Math.round(tc.getLineSpacing() * 240);
        CTSpacing spacing = pPr.isSetSpacing() ? pPr.getSpacing() : pPr.addNewSpacing();
        spacing.setLine(BigInteger.valueOf(line));
        spacing.setLineRule(STLineSpacingRule.AUTO);
        String leader = tc.getLeader();
        if (leader != null && !leader.isEmpty()) {
            if (pPr.isSetTabs()) {
                pPr.unsetTabs();
            }
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabs tabs = pPr.addNewTabs();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabStop tab = tabs.addNewTab();
            tab.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc.RIGHT);
            tab.setLeader(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabTlc.Enum.forString(leader));
            tab.setPos(BigInteger.valueOf(tabPos));
        }
    }

    /** 段落目录级别 -> 对应级配置; 级别不明按一级 */
    private static TocConfig.Level levelForTocParagraph(XWPFParagraph p, TocConfig tc, XWPFDocument doc) {
        int level = tocLevelOf(p, doc);
        if (level == 2) {
            return tc.getToc2();
        }
        if (level == 3) {
            return tc.getToc3();
        }
        return tc.getToc1();
    }

    /** 改 toc 样式定义: 按 name "toc N" 找各级(WPS 的 styleId 是数字, 只能按 name 匹配) */
    private void addTocStyles(XWPFDocument doc, TocConfig tc, long tabPos) {
        XWPFStyles styles = resolveStyles(doc);
        if (styles == null) {
            return;
        }
        CTStyles cts = readCtStyles(styles);
        if (cts == null) {
            return;
        }
        for (CTStyle st : cts.getStyleArray()) {
            if (!st.isSetName() || st.getName().getVal() == null) {
                continue;
            }
            String name = st.getName().getVal();
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("toc\\s*(\\d)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(name);
            if (!m.find()) {
                continue;
            }
            int n = Integer.parseInt(m.group(1));
            if (n == 1) {
                fillTocStyle(st, tc.getToc1(), tc, tabPos);
            } else if (n == 2) {
                fillTocStyle(st, tc.getToc2(), tc, tabPos);
            } else if (n == 3) {
                fillTocStyle(st, tc.getToc3(), tc, tabPos);
            }
        }
    }

    private XWPFStyles resolveStyles(XWPFDocument doc) {
        // 直接用 POI 已加载的样式表; 取不到时返回 null(仅不修改样式定义, 条目 run 直写仍生效)
        try {
            return doc.getStyles();
        } catch (Exception e) {
            return null;
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

    private void fillTocStyle(CTStyle ct, TocConfig.Level lvl, TocConfig tc, long tabPos) {
        if (lvl == null || ct == null) {
            return;
        }
        CTRPr rPr = ct.isSetRPr() ? ct.getRPr() : ct.addNewRPr();
        CTFonts fonts = rPr.sizeOfRFontsArray() > 0 ? rPr.getRFontsArray(0) : rPr.addNewRFonts();
        fonts.setAscii(lvl.getFontLatin());
        fonts.setHAnsi(lvl.getFontLatin());
        fonts.setCs(lvl.getFontLatin());
        fonts.setEastAsia(lvl.getFont());
        int sz = Math.max(lvl.getFontSize(), 1) * 2;
        if (rPr.sizeOfSzArray() == 0) {
            rPr.addNewSz();
        }
        rPr.getSzArray(0).setVal(BigInteger.valueOf(sz));
        if (rPr.sizeOfSzCsArray() == 0) {
            rPr.addNewSzCs();
        }
        rPr.getSzCsArray(0).setVal(BigInteger.valueOf(sz));
        CTPPrGeneral pPr = ct.isSetPPr() ? ct.getPPr() : ct.addNewPPr();
        int line = Math.round(tc.getLineSpacing() * 240);
        CTSpacing spacing = pPr.isSetSpacing() ? pPr.getSpacing() : pPr.addNewSpacing();
        spacing.setLine(BigInteger.valueOf(line));
        spacing.setLineRule(STLineSpacingRule.AUTO);
        String leader = tc.getLeader();
        if (leader != null && !leader.isEmpty()) {
            if (pPr.isSetTabs()) {
                pPr.unsetTabs();
            }
            CTTabs tabs = pPr.addNewTabs();
            CTTabStop tab = tabs.addNewTab();
            tab.setVal(STTabJc.RIGHT);
            tab.setLeader(STTabTlc.Enum.forString(leader));
            tab.setPos(BigInteger.valueOf(tabPos));
        }
    }

    /** 目录条目级别: 样式 name "toc N" 取 N 优先, styleId 末位数字兜底, 大纲级别再兜底 */
    private static int tocLevelOf(XWPFParagraph p, XWPFDocument doc) {
        try {
            CTPPr pPr = p.getCTP().isSetPPr() ? p.getCTP().getPPr() : null;
            if (pPr != null && pPr.isSetPStyle() && pPr.getPStyle().getVal() != null) {
                String sid = pPr.getPStyle().getVal();
                int n = tocLevelByName(doc, sid);
                if (n >= 1 && n <= 3) {
                    return n;
                }
                n = trailingDigits(sid);
                if (n >= 1 && n <= 3) {
                    return n;
                }
            }
            if (pPr != null && pPr.isSetOutlineLvl()) {
                int lvl = pPr.getOutlineLvl().getVal().intValue();
                if (lvl >= 0 && lvl <= 2) {
                    return lvl + 1;
                }
            }
        } catch (Exception ignore) {
        }
        return 0;
    }

    /** 查样式 name "toc N" 取级别 N; WPS 数字 styleId 仅 name 标识 toc 级别 */
    private static int tocLevelByName(XWPFDocument doc, String styleId) {
        if (doc == null || styleId == null) {
            return 0;
        }
        try {
            org.apache.poi.xwpf.usermodel.XWPFStyles styles = doc.getStyles();
            if (styles == null) {
                return 0;
            }
            org.apache.poi.xwpf.usermodel.XWPFStyle st = styles.getStyle(styleId);
            if (st == null) {
                return 0;
            }
            String name = st.getName();
            if (name == null) {
                return 0;
            }
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("toc\\s*(\\d)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(name);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception e) {
        }
        return 0;
    }

    private static int trailingDigits(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int end = s.length();
        while (end > 0 && Character.isDigit(s.charAt(end - 1))) {
            end--;
        }
        if (end == s.length()) {
            return 0;
        }
        try {
            return Integer.parseInt(s.substring(end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 目录标题: 三号黑体居中, 另起一页 */
    private static void formatTocTitle(XWPFParagraph p) {
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }
        XWPFRun run = p.createRun();
        run.setText("目    录");
        TextFormatter.setFont(run, "黑体", TextFormatter.DEFAULT_LATIN);
        run.setFontSize(16);
        run.setBold(true);
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        ParagraphFormatter.setPageBreakBefore(p);
    }

    /** 纯文本目录: 标题三号黑体居中, 条目按各级配置字体/字号/行距 */
    private static void formatManualToc(List<DocItem> items, TocConfig tc, long tabPos) {
        boolean inToc = false;
        for (DocItem item : items) {
            String text = item.getText() == null ? "" : item.getText().trim();
            if (item.getKind() == ParagraphKind.SECTION_TITLE && StructureHelper.isTocTitle(text)) {
                inToc = true;
                formatTocTitle(item.getParagraph());
                continue;
            }
            if (inToc) {
                if (item.getKind() == ParagraphKind.HEADING1) {
                    inToc = false;
                    continue;
                }
                if (!text.isEmpty() && StructureHelper.isTocEntry(text)) {
                    formatTocEntry(item.getParagraph(), levelForTocText(text, tc), tc, tabPos);
                }
            }
        }
    }

    /** 纯文本目录条目级别: 按编号点数判断(1.1.1=三级, 1.1=二级, 其余=一级) */
    private static TocConfig.Level levelForTocText(String text, TocConfig tc) {
        String t = text.replaceFirst("[\\s\\d０-９]+$", "").trim();
        if (t.matches(".*\\d+\\.\\d+\\.\\d+.*")) {
            return tc.getToc3();
        }
        if (t.matches(".*\\d+\\.\\d+.*")) {
            return tc.getToc2();
        }
        return tc.getToc1();
    }

    private static void formatTocEntry(XWPFParagraph p, TocConfig.Level lvl, TocConfig tc, long tabPos) {
        if (lvl == null) {
            return;
        }
        for (XWPFRun run : p.getRuns()) {
            TextFormatter.setFont(run, lvl.getFont(), lvl.getFontLatin());
            run.setFontSize(lvl.getFontSize());
            run.setBold(false);
        }
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        applyTocParagraphProps(p, tc, tabPos);
    }
}
