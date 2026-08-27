package com.graduate.thesis.engine.formatter;

import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.ParagraphKind;
import com.graduate.thesis.engine.StructureDetector;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.engine.model.TocConfig;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.math.BigInteger;
import java.util.List;

/**
 * 目录格式化:
 * 自动目录(含 TOC 域/PAGEREF/toc 样式)按 tocConfig 改 hyperlink 内条目 run 的字体字号;
 * 纯文本目录按 tocConfig 套各级字体/字号/行距; 无目录时不自动插入, 保持原文档.
 */
public class TocFormatter {

    public void apply(XWPFDocument doc, List<DocItem> items, RuleSet ruleSet) {
        TocConfig tc = ruleSet.getTocConfig();
        boolean hasAutoToc = false;
        for (XWPFParagraph p : doc.getParagraphs()) {
            if (StructureDetector.isTocStructure(p, doc)) {
                hasAutoToc = true;
                break;
            }
        }
        // 处理纯文本目录条目
        formatManualToc(items, tc);

        // 自动目录: 按条目级别改 run 字体字号
        if (hasAutoToc) {
            formatAutoTocEntries(doc, tc);
        }
    }

    /**
     * 自动目录条目: 按段落 toc 样式级别取对应级配置, 改 run 字体字号.
     * Word 自动目录的条目文本嵌在段落 hyperlink 内, POI getRuns 不含,
     * 需走 CTP 取 hyperlink 的 CTR 才能改到实际显示的 run.
     */
    private void formatAutoTocEntries(XWPFDocument doc, TocConfig tc) {
        if (tc == null) {
            return;
        }
        for (XWPFParagraph p : doc.getParagraphs()) {
            if (!StructureDetector.isTocStructure(p, doc)) {
                continue;
            }
            TocConfig.Level lvl = levelForTocParagraph(p, tc, doc);
            if (lvl == null) {
                continue;
            }
            CTP ctp = p.getCTP();
            for (CTR ctr : ctp.getRList()) {
                applyTocCtr(ctr, lvl);
            }
            for (CTHyperlink hl : ctp.getHyperlinkList()) {
                for (CTR ctr : hl.getRList()) {
                    applyTocCtr(ctr, lvl);
                }
            }
            applyTocParagraphProps(p, tc);
        }
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
    private static void applyTocParagraphProps(XWPFParagraph p, TocConfig tc) {
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
            tab.setPos(BigInteger.valueOf(8296));
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
    private static void formatManualToc(List<DocItem> items, TocConfig tc) {
        boolean inToc = false;
        for (DocItem item : items) {
            String text = item.getText() == null ? "" : item.getText().trim();
            if (item.getKind() == ParagraphKind.SECTION_TITLE && StructureDetector.isTocTitle(text)) {
                inToc = true;
                formatTocTitle(item.getParagraph());
                continue;
            }
            if (inToc) {
                if (item.getKind() == ParagraphKind.HEADING1) {
                    inToc = false;
                    continue;
                }
                if (!text.isEmpty() && StructureDetector.isTocEntry(text)) {
                    formatTocEntry(item.getParagraph(), levelForTocText(text, tc), tc);
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

    private static void formatTocEntry(XWPFParagraph p, TocConfig.Level lvl, TocConfig tc) {
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
        applyTocParagraphProps(p, tc);
    }
}
