package com.graduate.thesis.controller;

import com.graduate.thesis.annotation.OperLog;
import com.graduate.thesis.annotation.RequiresPerms;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.FeedbackVO;
import com.graduate.thesis.service.FeedbackService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户反馈管理后台接口(AdminInterceptor + @RequiresPerms)。
 */
@RestController
@RequestMapping("/admin/feedback")
public class FeedbackAdminController {

    private final FeedbackService feedbackService;

    public FeedbackAdminController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /** 后台分页列表(可选状态/关键字筛选) */
    @GetMapping("/list")
    @RequiresPerms("system:feedback:list")
    public Result<PageResult<FeedbackVO>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String keyword) {
        return Result.ok(feedbackService.listForAdmin(Math.max(page, 1), Math.min(Math.max(size, 1), 100),
                status, keyword));
    }

    /** 后台详情(含联系方式) */
    @GetMapping("/{id}")
    @RequiresPerms("system:feedback:list")
    public Result<FeedbackVO> detail(@PathVariable Long id) {
        return Result.ok(feedbackService.getDetail(id, false));
    }

    /** 回复反馈 */
    @PostMapping("/{id}/reply")
    @OperLog(module = "反馈管理", action = "回复反馈")
    @RequiresPerms("system:feedback:reply")
    public Result<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        feedbackService.reply(id, body.get("reply"), UserContext.get());
        return Result.ok();
    }

    /** 修改状态(关闭/重开) */
    @PostMapping("/{id}/status")
    @OperLog(module = "反馈管理", action = "修改反馈状态")
    @RequiresPerms("system:feedback:reply")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        feedbackService.updateStatus(id, body.get("status"));
        return Result.ok();
    }

    /** 删除反馈 */
    @DeleteMapping("/{id}")
    @OperLog(module = "反馈管理", action = "删除反馈")
    @RequiresPerms("system:feedback:delete")
    public Result<Void> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return Result.ok();
    }
}
