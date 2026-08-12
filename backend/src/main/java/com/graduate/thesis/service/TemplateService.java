package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.RuleSaveDTO;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTemplate;
import com.graduate.thesis.mapper.FormatRuleMapper;
import com.graduate.thesis.mapper.FormatTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    public TemplateService(FormatTemplateMapper templateMapper, FormatRuleMapper ruleMapper) {
        this.templateMapper = templateMapper;
        this.ruleMapper = ruleMapper;
    }

    public FormatTemplate create(Long userId, String name) {
        FormatTemplate template = new FormatTemplate();
        template.setUserId(userId);
        template.setName(name);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(template);
        return template;
    }

    public List<FormatTemplate> listByUser(Long userId) {
        return templateMapper.selectList(new LambdaQueryWrapper<FormatTemplate>()
                .eq(FormatTemplate::getUserId, userId)
                .orderByDesc(FormatTemplate::getId));
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
        if (template == null || !template.getUserId().equals(userId)) {
            throw new BusinessException(404, "模板不存在");
        }
        return template;
    }

    public Map<String, FormatRule> ruleMap(Long templateId) {
        return listRules(templateId).stream()
                .collect(Collectors.toMap(FormatRule::getRuleType, r -> r, (a, b) -> a));
    }
}
