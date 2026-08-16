package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.RuleSaveDTO;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTemplate;
import com.graduate.thesis.entity.MarketRating;
import com.graduate.thesis.entity.TemplateFavorite;
import com.graduate.thesis.mapper.FormatRuleMapper;
import com.graduate.thesis.mapper.FormatTemplateMapper;
import com.graduate.thesis.mapper.MarketRatingMapper;
import com.graduate.thesis.mapper.TemplateFavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 格式模板服务
 */
@Service
public class TemplateService {

    private final FormatTemplateMapper templateMapper;
    private final FormatRuleMapper ruleMapper;
    private final MarketRatingMapper ratingMapper;
    private final TemplateFavoriteMapper favoriteMapper;
    private final DbRetryService dbRetryService;
    private final TeamService teamService;

    public TemplateService(FormatTemplateMapper templateMapper, FormatRuleMapper ruleMapper,
                           MarketRatingMapper ratingMapper, TemplateFavoriteMapper favoriteMapper,
                           DbRetryService dbRetryService, TeamService teamService) {
        this.templateMapper = templateMapper;
        this.ruleMapper = ruleMapper;
        this.ratingMapper = ratingMapper;
        this.favoriteMapper = favoriteMapper;
        this.dbRetryService = dbRetryService;
        this.teamService = teamService;
    }

    public FormatTemplate create(Long userId, String name, Long teamId) {
        if (teamId != null && !teamService.isMember(teamId, userId)) {
            throw new BusinessException(403, "无权在该团队创建模板");
        }
        FormatTemplate template = new FormatTemplate();
        template.setUserId(userId);
        template.setName(name);
        if (teamId != null) {
            template.setTeamId(teamId);
        }
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(template);
        return template;
    }

    /** 我的模板(个人 + 团队内共享) */
    public List<FormatTemplate> listByUser(Long userId) {
        List<FormatTemplate> mine = templateMapper.selectList(new LambdaQueryWrapper<FormatTemplate>()
                .eq(FormatTemplate::getUserId, userId)
                .isNull(FormatTemplate::getTeamId)
                .orderByDesc(FormatTemplate::getId));
        List<Long> teamIds = teamService.myTeamIds(userId);
        if (!teamIds.isEmpty()) {
            List<FormatTemplate> team = templateMapper.selectList(new LambdaQueryWrapper<FormatTemplate>()
                    .in(FormatTemplate::getTeamId, teamIds)
                    .orderByDesc(FormatTemplate::getId));
            mine.addAll(team);
        }
        return mine;
    }

    public Map<String, Object> detail(Long templateId, Long userId) {
        FormatTemplate template = getOwned(templateId, userId);
        List<FormatRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, templateId));
        Map<String, Object> templateMap = new java.util.HashMap<>();
        templateMap.put("id", template.getId());
        templateMap.put("name", template.getName());
        templateMap.put("pageConfig", template.getPageConfig());
        templateMap.put("headingPatterns", template.getHeadingPatterns());
        templateMap.put("coverConfig", template.getCoverConfig());
        templateMap.put("generateToc", template.getGenerateToc());
        templateMap.put("referenceConfig", template.getReferenceConfig());
        templateMap.put("createTime", template.getCreateTime());
        templateMap.put("rules", rules);
        return java.util.Collections.singletonMap("template", templateMap);
    }

    public void delete(Long templateId, Long userId) {
        getOwned(templateId, userId);
        templateMapper.deleteById(templateId);
        ruleMapper.delete(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, templateId));
    }

    public void savePageConfig(Long templateId, Long userId, String pageConfig) {
        FormatTemplate template = getOwned(templateId, userId);
        template.setPageConfig(pageConfig);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    public void saveHeadingPatterns(Long templateId, Long userId, String headingPatterns) {
        FormatTemplate template = getOwned(templateId, userId);
        template.setHeadingPatterns(headingPatterns);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    public void saveCoverConfig(Long templateId, Long userId, String coverConfig) {
        FormatTemplate template = getOwned(templateId, userId);
        template.setCoverConfig(coverConfig);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    public void saveGenerateToc(Long templateId, Long userId, Boolean generateToc) {
        FormatTemplate template = getOwned(templateId, userId);
        template.setGenerateToc(Boolean.TRUE.equals(generateToc));
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    public void saveReferenceConfig(Long templateId, Long userId, String referenceConfig) {
        FormatTemplate template = getOwned(templateId, userId);
        template.setReferenceConfig(referenceConfig);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    @Transactional
    public FormatRule saveRule(Long userId, RuleSaveDTO dto) {
        getOwned(dto.getTemplateId(), userId);
        FormatRule existing = ruleMapper.selectOne(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, dto.getTemplateId())
                .eq(FormatRule::getRuleType, dto.getRuleType()));
        if (existing == null) {
            existing = new FormatRule();
            existing.setTemplateId(dto.getTemplateId());
            existing.setRuleType(dto.getRuleType());
            existing.setCreateTime(LocalDateTime.now());
        }
        existing.setFont(dto.getFont());
        existing.setFontLatin(dto.getFontLatin());
        existing.setFontSize(dto.getFontSize());
        existing.setBold(dto.getBold());
        existing.setAlign(dto.getAlign());
        existing.setLineSpacing(dto.getLineSpacing());
        existing.setLineSpacingType(dto.getLineSpacingType());
        existing.setLineSpacingExact(dto.getLineSpacingExact());
        existing.setFirstLineIndent(dto.getFirstLineIndent());
        existing.setSpaceBefore(dto.getSpaceBefore());
        existing.setSpaceAfter(dto.getSpaceAfter());
        existing.setCaptionPosition(dto.getCaptionPosition());
        existing.setNumberingPattern(dto.getNumberingPattern());
        existing.setCaptionEnabled(dto.getCaptionEnabled());
        existing.setUpdateTime(LocalDateTime.now());
        if (existing.getId() == null) {
            ruleMapper.insert(existing);
        } else {
            ruleMapper.updateById(existing);
        }
        return existing;
    }

    public FormatRule getRule(Long templateId, String ruleType) {
        return ruleMapper.selectOne(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, templateId)
                .eq(FormatRule::getRuleType, ruleType));
    }

    public List<FormatRule> listRules(Long templateId) {
        return ruleMapper.selectList(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, templateId));
    }

    public FormatTemplate getOwned(Long templateId, Long userId) {
        FormatTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(404, "模板不存在或已被删除");
        }
        boolean mine = template.getUserId().equals(userId);
        boolean teamAccess = template.getTeamId() != null && teamService.isMember(template.getTeamId(), userId);
        if (!mine && !teamAccess) {
            throw new BusinessException(403, "无权访问该模板");
        }
        return template;
    }

    public Map<String, FormatRule> ruleMap(Long templateId) {
        return listRules(templateId).stream()
                .collect(Collectors.toMap(FormatRule::getRuleType, r -> r, (a, b) -> a));
    }

    // ==================== 模板市场 ====================

    /** 模板市场公开模板列表 */
    public List<Map<String, Object>> listPublicTemplates(String category, String sort) {
        List<FormatTemplate> list = templateMapper.selectList(new LambdaQueryWrapper<FormatTemplate>()
                .eq(FormatTemplate::getIsPublic, true)
                .eq(category != null && !category.isEmpty(), FormatTemplate::getCategory, category)
                .orderByAsc(FormatTemplate::getId));
        // 排序: recommended(推荐优先,默认) / downloads(下载量) / rating(评分) / newest(最新上架)
        String s = sort == null ? "recommended" : sort;
        if ("downloads".equals(s)) {
            list.sort((a, b) -> Integer.compare(nvl(b.getDownloadCount()), nvl(a.getDownloadCount())));
        } else if ("rating".equals(s)) {
            list.sort((a, b) -> {
                int c = nvl2(b.getRatingAvg()).compareTo(nvl2(a.getRatingAvg()));
                return c != 0 ? c : Integer.compare(nvl(b.getRatingCount()), nvl(a.getRatingCount()));
            });
        } else if ("newest".equals(s)) {
            list.sort((a, b) -> nvl3(b.getPublicTime()).compareTo(nvl3(a.getPublicTime())));
        } else {
            list.sort((a, b) -> {
                int r = Boolean.compare(Boolean.TRUE.equals(b.getRecommended()), Boolean.TRUE.equals(a.getRecommended()));
                return r != 0 ? r : nvl3(b.getPublicTime()).compareTo(nvl3(a.getPublicTime()));
            });
        }
        return list.stream().map(t -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("category", t.getCategory());
            m.put("recommended", Boolean.TRUE.equals(t.getRecommended()));
            m.put("publicTime", t.getPublicTime());
            m.put("downloadCount", nvl(t.getDownloadCount()));
            m.put("ratingAvg", nvl2(t.getRatingAvg()).doubleValue());
            m.put("ratingCount", nvl(t.getRatingCount()));
            m.put("ruleCount", ruleMapper.selectCount(new LambdaQueryWrapper<FormatRule>()
                    .eq(FormatRule::getTemplateId, t.getId())));
            return m;
        }).collect(Collectors.toList());
    }

    /** 模板市场分类列表 */
    public List<String> listMarketCategories() {
        return java.util.Arrays.asList("毕业论文", "期刊论文", "报告文档", "其他");
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private static BigDecimal nvl2(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static LocalDateTime nvl3(LocalDateTime v) {
        return v == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : v;
    }

    /** 复制公开模板到自己的模板(含规则) */
    @Transactional
    public FormatTemplate copyPublic(Long userId, Long publicTemplateId) {
        FormatTemplate source = templateMapper.selectById(publicTemplateId);
        if (source == null || !Boolean.TRUE.equals(source.getIsPublic())) {
            throw new BusinessException(404, "模板不存在或未上架");
        }
        FormatTemplate copy = new FormatTemplate();
        copy.setUserId(userId);
        copy.setName(source.getName() + "（副本）");
        copy.setPageConfig(source.getPageConfig());
        copy.setHeadingPatterns(source.getHeadingPatterns());
        copy.setCoverConfig(source.getCoverConfig());
        copy.setGenerateToc(source.getGenerateToc());
        copy.setReferenceConfig(source.getReferenceConfig());
        copy.setCategory(source.getCategory());
        copy.setCreateTime(LocalDateTime.now());
        copy.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(copy);

        // 复制成功计为一次市场下载
        if (source.getDownloadCount() == null) {
            source.setDownloadCount(0);
        }
        source.setDownloadCount(source.getDownloadCount() + 1);
        source.setUpdateTime(LocalDateTime.now());
        dbRetryService.run(() -> templateMapper.updateById(source));

        List<FormatRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, publicTemplateId));
        for (FormatRule r : rules) {
            FormatRule nr = new FormatRule();
            nr.setTemplateId(copy.getId());
            nr.setRuleType(r.getRuleType());
            nr.setFont(r.getFont());
            nr.setFontLatin(r.getFontLatin());
            nr.setFontSize(r.getFontSize());
            nr.setBold(r.getBold());
            nr.setAlign(r.getAlign());
            nr.setLineSpacing(r.getLineSpacing());
            nr.setLineSpacingType(r.getLineSpacingType());
            nr.setLineSpacingExact(r.getLineSpacingExact());
            nr.setFirstLineIndent(r.getFirstLineIndent());
            nr.setSpaceBefore(r.getSpaceBefore());
            nr.setSpaceAfter(r.getSpaceAfter());
            nr.setCaptionPosition(r.getCaptionPosition());
            nr.setNumberingPattern(r.getNumberingPattern());
            nr.setCaptionEnabled(r.getCaptionEnabled());
            nr.setCreateTime(LocalDateTime.now());
            nr.setUpdateTime(LocalDateTime.now());
            ruleMapper.insert(nr);
        }
        return copy;
    }

    /**
     * 市场模板评分(1~5): 同一用户重复评分则覆盖, 并重算模板平均分.
     */
    @Transactional
    public void ratePublic(Long userId, Long templateId, int score) {
        if (score < 1 || score > 5) {
            throw new BusinessException("评分范围为 1~5");
        }
        FormatTemplate template = templateMapper.selectById(templateId);
        if (template == null || !Boolean.TRUE.equals(template.getIsPublic())) {
            throw new BusinessException(404, "模板不存在或未上架");
        }
        MarketRating exist = ratingMapper.selectOne(new LambdaQueryWrapper<MarketRating>()
                .eq(MarketRating::getUserId, userId)
                .eq(MarketRating::getTemplateId, templateId));
        MarketRating rate = new MarketRating();
        rate.setUserId(userId);
        rate.setTemplateId(templateId);
        rate.setScore(score);
        if (exist == null) {
            rate.setCreateTime(LocalDateTime.now());
            ratingMapper.insert(rate);
        } else {
            rate.setCreateTime(exist.getCreateTime());
            ratingMapper.update(rate, new LambdaQueryWrapper<MarketRating>()
                    .eq(MarketRating::getUserId, userId)
                    .eq(MarketRating::getTemplateId, templateId));
        }
        // 重算平均分
        List<MarketRating> all = ratingMapper.selectList(new LambdaQueryWrapper<MarketRating>()
                .eq(MarketRating::getTemplateId, templateId));
        double sum = 0;
        for (MarketRating r : all) {
            sum += r.getScore();
        }
        BigDecimal avg = BigDecimal.valueOf(sum / all.size()).setScale(1, RoundingMode.HALF_UP);
        template.setRatingAvg(avg);
        template.setRatingCount(all.size());
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    // ==================== 收藏 ====================

    /** 收藏/取消收藏市场模板, 返回是否已收藏 */
    public boolean toggleFavorite(Long userId, Long templateId) {
        FormatTemplate t = templateMapper.selectById(templateId);
        if (t == null || !Boolean.TRUE.equals(t.getIsPublic())) {
            throw new BusinessException(404, "模板不存在或未上架");
        }
        TemplateFavorite exist = favoriteMapper.selectOne(new LambdaQueryWrapper<TemplateFavorite>()
                .eq(TemplateFavorite::getUserId, userId)
                .eq(TemplateFavorite::getTemplateId, templateId));
        if (exist != null) {
            favoriteMapper.delete(new LambdaQueryWrapper<TemplateFavorite>()
                    .eq(TemplateFavorite::getUserId, userId)
                    .eq(TemplateFavorite::getTemplateId, templateId));
            return false;
        }
        TemplateFavorite f = new TemplateFavorite();
        f.setUserId(userId);
        f.setTemplateId(templateId);
        f.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(f);
        return true;
    }

    /** 我的收藏列表(仅展示仍在市场上架的模板) */
    public List<Map<String, Object>> listFavorites(Long userId) {
        List<TemplateFavorite> favs = favoriteMapper.selectList(new LambdaQueryWrapper<TemplateFavorite>()
                .eq(TemplateFavorite::getUserId, userId)
                .orderByDesc(TemplateFavorite::getCreateTime));
        if (favs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = favs.stream().map(TemplateFavorite::getTemplateId).collect(Collectors.toList());
        Map<Long, FormatTemplate> map = templateMapper.selectList(
                        new LambdaQueryWrapper<FormatTemplate>().in(FormatTemplate::getId, ids))
                .stream().collect(Collectors.toMap(FormatTemplate::getId, x -> x, (a, b) -> a));
        List<Map<String, Object>> out = new ArrayList<>();
        for (TemplateFavorite f : favs) {
            FormatTemplate t = map.get(f.getTemplateId());
            if (t == null || !Boolean.TRUE.equals(t.getIsPublic())) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("category", t.getCategory());
            m.put("recommended", Boolean.TRUE.equals(t.getRecommended()));
            m.put("publicTime", t.getPublicTime());
            m.put("downloadCount", nvl(t.getDownloadCount()));
            m.put("ratingAvg", nvl2(t.getRatingAvg()).doubleValue());
            m.put("ratingCount", nvl(t.getRatingCount()));
            m.put("ruleCount", ruleMapper.selectCount(new LambdaQueryWrapper<FormatRule>()
                    .eq(FormatRule::getTemplateId, t.getId())));
            out.add(m);
        }
        return out;
    }

    // ==================== 导入 / 导出 ====================

    /** 导出模板(含全部配置与规则)为 JSON */
    public Map<String, Object> exportTemplate(Long userId, Long templateId) {
        FormatTemplate t = getOwned(templateId, userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "thesis-template");
        m.put("version", 1);
        m.put("name", t.getName());
        m.put("pageConfig", t.getPageConfig());
        m.put("headingPatterns", t.getHeadingPatterns());
        m.put("coverConfig", t.getCoverConfig());
        m.put("generateToc", t.getGenerateToc());
        m.put("referenceConfig", t.getReferenceConfig());
        m.put("category", t.getCategory());
        m.put("rules", ruleMapper.selectList(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, templateId)));
        return m;
    }

    /** 从 JSON 导入模板(含规则), 返回新模板 */
    @Transactional
    public FormatTemplate importTemplate(Long userId, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            throw new BusinessException("导入数据不能为空");
        }
        String name = data.get("name") == null ? "" : String.valueOf(data.get("name")).trim();
        if (name.isEmpty()) {
            name = "导入的模板";
        }
        FormatTemplate t = new FormatTemplate();
        t.setUserId(userId);
        t.setName(name);
        t.setPageConfig(str(data.get("pageConfig")));
        t.setHeadingPatterns(str(data.get("headingPatterns")));
        t.setCoverConfig(str(data.get("coverConfig")));
        Object gt = data.get("generateToc");
        t.setGenerateToc(gt != null && (Boolean.TRUE.equals(gt) || "true".equalsIgnoreCase(String.valueOf(gt))));
        t.setReferenceConfig(str(data.get("referenceConfig")));
        Object cat = data.get("category");
        t.setCategory(cat == null ? null : String.valueOf(cat).trim());
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(t);
        Object rulesObj = data.get("rules");
        if (rulesObj instanceof List) {
            for (Object o : (List<?>) rulesObj) {
                if (!(o instanceof Map)) {
                    continue;
                }
                Map<?, ?> rm = (Map<?, ?>) o;
                FormatRule nr = new FormatRule();
                nr.setTemplateId(t.getId());
                nr.setRuleType(str(rm.get("ruleType")));
                nr.setFont(str(rm.get("font")));
                nr.setFontLatin(str(rm.get("fontLatin")));
                nr.setFontSize(intOf(rm.get("fontSize")));
                nr.setBold(boolOf(rm.get("bold")));
                nr.setAlign(str(rm.get("align")));
                nr.setLineSpacing(floatOf(rm.get("lineSpacing")));
                nr.setLineSpacingType(str(rm.get("lineSpacingType")));
                nr.setLineSpacingExact(intOf(rm.get("lineSpacingExact")));
                nr.setFirstLineIndent(intOf(rm.get("firstLineIndent")));
                nr.setSpaceBefore(intOf(rm.get("spaceBefore")));
                nr.setSpaceAfter(intOf(rm.get("spaceAfter")));
                nr.setCaptionPosition(str(rm.get("captionPosition")));
                nr.setNumberingPattern(str(rm.get("numberingPattern")));
                nr.setCaptionEnabled(boolOf(rm.get("captionEnabled")));
                nr.setCreateTime(LocalDateTime.now());
                nr.setUpdateTime(LocalDateTime.now());
                ruleMapper.insert(nr);
            }
        }
        return t;
    }

    /** 市场模板详情(公开, 无需登录): 用于市场详情弹窗 */
    public Map<String, Object> marketDetail(Long templateId) {
        FormatTemplate t = templateMapper.selectById(templateId);
        if (t == null || !Boolean.TRUE.equals(t.getIsPublic())) {
            throw new BusinessException(404, "模板不存在或未上架");
        }
        List<FormatRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, templateId));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("category", t.getCategory());
        m.put("recommended", Boolean.TRUE.equals(t.getRecommended()));
        m.put("publicTime", t.getPublicTime());
        m.put("downloadCount", nvl(t.getDownloadCount()));
        m.put("ratingAvg", nvl2(t.getRatingAvg()).doubleValue());
        m.put("ratingCount", nvl(t.getRatingCount()));
        m.put("ruleCount", rules.size());
        m.put("generateToc", t.getGenerateToc());
        m.put("pageConfig", t.getPageConfig());
        m.put("headingPatterns", t.getHeadingPatterns());
        m.put("referenceConfig", t.getReferenceConfig());
        m.put("rules", rules);
        return m;
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Integer intOf(Object v) {
        if (v == null || String.valueOf(v).isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(String.valueOf(v)).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    private static Float floatOf(Object v) {
        if (v == null || String.valueOf(v).isEmpty()) {
            return null;
        }
        try {
            return Float.valueOf(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static Boolean boolOf(Object v) {
        if (v == null) {
            return null;
        }
        return Boolean.TRUE.equals(v) || "true".equalsIgnoreCase(String.valueOf(v));
    }
}
