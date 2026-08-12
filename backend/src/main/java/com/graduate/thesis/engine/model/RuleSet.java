package com.graduate.thesis.engine.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTemplate;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 排版规则集: 由格式模板 + 规则列表构建
 */
@Getter
public class RuleSet {

    public static final String DEFAULT_HEADING1 = "^第[一二三四五六七八九十百]+章.*";
    public static final String DEFAULT_HEADING2 = "^\\d+\\.\\d+.*";
    public static final String DEFAULT_HEADING3 = "^\\d+\\.\\d+\\.\\d+.*";

    private final Long templateId;
    private final String templateName;
    private final PageConfig pageConfig;
    private final Map<String, FormatRule> rules;
    private final Pattern heading1Pattern;
    private final Pattern heading2Pattern;
    private final Pattern heading3Pattern;
    private final CoverConfig coverConfig;
    private final ReferenceConfig referenceConfig;
    private final boolean generateToc;

    private RuleSet(Long templateId, String templateName, PageConfig pageConfig,
                    Map<String, FormatRule> rules,
                    Pattern heading1Pattern, Pattern heading2Pattern, Pattern heading3Pattern,
                    CoverConfig coverConfig, ReferenceConfig referenceConfig, boolean generateToc) {
        this.templateId = templateId;
        this.templateName = templateName;
        this.pageConfig = pageConfig;
        this.rules = rules;
        this.heading1Pattern = heading1Pattern;
        this.heading2Pattern = heading2Pattern;
        this.heading3Pattern = heading3Pattern;
        this.coverConfig = coverConfig;
        this.referenceConfig = referenceConfig;
        this.generateToc = generateToc;
    }

    public static RuleSet from(FormatTemplate template, List<FormatRule> rules) {
        ObjectMapper mapper = new ObjectMapper();
        PageConfig pageConfig = new PageConfig();
        if (template.getPageConfig() != null && !template.getPageConfig().trim().isEmpty()) {
            try {
                pageConfig = mapper.readValue(template.getPageConfig(), PageConfig.class);
            } catch (Exception ignore) {
                // 配置损坏时使用默认
            }
        }

        String h1 = DEFAULT_HEADING1;
        String h2 = DEFAULT_HEADING2;
        String h3 = DEFAULT_HEADING3;
        if (template.getHeadingPatterns() != null && !template.getHeadingPatterns().trim().isEmpty()) {
            try {
                JsonNode node = mapper.readTree(template.getHeadingPatterns());
                h1 = patternValue(node, "heading1", h1);
                h2 = patternValue(node, "heading2", h2);
                h3 = patternValue(node, "heading3", h3);
            } catch (Exception ignore) {
                // 配置损坏时使用默认
            }
        }

        Map<String, FormatRule> ruleMap = rules.stream()
                .collect(Collectors.toMap(FormatRule::getRuleType, r -> r, (a, b) -> a));
        return new RuleSet(template.getId(), template.getName(), pageConfig, ruleMap,
                compileHeading(h1), compileHeading(h2), compileHeading(h3),
                CoverConfig.parse(template.getCoverConfig()),
                ReferenceConfig.parse(template.getReferenceConfig()),
                Boolean.TRUE.equals(template.getGenerateToc()));
    }

    private static String patternValue(JsonNode node, String key, String defaultValue) {
        JsonNode v = node.get(key);
        if (v != null && !v.asText().trim().isEmpty()) {
            return v.asText().trim();
        }
        return defaultValue;
    }

    /**
     * 编译标题识别正则: matches() 是全串匹配, 自动补 .* 让用户可只写前缀(如 ^\d+ 匹配 "1 绪论")
     */
    private static Pattern compileHeading(String regex) {
        String r = regex == null || regex.trim().isEmpty() ? "$^" : regex.trim();
        if (!r.endsWith("$") && !r.endsWith("\\z") && !r.endsWith(".*")) {
            r = r + ".*";
        }
        try {
            return Pattern.compile(r);
        } catch (Exception e) {
            return Pattern.compile("$^");
        }
    }

    /**
     * 获取规则; 缺失时返回默认规范值, 避免 NPE
     */
    public FormatRule rule(String ruleType) {
        FormatRule rule = rules.get(ruleType);
        if (rule != null) {
            return rule;
        }
        FormatRule fallback = new FormatRule();
        fallback.setRuleType(ruleType);
        fallback.setFont("宋体");
        fallback.setFontLatin("Times New Roman");
        fallback.setFontSize(12);
        fallback.setBold(false);
        fallback.setAlign("left");
        fallback.setLineSpacing(1.5f);
        fallback.setLineSpacingType("multiple");
        fallback.setFirstLineIndent(2);
        fallback.setSpaceAfter(0);
        fallback.setCaptionEnabled(true);
        rules.put(ruleType, fallback);
        return fallback;
    }
}
