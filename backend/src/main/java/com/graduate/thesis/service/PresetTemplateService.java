package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTemplate;
import com.graduate.thesis.mapper.FormatRuleMapper;
import com.graduate.thesis.mapper.FormatTemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置格式模板预设。
 *
 * <p>数据来源：桂林信息科技学院《毕业设计（论文）说明书统一格式的规定》（附件6，20250525 版）。
 * 关键参数：</p>
 * <ul>
 *   <li>页面：A4，上下左右页边距各 2.5cm，行间距固定值 20 磅，页脚居中页码</li>
 *   <li>章节级序：1 / 1.1 / 1.1.1 / (1) / ①</li>
 *   <li>一级标题：黑体四号（14pt）；二级：黑体小四（12pt）；三级：楷体小四（12pt）</li>
 *   <li>正文：宋体小四（12pt），首行缩进 2 字；正文英文字体用 Times New Roman</li>
 *   <li>摘要标题：黑体三号（16pt）居中；摘要内容宋体小四；Abstract 用 Times New Roman 三号加粗</li>
 *   <li>目录：1.5 倍行距，一级四号黑体、二级小四黑体、三级楷体小四，数字用 Times New Roman</li>
 *   <li>图表题注：五号宋体（10.5pt），图注在图下方、表注在表格上方，编号 "图1-1" / "表1-1"</li>
 *   <li>谢辞/参考文献/附录：另起一页，标题黑体四号；参考文献条目五号宋体（外文 Times New Roman）</li>
 * </ul>
 */
@Service
public class PresetTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PresetTemplateService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 预设标识 */
    public static final String GUILIN_PRESET_KEY = "guilin-institute";

    /** 预设名称：新用户注册时自动创建的模板名 */
    public static final String GUILIN_TEMPLATE_NAME = "桂林信息科技学院毕业论文格式";

    private final FormatTemplateMapper templateMapper;
    private final FormatRuleMapper ruleMapper;

    public PresetTemplateService(FormatTemplateMapper templateMapper, FormatRuleMapper ruleMapper) {
        this.templateMapper = templateMapper;
        this.ruleMapper = ruleMapper;
    }

    /**
     * 为新用户初始化一份归属本人的桂林信息科技学院模板。
     * 独立事务 + 失败不抛出：模板创建异常不能阻断用户注册。
     *
     * @return 新建模板 id，失败或已存在时返回 null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long provisionForNewUser(Long userId) {
        Long existing = templateMapper.selectCount(new LambdaQueryWrapper<FormatTemplate>()
                .eq(FormatTemplate::getUserId, userId)
                .eq(FormatTemplate::getName, GUILIN_TEMPLATE_NAME));
        if (existing != null && existing > 0) {
            return null;
        }
        return createTemplate(userId, GUILIN_TEMPLATE_NAME, null);
    }

    /**
     * 按预设创建一份完整模板（含 7 条格式规则与 4 段配置 JSON）。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createTemplate(Long userId, String name, Long teamId) {
        LocalDateTime now = LocalDateTime.now();
        FormatTemplate template = new FormatTemplate();
        template.setUserId(userId);
        template.setName(name);
        template.setTeamId(teamId);
        template.setPageConfig(guilinPageConfig());
        template.setHeadingPatterns(guilinHeadingPatterns());
        template.setGenerateToc(true);
        template.setGenerateAbstract(true);
        template.setReferenceConfig(guilinReferenceConfig());
        template.setTocConfig(guilinTocConfig());
        template.setIsPublic(false);
        template.setRecommended(false);
        template.setRatingAvg(java.math.BigDecimal.ZERO);
        template.setRatingCount(0);
        template.setCreateTime(now);
        template.setUpdateTime(now);
        templateMapper.insert(template);

        for (FormatRule rule : guilinRules(template.getId())) {
            rule.setCreateTime(now);
            rule.setUpdateTime(now);
            ruleMapper.insert(rule);
        }
        return template.getId();
    }

    // ================= 配置内容 =================

    /** 页面设置：A4、四边 2.5cm、页脚居中页码、摘要与谢辞/参考文献/附录区段 */
    public static String guilinPageConfig() {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("paper", "A4");

        Map<String, Object> margin = new LinkedHashMap<>();
        margin.put("top", 2.5);
        margin.put("bottom", 2.5);
        margin.put("left", 2.5);
        margin.put("right", 2.5);
        page.put("margin", margin);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("height", 1.5);
        header.put("text", "");
        page.put("header", header);

        Map<String, Object> footer = new LinkedHashMap<>();
        footer.put("pageNumber", "center");
        footer.put("font", "宋体");
        footer.put("fontSize", 10.5);
        footer.put("skipFirst", false);
        footer.put("format", "decimal");
        page.put("footer", footer);

        // 摘要：标题三号黑体居中；内容小四宋体；Abstract 三号 TNR 加粗，内容小四 TNR
        Map<String, Object> abs = new LinkedHashMap<>();
        abs.put("zhTitleFont", "黑体");
        abs.put("zhTitleFontSize", 16);
        abs.put("zhTitleBold", true);
        abs.put("zhTitleText", "摘  要");
        abs.put("zhBodyFont", "宋体");
        abs.put("zhBodyFontSize", 12);
        abs.put("keywordLabelFont", "黑体");
        abs.put("keywordBodyFont", "宋体");
        abs.put("keywordFontSize", 12);
        abs.put("enTitleFont", "Times New Roman");
        abs.put("enTitleFontSize", 16);
        abs.put("enTitleBold", true);
        abs.put("enBodyFont", "Times New Roman");
        abs.put("enBodyFontSize", 12);
        abs.put("enKeywordLabelFont", "Times New Roman");
        abs.put("enKeywordBodyFont", "Times New Roman");
        abs.put("enKeywordFontSize", 12);
        abs.put("titlePageBreak", true);
        page.put("abstract", abs);

        // 固定章节：谢辞居中、参考文献顶格、附录居中，均黑体四号并另起一页
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("font", "黑体");
        section.put("fontSize", 14);
        section.put("bold", true);
        section.put("pageBreakBefore", true);
        section.put("outlineLevel", 0);
        section.put("xieCi", "谢  辞");
        section.put("references", "参考文献");
        section.put("appendix", "附  录");
        section.put("xieCiAlign", "center");
        section.put("referencesAlign", "left");
        section.put("appendixAlign", "center");
        page.put("section", section);

        return toJson(page);
    }

    /** 标题识别：1 / 1.1 / 1.1.1 三级编号体系（校规第三章节目序号级序） */
    public static String guilinHeadingPatterns() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("heading1", "^\\d+");
        m.put("heading2", "^\\d+\\.\\d+");
        m.put("heading3", "^\\d+\\.\\d+\\.\\d+");
        return toJson(m);
    }

    /** 目录：1.5 倍行距；一级四号黑体、二级小四黑体、三级楷体小四；数字用 TNR */
    public static String guilinTocConfig() {
        Map<String, Object> toc = new LinkedHashMap<>();
        toc.put("lineSpacing", 1.5);
        toc.put("leader", "dot");
        toc.put("toc1", tocLevel("黑体", 14));
        toc.put("toc2", tocLevel("黑体", 12));
        toc.put("toc3", tocLevel("楷体", 12));
        return toJson(toc);
    }

    /** 参考文献：标题黑体四号，条目五号宋体/外文 TNR，序号悬挂对齐，作者写到第三位 */
    public static String guilinReferenceConfig() {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("enabled", true);
        ref.put("title", "参考文献");
        ref.put("titleFont", "黑体");
        ref.put("titleFontSize", 14);
        ref.put("itemFont", "宋体");
        ref.put("itemFontLatin", "Times New Roman");
        ref.put("itemFontSize", 10.5);
        ref.put("removeDoi", true);
        ref.put("maxAuthors", 3);
        ref.put("renumber", true);
        ref.put("hangingIndentTwips", 640);
        ref.put("lineSpacing", 1.5);
        return toJson(ref);
    }

    private static Map<String, Object> tocLevel(String font, int size) {
        Map<String, Object> lv = new LinkedHashMap<>();
        lv.put("font", font);
        lv.put("fontLatin", "Times New Roman");
        lv.put("fontSize", size);
        return lv;
    }

    /** 7 条格式规则：标题三级 + 正文 + 图/表题注 + 表格文字 */
    public static List<FormatRule> guilinRules(Long templateId) {
        List<FormatRule> list = new ArrayList<>(7);

        // 一级标题：黑体四号，顶格，固定 20 磅行距
        list.add(rule(templateId, "heading1", "黑体", 14.0, false, "left", 0, 0, null, null));
        // 二级标题：黑体小四
        list.add(rule(templateId, "heading2", "黑体", 12.0, false, "left", 0, 0, null, null));
        // 三级标题：楷体小四
        list.add(rule(templateId, "heading3", "楷体", 12.0, false, "left", 0, 0, null, null));
        // 正文：宋体小四，首行缩进 2 字，两端对齐
        list.add(rule(templateId, "body", "宋体", 12.0, false, "justify", 2, 0, null, null));
        // 图题注：五号宋体，图下方
        list.add(rule(templateId, "figure", "宋体", 10.5, false, "center", 0, 0, "below", "图{chapter}-{no}"));
        // 表题注：五号宋体，表格上方
        list.add(rule(templateId, "table", "宋体", 10.5, false, "center", 0, 0, "above", "表{chapter}-{no}"));
        // 表格文字：五号宋体居中
        list.add(rule(templateId, "tableText", "宋体", 10.5, false, "center", 0, 0, null, null));

        return list;
    }

    private static FormatRule rule(Long templateId, String ruleType, String font, Double fontSize,
                                   boolean bold, String align, int firstLineIndent, int spaceAfter,
                                   String captionPosition, String numberingPattern) {
        FormatRule r = new FormatRule();
        r.setTemplateId(templateId);
        r.setRuleType(ruleType);
        r.setFont(font);
        r.setFontLatin("Times New Roman");
        r.setFontSize(fontSize);
        r.setBold(bold);
        r.setAlign(align);
        // 校规：全文行间距取固定值 20 磅
        r.setLineSpacingType("exact");
        r.setLineSpacingExact(20);
        r.setLineSpacing(1.0f);
        r.setFirstLineIndent(firstLineIndent);
        r.setSpaceBefore(0);
        r.setSpaceAfter(spaceAfter);
        r.setCaptionPosition(captionPosition);
        r.setNumberingPattern(numberingPattern);
        r.setCaptionEnabled(true);
        return r;
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("预设配置序列化失败", e);
        }
    }
}
