package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.GenerateTocSaveDTO;
import com.graduate.thesis.dto.HeadingPatternsSaveDTO;
import com.graduate.thesis.dto.PageConfigSaveDTO;
import com.graduate.thesis.dto.ReferenceConfigSaveDTO;
import com.graduate.thesis.dto.RuleSaveDTO;
import com.graduate.thesis.dto.TemplateCreateDTO;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTemplate;
import com.graduate.thesis.service.TemplateService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 格式模板接口
 */
@RestController
@RequestMapping("/template")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping("/create")
    public Result<FormatTemplate> create(@Valid @RequestBody TemplateCreateDTO dto) {
        return Result.ok(templateService.create(UserContext.get(), dto.getName(), dto.getTeamId()));
    }

    /** 模板市场公开模板(无需登录), 支持分类筛选与排序 */
    @GetMapping("/market/list")
    public Result<List<Map<String, Object>>> marketList(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sort", required = false) String sort) {
        return Result.ok(templateService.listPublicTemplates(category, sort));
    }

    /** 模板市场分类列表(无需登录) */
    @GetMapping("/market/categories")
    public Result<List<String>> marketCategories() {
        return Result.ok(templateService.listMarketCategories());
    }

    /** 模板市场详情(无需登录) */
    @GetMapping("/market/{id}/detail")
    public Result<Map<String, Object>> marketDetail(@PathVariable Long id) {
        return Result.ok(templateService.marketDetail(id));
    }

    /** 收藏/取消收藏市场模板(登录) */
    @PostMapping("/market/{id}/favorite")
    public Result<Map<String, Object>> toggleFavorite(@PathVariable Long id) {
        boolean favorited = templateService.toggleFavorite(UserContext.get(), id);
        return Result.ok(java.util.Map.of("favorited", favorited));
    }

    /** 我的收藏(登录) */
    @GetMapping("/market/favorites")
    public Result<List<Map<String, Object>>> myFavorites() {
        return Result.ok(templateService.listFavorites(UserContext.get()));
    }

    /** 复制公开模板到我的模板 */
    @PostMapping("/market/{id}/copy")
    public Result<FormatTemplate> copyMarket(@PathVariable Long id) {
        return Result.ok(templateService.copyPublic(UserContext.get(), id));
    }

    /** 市场模板评分(1~5) */
    @PostMapping("/market/{id}/rate")
    public Result<Void> rateMarket(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object s = body.get("score");
        int score = s == null ? 0 : Integer.parseInt(String.valueOf(s));
        templateService.ratePublic(UserContext.get(), id, score);
        return Result.ok();
    }

    /** 模板评论列表(公开) */
    @GetMapping("/market/{id}/comments")
    public Result<List<Map<String, Object>>> listComments(@PathVariable Long id) {
        return Result.ok(templateService.listComments(id));
    }

    /** 发布评论或回复(登录) */
    @PostMapping("/market/{id}/comment")
    public Result<com.graduate.thesis.entity.MarketComment> addComment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object p = body.get("parentId");
        Long parentId = p == null ? null : Long.valueOf(String.valueOf(p));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        return Result.ok(templateService.addComment(UserContext.get(), id, content, parentId));
    }

    /** 删除评论(本人或模板作者) */
    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        templateService.deleteComment(UserContext.get(), commentId);
        return Result.ok();
    }

    /** 点赞/取消点赞(登录) */
    @PostMapping("/market/{id}/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long id) {
        return Result.ok(templateService.toggleLike(UserContext.get(), id));
    }

    @GetMapping("/list")
    public Result<List<FormatTemplate>> list() {
        return Result.ok(templateService.listByUser(UserContext.get()));
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(templateService.detail(id, UserContext.get()));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id, UserContext.get());
        return Result.ok();
    }

    /** 克隆模板(复制配置与规则) */
    @PostMapping("/{id}/clone")
    public Result<FormatTemplate> cloneTemplate(@PathVariable Long id) {
        return Result.ok(templateService.clone(UserContext.get(), id));
    }

    /** 模板规则完整性检查: 返回缺失的关键规则类型 */
    @GetMapping("/{id}/missing-rules")
    public Result<List<String>> missingRules(@PathVariable Long id) {
        return Result.ok(templateService.missingRules(id, UserContext.get()));
    }

    @PutMapping("/{id}/page-config")
    public Result<Void> savePageConfig(@PathVariable Long id, @Valid @RequestBody PageConfigSaveDTO dto) {
        templateService.savePageConfig(id, UserContext.get(), dto.getPageConfig());
        return Result.ok();
    }

    @PutMapping("/{id}/heading-patterns")
    public Result<Void> saveHeadingPatterns(@PathVariable Long id, @Valid @RequestBody HeadingPatternsSaveDTO dto) {
        templateService.saveHeadingPatterns(id, UserContext.get(),
                "{\"heading1\":\"" + escapeJson(dto.getHeading1())
                        + "\",\"heading2\":\"" + escapeJson(dto.getHeading2())
                        + "\",\"heading3\":\"" + escapeJson(dto.getHeading3()) + "\"}");
        return Result.ok();
    }

    @PutMapping("/{id}/generate-toc")
    public Result<Void> saveGenerateToc(@PathVariable Long id, @RequestBody GenerateTocSaveDTO dto) {
        templateService.saveGenerateToc(id, UserContext.get(), dto.getGenerateToc());
        return Result.ok();
    }

    @PutMapping("/{id}/reference-config")
    public Result<Void> saveReferenceConfig(@PathVariable Long id, @RequestBody ReferenceConfigSaveDTO dto) {
        templateService.saveReferenceConfig(id, UserContext.get(), dto.getReferenceConfig());
        return Result.ok();
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @PostMapping("/rule/save")
    public Result<FormatRule> saveRule(@Valid @RequestBody RuleSaveDTO dto) {
        return Result.ok(templateService.saveRule(UserContext.get(), dto));
    }

    @GetMapping("/{id}/rules")
    public Result<List<FormatRule>> rules(@PathVariable Long id) {
        templateService.getOwned(id, UserContext.get());
        return Result.ok(templateService.listRules(id));
    }
}
