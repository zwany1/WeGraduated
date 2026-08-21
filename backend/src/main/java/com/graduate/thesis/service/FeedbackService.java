package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.dto.FeedbackVO;
import com.graduate.thesis.entity.Feedback;
import com.graduate.thesis.entity.Notification;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.mapper.FeedbackMapper;
import com.graduate.thesis.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户反馈服务: 提交(含图片) / 公开墙 / 详情 / 管理员回复(回复后发站内信) / 状态管理。
 * 公开墙与前台详情不暴露 contact(联系方式仅管理员可见)。
 * 图片以 base64 data URL 的 JSON 数组形式直存 t_feedback.images(仿 t_user.avatar 方案)。
 */
@Service
public class FeedbackService {

    private static final int MAX_CONTENT_LEN = 2000;
    private static final int MAX_CONTACT_LEN = 128;
    private static final int MAX_IMAGES = 6;
    /** 单张图片 base64 字符串长度上限(约 2MB 原图) */
    private static final int MAX_IMAGE_BASE64_LEN = 3 * 1024 * 1024;
    /** 用户级提交频率: 60 秒间隔 + 每日上限, 防止刷反馈撑爆库 */
    private static final long USER_SUBMIT_INTERVAL = 60 * 1000L;
    private static final int USER_DAILY_LIMIT = 10;
    private static final long DAY_MILLIS = 24 * 60 * 60 * 1000L;
    /** userId -> [上次提交ms, 当日起点ms, 当日已提交次数] */
    private final ConcurrentHashMap<Long, long[]> userSubmitWindow = new ConcurrentHashMap<>();

    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public FeedbackService(FeedbackMapper feedbackMapper, UserMapper userMapper,
                           NotificationService notificationService) {
        this.feedbackMapper = feedbackMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    /** 提交反馈(可带图片) */
    public FeedbackVO create(Long userId, String category, String content, String contact, List<String> images) {
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        if (!acquireUserSlot(userId)) {
            throw new BusinessException(429, "提交过于频繁，请稍后再试");
        }
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BusinessException(400, "反馈内容不能为空");
        }
        if (text.length() > MAX_CONTENT_LEN) {
            throw new BusinessException(400, "反馈内容过长");
        }
        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setCategory(normalizeCategory(category));
        fb.setContent(text);
        if (contact != null) {
            String c = contact.trim();
            if (c.length() > MAX_CONTACT_LEN) {
                throw new BusinessException(400, "联系方式过长");
            }
            fb.setContact(c.isEmpty() ? null : c);
        }
        fb.setImages(toJsonImages(sanitizeImages(images)));
        fb.setStatus(Feedback.STATUS_PENDING);
        fb.setCreateTime(LocalDateTime.now());
        feedbackMapper.insert(fb);
        return assembleOne(fb, false);
    }

    /** 公开反馈墙(只展示已回复, 按时间倒序; 不含联系方式) */
    public PageResult<FeedbackVO> listPublic(int pageNum, int pageSize, String category) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getStatus, Feedback.STATUS_REPLIED)
                .orderByDesc(Feedback::getId);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Feedback::getCategory, normalizeCategory(category));
        }
        IPage<Feedback> page = feedbackMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<FeedbackVO> vos = assemble(page.getRecords(), false);
        vos.forEach(v -> v.setContact(null));
        return PageResult.of(page.getTotal(), vos);
    }

    /** 反馈详情; publicView=true 时清空联系方式 */
    public FeedbackVO getDetail(Long id, boolean publicView) {
        FeedbackVO vo = assembleOne(requireFeedback(id), false);
        if (publicView && vo != null) {
            vo.setContact(null);
        }
        return vo;
    }

    /** 后台分页列表(可选状态/关键字; 含联系方式) */
    public PageResult<FeedbackVO> listForAdmin(int pageNum, int pageSize, String status, String keyword) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Feedback::getStatus, status.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            Set<Long> matchedUserIds = findUserIdsByKeyword(kw);
            if (matchedUserIds.isEmpty()) {
                wrapper.and(w -> w.like(Feedback::getContent, kw));
            } else {
                wrapper.and(w -> w.like(Feedback::getContent, kw)
                        .or().in(Feedback::getUserId, matchedUserIds));
            }
        }
        wrapper.orderByDesc(Feedback::getId);
        IPage<Feedback> page = feedbackMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), assemble(page.getRecords(), true));
    }

    /** 管理员回复(状态置为已回复, 并发站内信通知反馈提交者) */
    public void reply(Long id, String reply, Long adminId) {
        Feedback fb = requireFeedback(id);
        String text = reply == null ? "" : reply.trim();
        if (text.isEmpty()) {
            throw new BusinessException(400, "回复内容不能为空");
        }
        fb.setReply(text);
        fb.setReplyUserId(adminId);
        fb.setReplyTime(LocalDateTime.now());
        fb.setStatus(Feedback.STATUS_REPLIED);
        feedbackMapper.updateById(fb);
        // 站内信通知反馈提交者; 通知为附加行为, 失败不阻断回复主流程
        try {
            notificationService.send(fb.getUserId(), Notification.TYPE_FEEDBACK_REPLY,
                    "反馈回复通知",
                    "管理员已回复你的反馈，请查看详情。",
                    Map.of("feedbackId", fb.getId()));
        } catch (Exception ignored) {
            // 通知发送失败不影响已成功的回复
        }
    }

    /** 修改状态(管理员手动关闭/重开/标记) */
    public void updateStatus(Long id, String status) {
        Feedback fb = requireFeedback(id);
        String s = status == null ? "" : status.trim();
        if (!Feedback.STATUS_PENDING.equals(s)
                && !Feedback.STATUS_REPLIED.equals(s)
                && !Feedback.STATUS_CLOSED.equals(s)) {
            throw new BusinessException(400, "非法状态");
        }
        fb.setStatus(s);
        feedbackMapper.updateById(fb);
    }

    /** 删除反馈 */
    public void delete(Long id) {
        requireFeedback(id);
        feedbackMapper.deleteById(id);
    }

    /** 用户删除自己的反馈(校验归属) */
    public void deleteByUser(Long userId, Long id) {
        Feedback fb = requireFeedback(id);
        if (!fb.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己的反馈");
        }
        feedbackMapper.deleteById(id);
    }

    // ==================== 内部工具 ====================

    /** 用户级提交频率限制: 60 秒间隔 + 每日 USER_DAILY_LIMIT 条, 超限拒绝 */
    private boolean acquireUserSlot(Long userId) {
        long now = System.currentTimeMillis();
        boolean[] allowed = {false};
        userSubmitWindow.compute(userId, (k, v) -> {
            if (v == null) {
                allowed[0] = true;
                return new long[]{now, now, 1};
            }
            if (now - v[0] < USER_SUBMIT_INTERVAL) {
                allowed[0] = false;
                return v;
            }
            long dayStart = v[1];
            long dayCount = v[2];
            if (now - dayStart >= DAY_MILLIS) {
                dayStart = now;
                dayCount = 0;
            }
            if (dayCount >= USER_DAILY_LIMIT) {
                allowed[0] = false;
                return new long[]{v[0], dayStart, dayCount};
            }
            allowed[0] = true;
            return new long[]{now, dayStart, dayCount + 1};
        });
        return allowed[0];
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return Feedback.CATEGORY_OTHER;
        }
        String c = category.trim();
        if (Feedback.CATEGORY_SUGGESTION.equals(c) || Feedback.CATEGORY_BUG.equals(c)) {
            return c;
        }
        return Feedback.CATEGORY_OTHER;
    }

    /** 校验并清洗图片列表: 限数量、须为 data:image、限单张大小 */
    private List<String> sanitizeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        if (images.size() > MAX_IMAGES) {
            throw new BusinessException(400, "最多上传" + MAX_IMAGES + "张图片");
        }
        List<String> result = new ArrayList<>();
        for (String img : images) {
            if (img == null) {
                continue;
            }
            String s = img.trim();
            if (s.isEmpty()) {
                continue;
            }
            if (!s.startsWith("data:image")) {
                throw new BusinessException(400, "图片格式不合法");
            }
            if (s.length() > MAX_IMAGE_BASE64_LEN) {
                throw new BusinessException(400, "图片过大，请压缩后上传");
            }
            result.add(s);
        }
        return result;
    }

    private Feedback requireFeedback(Long id) {
        if (id == null) {
            throw new BusinessException(404, "反馈不存在");
        }
        Feedback fb = feedbackMapper.selectById(id);
        if (fb == null) {
            throw new BusinessException(404, "反馈不存在");
        }
        return fb;
    }

    private Set<Long> findUserIdsByKeyword(String kw) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .like(User::getUsername, kw).or().like(User::getNickname, kw)
                .last("LIMIT 100"));
        return users.stream().map(User::getId).collect(Collectors.toSet());
    }

    /** 批量组装 VO: 一次查全所有相关用户(提交者 + 回复者) */
    private List<FeedbackVO> assemble(List<Feedback> list, boolean withContact) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, User> userMap = loadUsers(list);
        List<FeedbackVO> result = new ArrayList<>(list.size());
        for (Feedback fb : list) {
            result.add(toVO(fb, userMap, withContact));
        }
        return result;
    }

    private FeedbackVO assembleOne(Feedback fb, boolean withContact) {
        if (fb == null) {
            return null;
        }
        Map<Long, User> userMap = loadUsers(Collections.singletonList(fb));
        return toVO(fb, userMap, withContact);
    }

    private Map<Long, User> loadUsers(List<Feedback> list) {
        Set<Long> ids = new HashSet<>();
        for (Feedback fb : list) {
            if (fb.getUserId() != null) {
                ids.add(fb.getUserId());
            }
            if (fb.getReplyUserId() != null) {
                ids.add(fb.getReplyUserId());
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(ids);
        return users.stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private FeedbackVO toVO(Feedback fb, Map<Long, User> userMap, boolean withContact) {
        FeedbackVO vo = new FeedbackVO();
        vo.setId(fb.getId());
        vo.setUserId(fb.getUserId());
        vo.setCategory(fb.getCategory());
        vo.setContent(fb.getContent());
        vo.setImages(parseImages(fb.getImages()));
        vo.setContact(withContact ? fb.getContact() : null);
        vo.setStatus(fb.getStatus());
        vo.setReply(fb.getReply());
        vo.setReplyUserId(fb.getReplyUserId());
        vo.setReplyTime(fb.getReplyTime());
        vo.setCreateTime(fb.getCreateTime());
        User author = fb.getUserId() == null ? null : userMap.get(fb.getUserId());
        if (author != null) {
            vo.setUsername(author.getUsername());
            vo.setNickname(author.getNickname());
            vo.setAvatar(author.getAvatar());
        }
        User replier = fb.getReplyUserId() == null ? null : userMap.get(fb.getReplyUserId());
        if (replier != null) {
            vo.setReplyUsername(replier.getUsername());
        }
        return vo;
    }

    private static String toJsonImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(images);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> parseImages(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                    });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
