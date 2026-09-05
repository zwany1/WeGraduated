package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.engine.model.SpecExtractVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 校规文档启发式抽取: 从学校《格式规范》docx 的文本语句中,
 * 用正则匹配"字体/字号/行距/边距/编号形式"等表述, 生成模板配置初稿.
 * 命中的字段都带原文摘录(evidence), 抽不到的字段不产出, 由前端保持当前值.
 */
@Slf4j
@Service
public class SpecExtractService {

    /** 中文字号 → 磅值(与前端字号下拉一致) */
    private static final Map<String, Integer> CN_SIZE = Map.ofEntries(
            Map.entry("初号", 42), Map.entry("小初", 36),
            Map.entry("一号", 26), Map.entry("小一", 24),
            Map.entry("二号", 22), Map.entry("小二", 18),
            Map.entry("三号", 16), Map.entry("小三", 15),
            Map.entry("四号", 14), Map.entry("小四", 12),
            Map.entry("五号", 10), Map.entry("小五", 9));

    private static final Pattern FONT_RE =
            Pattern.compile("(宋体|仿宋_GB2312|仿宋|黑体|楷体_GB2312|楷体|微软雅黑|Times New Roman|Times新罗马|新罗马)");
    private static final Pattern SIZE_CN_RE =
            Pattern.compile("(初号|小初|一号|小一|二号|小二|三号|小三|四号|小四|五号|小五)");
    private static final Pattern SIZE_PT_RE = Pattern.compile("(\\d{1,2}(?:\\.\\d)?)\\s*(?:磅|pt|PT|Pt)");
    private static final Pattern LINE_MULTIPLE_RE = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*倍(?:行距)?|行距[^。;；]{0,6}?(\\d+(?:\\.\\d+)?)\\s*倍");
    private static final Pattern LINE_EXACT_RE = Pattern.compile("固定值?[^0-9。;；]{0,4}(\\d{2})\\s*(?:磅|pt)|行距[^。;；]{0,6}?(\\d{2})\\s*磅");
    private static final Pattern INDENT_RE = Pattern.compile("缩进[^0-9。;；]{0,4}(\\d+)\\s*(?:个)?(?:汉字|字符)");
    private static final Pattern MARGIN_RE = Pattern.compile("(上|下|左|右)(?:边距)?[^0-9。;；上下左右]{0,3}(\\d+(?:\\.\\d+)?)\\s*(?:cm|厘米|公分)");
    /** 成对形式: "上下2.5cm" / "左右3cm" */
    private static final Pattern PAIR_MARGIN_RE = Pattern.compile("(上下|左右)[^0-9。;；]{0,3}(\\d+(?:\\.\\d+)?)\\s*(?:cm|厘米|公分)");
    private static final Pattern PAPER_RE = Pattern.compile("(A4|B5|16开|32开|[1-9]开)");

    /** 句子切分 */
    private static final Pattern SENT_SPLIT = Pattern.compile("[。;；\\n]");

    public SpecExtractVO extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传校规文档");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".docx")) {
            throw new BusinessException("仅支持 .docx 格式的校规文档");
        }
        SpecExtractVO vo = new SpecExtractVO();
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            List<String> sentences = new java.util.ArrayList<>();
            int count = 0;
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText() == null ? "" : p.getText().replaceAll("\\s+", "");
                if (text.isEmpty()) {
                    continue;
                }
                count++;
                if (sentences.size() < 2000) {
                    for (String s : SENT_SPLIT.split(text)) {
                        if (!s.isBlank()) {
                            sentences.add(s);
                        }
                    }
                }
            }
            vo.setParagraphCount(count);
            extractAll(vo, sentences);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("校规抽取失败: {}", e.getMessage());
            throw new BusinessException("文档无法解析，请确认是有效的 .docx 格式");
        }
    }

    private void extractAll(SpecExtractVO vo, List<String> sentences) {
        for (String s0 : sentences) {
            final String s = s0;
            extractPaper(vo, s);
            extractMargins(vo, s);
            extractBody(vo, s);
            extractHeading(vo, s);
            extractCaption(vo, s);
            extractReference(vo, s);
        }
    }

    // ---------- 页面 ----------

    private void extractPaper(SpecExtractVO vo, String s) {
        if (!s.contains("纸") && !s.contains("A4") && !s.contains("B5")) {
            return;
        }
        Matcher m = PAPER_RE.matcher(s);
        if (m.find()) {
            String paper = m.group(1);
            // 16开纸张与 A4 尺寸接近, 统一映射为 A4; 其余原样
            if (paper.equals("16")) {
                paper = "A4";
            }
            vo.getPageConfig().put("paper", paper);
            vo.getEvidence().add(new SpecExtractVO.Evidence("page.paper", paper, s, "high"));
        }
    }

    private void extractMargins(SpecExtractVO vo, String s) {
        if (!s.contains("边距") && !s.contains("cm") && !s.contains("厘米")) {
            return;
        }
        // 边距写入 pageConfig.margin 子对象(与前端 page.margin 键结构一致)
        Map<String, Object> margin = (Map<String, Object>) vo.getPageConfig()
                .computeIfAbsent("margin", k -> new LinkedHashMap<String, Object>());
        // 成对形式优先: "上下2.5cm" 同时设置上/下, "左右3cm" 同时设置左/右
        Matcher pair = PAIR_MARGIN_RE.matcher(s);
        while (pair.find()) {
            double v = Double.parseDouble(pair.group(2));
            if (v <= 0 || v > 10) {
                continue;
            }
            if (pair.group(1).contains("上")) {
                margin.put("top", v);
                margin.put("bottom", v);
                vo.getEvidence().add(new SpecExtractVO.Evidence("page.margin上下", String.valueOf(v), s, "high"));
            } else {
                margin.put("left", v);
                margin.put("right", v);
                vo.getEvidence().add(new SpecExtractVO.Evidence("page.margin左右", String.valueOf(v), s, "high"));
            }
        }
        // 单边形式: "上边距2.5cm"(仅补未设置的边)
        Matcher m = MARGIN_RE.matcher(s);
        while (m.find()) {
            String side = m.group(1);
            double v = Double.parseDouble(m.group(2));
            if (v <= 0 || v > 10) {
                continue;
            }
            String key;
            if (side.equals("上")) {
                key = "top";
            } else if (side.equals("下")) {
                key = "bottom";
            } else if (side.equals("左")) {
                key = "left";
            } else {
                key = "right";
            }
            if (margin.containsKey(key)) {
                continue;
            }
            margin.put(key, v);
            vo.getEvidence().add(new SpecExtractVO.Evidence("page.margin" + side, String.valueOf(v), s, "high"));
        }
    }

    // ---------- 正文 ----------

    private void extractBody(SpecExtractVO vo, String s) {
        if (!s.contains("正文") && !s.contains("行距") && !s.contains("缩进")) {
            return;
        }
        Map<String, Object> body = vo.getRules().computeIfAbsent("body", k -> new LinkedHashMap<>());
        if (s.contains("正文")) {
            String font = findFont(s);
            if (font != null && !body.containsKey("font")) {
                body.put("font", font);
                vo.getEvidence().add(new SpecExtractVO.Evidence("body.font", font, s, "high"));
            }
            Integer size = findSize(s);
            if (size != null && !body.containsKey("fontSize")) {
                body.put("fontSize", size);
                vo.getEvidence().add(new SpecExtractVO.Evidence("body.fontSize", String.valueOf(size), s, "high"));
            }
        }
        if (s.contains("行距") && !body.containsKey("lineSpacingType")) {
            Matcher ex = LINE_EXACT_RE.matcher(s);
            Matcher mu = LINE_MULTIPLE_RE.matcher(s);
            if (ex.find()) {
                body.put("lineSpacingType", "exact");
                body.put("lineSpacingExact", Integer.parseInt(ex.group(1) != null ? ex.group(1) : ex.group(2)));
                vo.getEvidence().add(new SpecExtractVO.Evidence("body.lineSpacing", "固定 " + ex.group(0) , s, "high"));
            } else if (mu.find()) {
                String v = mu.group(1) != null ? mu.group(1) : mu.group(2);
                body.put("lineSpacingType", "multiple");
                body.put("lineSpacing", Double.parseDouble(v));
                vo.getEvidence().add(new SpecExtractVO.Evidence("body.lineSpacing", v + " 倍", s, "high"));
            }
        }
        if (s.contains("缩进") && !body.containsKey("firstLineIndent")) {
            Matcher im = INDENT_RE.matcher(s);
            if (im.find()) {
                int n = Integer.parseInt(im.group(1));
                if (n >= 1 && n <= 4) {
                    body.put("firstLineIndent", n);
                    vo.getEvidence().add(new SpecExtractVO.Evidence("body.firstLineIndent", String.valueOf(n), s, "high"));
                }
            }
        }
    }

    // ---------- 标题 ----------

    private void extractHeading(SpecExtractVO vo, String s) {
        if (!s.contains("标题")) {
            return;
        }
        Integer level = null;
        if (s.contains("一级") || s.contains("章标题") || s.contains("各章")) {
            level = 1;
        } else if (s.contains("二级")) {
            level = 2;
        } else if (s.contains("三级")) {
            level = 3;
        }
        String type = level == null ? null : "heading" + level;
        if (type != null) {
            Map<String, Object> rule = vo.getRules().computeIfAbsent(type, k -> new LinkedHashMap<>());
            String font = findFont(s);
            if (font != null && !rule.containsKey("font")) {
                rule.put("font", font);
                vo.getEvidence().add(new SpecExtractVO.Evidence(type + ".font", font, s, "high"));
            }
            Integer size = findSize(s);
            if (size != null && !rule.containsKey("fontSize")) {
                rule.put("fontSize", size);
                vo.getEvidence().add(new SpecExtractVO.Evidence(type + ".fontSize", String.valueOf(size), s, "high"));
            }
            if (s.contains("居中") && !rule.containsKey("align")) {
                rule.put("align", "center");
                vo.getEvidence().add(new SpecExtractVO.Evidence(type + ".align", "center", s, "high"));
            } else if ((s.contains("左对齐") || s.contains("顶格")) && !rule.containsKey("align")) {
                rule.put("align", "left");
            }
        }
        // 编号形式 → 标题正则
        if (!vo.getHeadingPatterns().containsKey("heading1") && (s.contains("第") && s.contains("章") && (s.contains("编号") || s.contains("形式")))) {
            vo.getHeadingPatterns().put("heading1", "^第[一二三四五六七八九十百0-9]+章");
            vo.getHeadingPatterns().putIfAbsent("heading2", "^\\d+\\.\\d+");
            vo.getEvidence().add(new SpecExtractVO.Evidence("heading1.pattern", "第X章", s, "low"));
        }
        if (!vo.getHeadingPatterns().containsKey("heading2") && s.contains("标题") && (s.matches(".*\\b\\d+\\.\\d+\\b.*") || s.contains("1.1"))) {
            vo.getHeadingPatterns().put("heading2", "^\\d+\\.\\d+");
            vo.getEvidence().add(new SpecExtractVO.Evidence("heading2.pattern", "1.1", s, "low"));
        }
        if (!vo.getHeadingPatterns().containsKey("heading3") && s.contains("标题") && (s.matches(".*\\b\\d+\\.\\d+\\.\\d+\\b.*") || s.contains("1.1.1"))) {
            vo.getHeadingPatterns().put("heading3", "^\\d+\\.\\d+\\.\\d+");
            vo.getEvidence().add(new SpecExtractVO.Evidence("heading3.pattern", "1.1.1", s, "low"));
        }
    }

    // ---------- 题注 / 参考文献 ----------

    private void extractCaption(SpecExtractVO vo, String s) {
        if (!s.contains("图") && !s.contains("表")) {
            return;
        }
        String font = findFont(s);
        Integer size = findSize(s);
        if ((s.contains("图题") || s.contains("图的标题") || s.contains("图名")) && !vo.getRules().containsKey("figure")) {
            Map<String, Object> rule = vo.getRules().computeIfAbsent("figure", k -> new LinkedHashMap<>());
            if (font != null) {
                rule.put("font", font);
            }
            if (size != null) {
                rule.put("fontSize", size);
            }
            rule.put("captionPosition", "below");
            vo.getEvidence().add(new SpecExtractVO.Evidence("figure.font", (font == null ? "" : font) + " " + (size == null ? "" : size + "pt"), s, "high"));
        }
        if ((s.contains("表题") || s.contains("表的标题") || s.contains("表名")) && !vo.getRules().containsKey("table")) {
            Map<String, Object> rule = vo.getRules().computeIfAbsent("table", k -> new LinkedHashMap<>());
            if (font != null) {
                rule.put("font", font);
            }
            if (size != null) {
                rule.put("fontSize", size);
            }
            rule.put("captionPosition", "above");
            vo.getEvidence().add(new SpecExtractVO.Evidence("table.font", (font == null ? "" : font) + " " + (size == null ? "" : size + "pt"), s, "high"));
        }
        if (s.contains("图") && s.contains("下") && s.contains("题") && !vo.getPageConfig().containsKey("figurePositionNote")) {
            vo.getEvidence().add(new SpecExtractVO.Evidence("figure.captionPosition", "图题在图下方", s, "high"));
        }
    }

    private void extractReference(SpecExtractVO vo, String s) {
        if (!s.contains("参考文献") || vo.getRefConfig().containsKey("itemFont")) {
            return;
        }
        Map<String, Object> ref = vo.getRefConfig();
        String font = findFont(s);
        if (font != null) {
            ref.put("itemFont", font);
            ref.put("enabled", true);
            vo.getEvidence().add(new SpecExtractVO.Evidence("reference.itemFont", font, s, "high"));
        }
        Integer size = findSize(s);
        if (size != null) {
            ref.put("itemFontSize", size);
        }
    }

    // ---------- 工具 ----------

    private String findFont(String s) {
        Matcher m = FONT_RE.matcher(s);
        if (!m.find()) {
            return null;
        }
        return m.group(1).replace("_GB2312", "");
    }

    private Integer findSize(String s) {
        Matcher cn = SIZE_CN_RE.matcher(s);
        if (cn.find()) {
            Integer v = CN_SIZE.get(cn.group(1));
            if (v != null) {
                return v;
            }
        }
        Matcher pt = SIZE_PT_RE.matcher(s);
        if (pt.find()) {
            double v = Double.parseDouble(pt.group(1));
            if (v >= 7 && v <= 42) {
                return (int) Math.round(v);
            }
        }
        return null;
    }
}
