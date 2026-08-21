package com.graduate.thesis.controller;

import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.FeedbackCreateDTO;
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

/**
 * 用户反馈接口(登录即可): 提交(可带图片) / 公开墙 / 详情。
 * 普通用户之间不可回复; 管理员回复走后台接口。
 */
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /** 提交反馈(可带图片) */
    @PostMapping
    public Result<FeedbackVO> create(@RequestBody FeedbackCreateDTO dto) {
        return Result.ok(feedbackService.create(UserContext.get(),
                dto.getCategory(), dto.getContent(), dto.getContact(), dto.getImages()));
    }

    /** 公开反馈墙(分页, 可按分类筛选) */
    @GetMapping("/list")
    public Result<PageResult<FeedbackVO>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String category) {
        return Result.ok(feedbackService.listPublic(Math.max(page, 1), Math.min(Math.max(size, 1), 50), category));
    }

    /** 反馈详情(联系方式不对外公开) */
    @GetMapping("/{id}")
    public Result<FeedbackVO> detail(@PathVariable Long id) {
        return Result.ok(feedbackService.getDetail(id, true));
    }

    /** 用户删除自己的反馈 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        feedbackService.deleteByUser(UserContext.get(), id);
        return Result.ok();
    }
}
