package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.ConfigSaveDTO;
import com.graduate.thesis.dto.RuleSaveDTO;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.FormatTemplate;
import com.graduate.thesis.entity.MarketComment;
import com.graduate.thesis.entity.MarketLike;
import com.graduate.thesis.entity.MarketRating;
import com.graduate.thesis.entity.TemplateFavorite;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.mapper.FormatRuleMapper;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.mapper.FormatTemplateMapper;
import com.graduate.thesis.mapper.MarketCommentMapper;
import com.graduate.thesis.mapper.MarketLikeMapper;
import com.graduate.thesis.mapper.MarketRatingMapper;
import com.graduate.thesis.mapper.TemplateFavoriteMapper;
import com.graduate.thesis.mapper.UserMapper;
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
    private final FormatTaskMapper taskMapper;
    private final MarketRatingMapper ratingMapper;
    private final TemplateFavoriteMapper favoriteMapper;
    private final MarketCommentMapper commentMapper;
    private final MarketLikeMapper likeMapper;
    private final UserMapper userMapper;
    private final DbRetryService dbRetryService;
    private final TeamService teamService;
    private final NotificationService notificationService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public TemplateService(FormatTemplateMapper templateMapper, FormatRuleMapper ruleMapper,
                           FormatTaskMapper taskMapper,
                           MarketRatingMapper ratingMapper, TemplateFavoriteMapper favoriteMapper,
                           MarketCommentMapper commentMapper, MarketLikeMapper likeMapper,
                           UserMapper userMapper,
                           DbRetryService dbRetryService, TeamService teamService,
                           NotificationService notificationService,
                           com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.ruleMapper = ruleMapper;
        this.taskMapper = taskMapper;
        this.ratingMapper = ratingMapper;
        this.favoriteMapper = favoriteMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.userMapper = userMapper;
        this.dbRetryService = dbRetryService;
        this.teamService = teamService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
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
        templateMap.put("generateToc", template.getGenerateToc());
        templateMap.put("generateAbstract", template.getGenerateAbstract());
        templateMap.put("referenceConfig", template.getReferenceConfig());
        templateMap.put("tocConfig", template.getTocConfig());
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

    public void saveHeadingPatterns(Long templateId, Long userId, String heading1, String heading2, String heading3) {
        FormatTemplate template = getOwned(templateId, userId);
        template.setHeadingPatterns(buildHeadingPatternsJson(heading1, heading2, heading3));
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    /** 用 ObjectMapper 生成标题正则 JSON, 避免手工拼接转义出错 */
    private String buildHeadingPatternsJson(String heading1, String heading2, String heading3) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("heading1", heading1 == null ? "" : heading1);
        m.put("heading2", heading2 == null ? "" : heading2);
        m.put("heading3", heading3 == null ? "" : heading3);
        try {
            return objectMapper.writeValueAsString(m);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BusinessException(500, "标题规则序列化失败");
        }
    }

    public void saveGenerateToc(Long templateId, Long userId, Boolean generateToc) {
        FormatTemplate template = getOwned(templateId, userId);
        template.setGenerateToc(Boolean.TRUE.equals(generateToc));
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    public void saveGenerateAbstract(Long templateId, Long userId, Boolean generateAbstract) {
        FormatTemplate template = getOwned(templateId, userId);
        template.setGenerateAbstract(Boolean.TRUE.equals(generateAbstract));
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    public void saveReferenceConfig(Long templateId, Long userId, String referenceConfig) {
        FormatTemplate template = getOwned(templateId, userId);
        template.setReferenceConfig(referenceConfig);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    public void saveTocConfig(Long templateId, Long userId, String tocConfig) {
        FormatTemplate template = getOwned(templateId, userId);
        template.setTocConfig(tocConfig);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    /** 一次性保存模板全部配置(单事务): 任一项失败整体回滚, 前端不再逐项发十几个请求 */
    @Transactional
    public void saveAllConfig(Long templateId, Long userId, ConfigSaveDTO dto) {
        FormatTemplate template = getOwned(templateId, userId);
        if (dto.getPageConfig() != null) {
            template.setPageConfig(dto.getPageConfig());
        }
        if (dto.getHeading1() != null || dto.getHeading2() != null || dto.getHeading3() != null) {
            template.setHeadingPatterns(buildHeadingPatternsJson(dto.getHeading1(), dto.getHeading2(), dto.getHeading3()));
        }
        if (dto.getGenerateToc() != null) {
            template.setGenerateToc(dto.getGenerateToc());
        }
        if (dto.getGenerateAbstract() != null) {
            template.setGenerateAbstract(dto.getGenerateAbstract());
        }
        if (dto.getReferenceConfig() != null) {
            template.setReferenceConfig(dto.getReferenceConfig());
        }
        if (dto.getTocConfig() != null) {
            template.setTocConfig(dto.getTocConfig());
        }
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
        // 团队模板修订提示: 一人修改, 通知其他成员"配置已变更", 避免按旧印象排版
        if (template.getTeamId() != null) {
            try {
                User modifier = userMapper.selectById(userId);
                String modifierName = modifier != null && modifier.getUsername() != null ? modifier.getUsername() : ("用户" + userId);
                for (Long mid : teamService.memberIds(template.getTeamId())) {
                    if (mid.equals(userId)) {
                        continue;
                    }
                    notificationService.send(mid, "TEMPLATE_UPDATE", "团队模板已更新",
                            "格式方案「" + template.getName() + "」已被 " + modifierName + " 修改，排版前请注意核对配置",
                            java.util.Map.of("templateId", templateId));
                }
            } catch (Exception ignore) {
                // 通知失败不影响保存
            }
        }
        if (dto.getRules() != null) {
            for (RuleSaveDTO rule : dto.getRules()) {
                upsertRule(templateId, rule);
            }
        }
    }

    @Transactional
    public FormatRule saveRule(Long userId, RuleSaveDTO dto) {
        getOwned(dto.getTemplateId(), userId);
        return upsertRule(dto.getTemplateId(), dto);
    }

    /**
     * 幂等保存单条规则: 按 (template_id, rule_type) 先查后写;
     * 并发插入撞唯一键时回退为更新, 避免 DuplicateKeyException 冒泡成 500
     */
    private FormatRule upsertRule(Long templateId, RuleSaveDTO dto) {
        FormatRule existing = ruleMapper.selectOne(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, templateId)
                .eq(FormatRule::getRuleType, dto.getRuleType()));
        if (existing == null) {
            existing = new FormatRule();
            existing.setTemplateId(templateId);
            existing.setRuleType(dto.getRuleType());
            existing.setCreateTime(LocalDateTime.now());
            applyRuleFields(existing, dto);
            try {
                ruleMapper.insert(existing);
                return existing;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                existing = ruleMapper.selectOne(new LambdaQueryWrapper<FormatRule>()
                        .eq(FormatRule::getTemplateId, templateId)
                        .eq(FormatRule::getRuleType, dto.getRuleType()));
                if (existing == null) {
                    throw e;
                }
            }
        }
        applyRuleFields(existing, dto);
        ruleMapper.updateById(existing);
        return existing;
    }

    private void applyRuleFields(FormatRule rule, RuleSaveDTO dto) {
        rule.setFont(dto.getFont());
        rule.setFontLatin(dto.getFontLatin());
        rule.setFontSize(dto.getFontSize());
        rule.setBold(dto.getBold());
        rule.setAlign(dto.getAlign());
        rule.setLineSpacing(dto.getLineSpacing());
        rule.setLineSpacingType(dto.getLineSpacingType());
        rule.setLineSpacingExact(dto.getLineSpacingExact());
        rule.setFirstLineIndent(dto.getFirstLineIndent());
        rule.setSpaceBefore(dto.getSpaceBefore());
        rule.setSpaceAfter(dto.getSpaceAfter());
        rule.setCaptionPosition(dto.getCaptionPosition());
        rule.setNumberingPattern(dto.getNumberingPattern());
        rule.setCaptionEnabled(dto.getCaptionEnabled());
        rule.setUpdateTime(LocalDateTime.now());
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
        List<Long> ids = list.stream().map(FormatTemplate::getId).collect(Collectors.toList());
        Map<Long, Long> usage = marketUsageCounts(ids);
        // 排序: recommended(推荐优先,默认) / usage(使用量) / rating(评分) / newest(最新上架)
        String s = sort == null ? "recommended" : sort;
        if ("usage".equals(s)) {
            list.sort((a, b) -> Long.compare(usage.getOrDefault(b.getId(), 0L), usage.getOrDefault(a.getId(), 0L)));
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
            m.put("usageCount", usage.getOrDefault(t.getId(), 0L));
            m.put("ratingAvg", nvl2(t.getRatingAvg()).doubleValue());
            m.put("ratingCount", nvl(t.getRatingCount()));
            m.put("ruleCount", ruleMapper.selectCount(new LambdaQueryWrapper<FormatRule>()
                    .eq(FormatRule::getTemplateId, t.getId())));
            return m;
        }).collect(Collectors.toList());
    }

    /** 市场模板使用量: 该模板及其所有副本被用于排版任务的次数总和(实时统计) */
    public Map<Long, Long> marketUsageCounts(List<Long> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<FormatTemplate> copies = templateMapper.selectList(new LambdaQueryWrapper<FormatTemplate>()
                .in(FormatTemplate::getSourceTemplateId, publicIds));
        Map<Long, List<Long>> copiesBySource = copies.stream()
                .collect(Collectors.groupingBy(FormatTemplate::getSourceTemplateId,
                        Collectors.mapping(FormatTemplate::getId, Collectors.toList())));
        java.util.Set<Long> allIds = new java.util.HashSet<>(publicIds);
        copiesBySource.values().forEach(allIds::addAll);
        Map<Long, Long> taskCount = new HashMap<>();
        if (!allIds.isEmpty()) {
            List<FormatTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                    .in(FormatTask::getTemplateId, allIds)
                    .select(FormatTask::getTemplateId));
            for (FormatTask t : tasks) {
                if (t.getTemplateId() != null) {
                    taskCount.merge(t.getTemplateId(), 1L, Long::sum);
                }
            }
        }
        Map<Long, Long> usage = new HashMap<>();
        for (Long pid : publicIds) {
            long u = taskCount.getOrDefault(pid, 0L);
            for (Long cid : copiesBySource.getOrDefault(pid, Collections.emptyList())) {
                u += taskCount.getOrDefault(cid, 0L);
            }
            usage.put(pid, u);
        }
        return usage;
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
        copy.setGenerateToc(source.getGenerateToc());
        copy.setGenerateAbstract(source.getGenerateAbstract());
        copy.setReferenceConfig(source.getReferenceConfig());
        copy.setTocConfig(source.getTocConfig());
        copy.setCategory(source.getCategory());
        copy.setSourceTemplateId(publicTemplateId);
        copy.setCreateTime(LocalDateTime.now());
        copy.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(copy);

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

    // ==================== 评论 / 点赞 ====================

    /** 模板评论列表(按 id 正序), 含用户昵称 */
    public List<Map<String, Object>> listComments(Long templateId) {
        List<MarketComment> comments = commentMapper.selectList(new LambdaQueryWrapper<MarketComment>()
                .eq(MarketComment::getTemplateId, templateId)
                .orderByAsc(MarketComment::getId));
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = comments.stream().map(MarketComment::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<Map<String, Object>> out = new ArrayList<>();
        for (MarketComment c : comments) {
            User u = users.get(c.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("userId", c.getUserId());
            m.put("nickname", u == null ? "-" : (u.getNickname() == null ? u.getUsername() : u.getNickname()));
            m.put("username", u == null ? "-" : u.getUsername());
            m.put("content", c.getContent());
            m.put("parentId", c.getParentId());
            m.put("createTime", c.getCreateTime());
            out.add(m);
        }
        return out;
    }

    /** 发布评论(或回复) */
    @Transactional
    public MarketComment addComment(Long userId, Long templateId, String content, Long parentId) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException("评论最多 500 字");
        }
        FormatTemplate template = templateMapper.selectById(templateId);
        if (template == null || !Boolean.TRUE.equals(template.getIsPublic())) {
            throw new BusinessException(404, "模板不存在或未上架");
        }
        MarketComment c = new MarketComment();
        c.setTemplateId(templateId);
        c.setUserId(userId);
        c.setContent(content.trim());
        c.setParentId(parentId);
        c.setCreateTime(LocalDateTime.now());
        commentMapper.insert(c);
        return c;
    }

    /** 删除评论(本人或模板作者) */
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        MarketComment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BusinessException(404, "评论不存在");
        }
        if (c.getUserId().equals(userId)) {
            commentMapper.deleteById(commentId);
            return;
        }
        FormatTemplate template = templateMapper.selectById(c.getTemplateId());
        if (template != null && template.getUserId().equals(userId)) {
            commentMapper.deleteById(commentId);
            return;
        }
        throw new BusinessException(403, "无权删除该评论");
    }

    /** 点赞/取消点赞, 返回当前是否已赞与总赞数 */
    @Transactional
    public Map<String, Object> toggleLike(Long userId, Long templateId) {
        FormatTemplate template = templateMapper.selectById(templateId);
        if (template == null || !Boolean.TRUE.equals(template.getIsPublic())) {
            throw new BusinessException(404, "模板不存在或未上架");
        }
        MarketLike exist = likeMapper.selectOne(new LambdaQueryWrapper<MarketLike>()
                .eq(MarketLike::getTemplateId, templateId)
                .eq(MarketLike::getUserId, userId));
        if (exist == null) {
            MarketLike like = new MarketLike();
            like.setTemplateId(templateId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            likeMapper.insert(like);
            return likeResult(true, templateId);
        } else {
            likeMapper.deleteById(exist.getId());
            return likeResult(false, templateId);
        }
    }

    private Map<String, Object> likeResult(boolean liked, Long templateId) {
        Long count = likeMapper.selectCount(new LambdaQueryWrapper<MarketLike>()
                .eq(MarketLike::getTemplateId, templateId));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("liked", liked);
        m.put("likeCount", count == null ? 0 : count);
        return m;
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
        Map<Long, Long> usage = marketUsageCounts(ids);
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
            m.put("usageCount", usage.getOrDefault(t.getId(), 0L));
            m.put("ratingAvg", nvl2(t.getRatingAvg()).doubleValue());
            m.put("ratingCount", nvl(t.getRatingCount()));
            m.put("ruleCount", ruleMapper.selectCount(new LambdaQueryWrapper<FormatRule>()
                    .eq(FormatRule::getTemplateId, t.getId())));
            out.add(m);
        }
        return out;
    }

    // ==================== 克隆 / 校验 ====================

    /** 克隆模板: 复制本人/团队可见的模板(含全部配置与规则)为个人模板 */
    @Transactional
    public FormatTemplate clone(Long userId, Long templateId) {
        FormatTemplate src = getOwned(templateId, userId);
        FormatTemplate c = new FormatTemplate();
        c.setUserId(userId);
        c.setName(src.getName() + "（副本）");
        c.setTeamId(src.getTeamId());
        c.setPageConfig(src.getPageConfig());
        c.setHeadingPatterns(src.getHeadingPatterns());
        c.setGenerateToc(src.getGenerateToc());
        c.setGenerateAbstract(src.getGenerateAbstract());
        c.setReferenceConfig(src.getReferenceConfig());
        c.setTocConfig(src.getTocConfig());
        c.setCategory(src.getCategory());
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(c);
        List<FormatRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, templateId));
        for (FormatRule r : rules) {
            FormatRule nr = new FormatRule();
            nr.setTemplateId(c.getId());
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
        return c;
    }

    /** 模板规则完整性: 返回缺失的关键规则类型(用于前端提醒) */
    public List<String> missingRules(Long templateId, Long userId) {
        getOwned(templateId, userId);
        List<FormatRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, templateId));
        java.util.Set<String> has = rules.stream().map(FormatRule::getRuleType).collect(Collectors.toSet());
        List<String> missing = new ArrayList<>();
        String[] required = {"heading1", "heading2", "body"};
        for (String k : required) {
            if (!has.contains(k)) {
                missing.add(k);
            }
        }
        return missing;
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
        m.put("usageCount", marketUsageCounts(Collections.singletonList(templateId)).getOrDefault(templateId, 0L));
        m.put("ratingAvg", nvl2(t.getRatingAvg()).doubleValue());
        m.put("ratingCount", nvl(t.getRatingCount()));
        m.put("ruleCount", rules.size());
        m.put("generateToc", t.getGenerateToc());
        m.put("generateAbstract", t.getGenerateAbstract());
        m.put("pageConfig", t.getPageConfig());
        m.put("headingPatterns", t.getHeadingPatterns());
        m.put("referenceConfig", t.getReferenceConfig());
        m.put("tocConfig", t.getTocConfig());
        m.put("rules", rules);
        return m;
    }

}
