package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.dto.admin.AdminStatsVO;
import com.graduate.thesis.dto.admin.AdminTaskVO;
import com.graduate.thesis.dto.admin.AdminTemplateVO;
import com.graduate.thesis.dto.admin.AdminUserVO;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.FormatTemplate;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.mapper.FormatRuleMapper;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.mapper.FormatTemplateMapper;
import com.graduate.thesis.mapper.PaperFileMapper;
import com.graduate.thesis.mapper.UserMapper;
import com.graduate.thesis.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理后台服务
 */
@Service
public class AdminService {

    private final UserMapper userMapper;
    private final FormatTemplateMapper templateMapper;
    private final FormatRuleMapper ruleMapper;
    private final FormatTaskMapper taskMapper;
    private final PaperFileMapper paperFileMapper;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AdminService(UserMapper userMapper,
                        FormatTemplateMapper templateMapper,
                        FormatRuleMapper ruleMapper,
                        FormatTaskMapper taskMapper,
                        PaperFileMapper paperFileMapper,
                        UserService userService,
                        JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.templateMapper = templateMapper;
        this.ruleMapper = ruleMapper;
        this.taskMapper = taskMapper;
        this.paperFileMapper = paperFileMapper;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    // ==================== 概览统计 ====================

    public AdminStatsVO overview() {
        AdminStatsVO vo = new AdminStatsVO();
        vo.setUserCount(safeCount(userMapper.selectCount(null)));
        vo.setTemplateCount(safeCount(templateMapper.selectCount(null)));
        vo.setTaskCount(safeCount(taskMapper.selectCount(null)));
        vo.setPaperCount(safeCount(paperFileMapper.selectCount(null)));
        vo.setAdminCount(safeCount(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, User.ROLE_ADMIN))));

        Map<String, Long> status = new LinkedHashMap<>();
        for (String s : new String[]{
                FormatTask.STATUS_PENDING, FormatTask.STATUS_PROCESSING,
                FormatTask.STATUS_SUCCESS, FormatTask.STATUS_FAILED}) {
            status.put(s, safeCount(taskMapper.selectCount(new LambdaQueryWrapper<FormatTask>()
                    .eq(FormatTask::getStatus, s))));
        }
        vo.setTaskStatus(status);

        vo.setRegisterTrend(buildTrend(7, userMapper.selectList(new LambdaQueryWrapper<User>()
                        .ge(User::getCreateTime, LocalDate.now().minusDays(6).atStartOfDay())
                        .select(User::getCreateTime)),
                User::getCreateTime));
        vo.setTaskTrend(buildTrend(7, taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                        .ge(FormatTask::getCreateTime, LocalDate.now().minusDays(6).atStartOfDay())
                        .select(FormatTask::getCreateTime)),
                FormatTask::getCreateTime));
        return vo;
    }

    private long safeCount(Long c) {
        return c == null ? 0 : c;
    }

    private <T> List<AdminStatsVO.TrendPoint> buildTrend(int days, List<T> rows, Function<T, LocalDateTime> timeFn) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        Map<LocalDate, Long> count = new HashMap<>();
        for (T row : rows) {
            LocalDateTime t = timeFn.apply(row);
            if (t != null) {
                count.merge(t.toLocalDate(), 1L, Long::sum);
            }
        }
        List<AdminStatsVO.TrendPoint> points = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            points.add(new AdminStatsVO.TrendPoint(date.format(fmt),
                    count.getOrDefault(date, 0L)));
        }
        return points;
    }

    // ==================== 用户管理 ====================

    public PageResult<AdminUserVO> listUsers(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getUsername, kw).or().like(User::getEmail, kw).or().like(User::getNickname, kw));
        }
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), toUserVOs(page.getRecords()));
    }

    private List<AdminUserVO> toUserVOs(List<User> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = users.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, Long> templateCnt = countByKey(templateMapper.selectList(new LambdaQueryWrapper<FormatTemplate>()
                        .in(FormatTemplate::getUserId, ids).select(FormatTemplate::getUserId)),
                FormatTemplate::getUserId);
        Map<Long, Long> taskCnt = countByKey(taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                        .in(FormatTask::getUserId, ids).select(FormatTask::getUserId)),
                FormatTask::getUserId);
        Map<Long, Long> paperCnt = countByKey(paperFileMapper.selectList(new LambdaQueryWrapper<PaperFile>()
                        .in(PaperFile::getUserId, ids).select(PaperFile::getUserId)),
                PaperFile::getUserId);

        List<AdminUserVO> result = new ArrayList<>();
        for (User u : users) {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setEmail(u.getEmail());
            vo.setNickname(u.getNickname());
            vo.setRole(u.getRole() == null ? User.ROLE_USER : u.getRole());
            vo.setCreateTime(u.getCreateTime());
            vo.setTemplateCount(templateCnt.getOrDefault(u.getId(), 0L));
            vo.setTaskCount(taskCnt.getOrDefault(u.getId(), 0L));
            vo.setPaperCount(paperCnt.getOrDefault(u.getId(), 0L));
            result.add(vo);
        }
        return result;
    }

    /** 修改用户角色; operatorId 为操作者(禁止操作自己) */
    @Transactional
    public void updateUserRole(Long userId, String role, Long operatorId) {
        if (!User.ROLE_ADMIN.equals(role) && !User.ROLE_USER.equals(role)) {
            throw new BusinessException(400, "非法角色");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (userId.equals(operatorId)) {
            throw new BusinessException(400, "不能修改自己的角色");
        }
        if (User.ROLE_USER.equals(role) && User.ROLE_ADMIN.equals(user.getRole())) {
            long adminCount = safeCount(userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, User.ROLE_ADMIN)));
            if (adminCount <= 1) {
                throw new BusinessException(400, "至少保留一名管理员");
            }
        }
        user.setRole(role);
        userMapper.updateById(user);
        // 降级后使其旧 token 全部失效, 需重新登录
        if (User.ROLE_USER.equals(role)) {
            jwtUtil.revokeAllForUser(userId);
        }
    }

    @Transactional
    public void deleteUser(Long userId, Long operatorId) {
        if (userId.equals(operatorId)) {
            throw new BusinessException(400, "不能删除当前登录账号");
        }
        userService.deleteAccount(userId);
    }

    // ==================== 模板管理 ====================

    public PageResult<AdminTemplateVO> listTemplates(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<FormatTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(FormatTemplate::getName, keyword.trim());
        }
        wrapper.orderByDesc(FormatTemplate::getUpdateTime);
        IPage<FormatTemplate> page = templateMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), toTemplateVOs(page.getRecords()));
    }

    private List<AdminTemplateVO> toTemplateVOs(List<FormatTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = templates.stream().map(FormatTemplate::getId).collect(Collectors.toList());
        List<Long> userIds = templates.stream().map(FormatTemplate::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> usernameMap = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, userIds))
                .stream().collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        Map<Long, Long> ruleCnt = countByKey(ruleMapper.selectList(new LambdaQueryWrapper<FormatRule>()
                        .in(FormatRule::getTemplateId, ids).select(FormatRule::getTemplateId)),
                FormatRule::getTemplateId);
        Map<Long, Long> taskCnt = countByKey(taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                        .in(FormatTask::getTemplateId, ids).select(FormatTask::getTemplateId)),
                FormatTask::getTemplateId);

        List<AdminTemplateVO> result = new ArrayList<>();
        for (FormatTemplate t : templates) {
            AdminTemplateVO vo = new AdminTemplateVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setUserId(t.getUserId());
            vo.setUsername(usernameMap.getOrDefault(t.getUserId(), "-"));
            vo.setGenerateToc(t.getGenerateToc());
            vo.setCreateTime(t.getCreateTime());
            vo.setUpdateTime(t.getUpdateTime());
            vo.setRuleCount(ruleCnt.getOrDefault(t.getId(), 0L));
            vo.setTaskCount(taskCnt.getOrDefault(t.getId(), 0L));
            result.add(vo);
        }
        return result;
    }

    private <T> Map<Long, Long> countByKey(List<T> rows, Function<T, Long> keyFn) {
        Map<Long, Long> map = new HashMap<>();
        for (T row : rows) {
            Long k = keyFn.apply(row);
            if (k != null) {
                map.merge(k, 1L, Long::sum);
            }
        }
        return map;
    }

    @Transactional
    public void deleteTemplate(Long id) {
        FormatTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(404, "模板不存在");
        }
        ruleMapper.delete(new LambdaQueryWrapper<FormatRule>().eq(FormatRule::getTemplateId, id));
        templateMapper.deleteById(id);
    }

    // ==================== 任务管理 ====================

    public PageResult<AdminTaskVO> listTasks(int pageNum, int pageSize, String status, String keyword) {
        LambdaQueryWrapper<FormatTask> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(FormatTask::getStatus, status.trim().toUpperCase());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getUsername, kw).select(User::getId))
                    .stream().map(User::getId).collect(Collectors.toList());
            List<Long> fileIds = paperFileMapper.selectList(new LambdaQueryWrapper<PaperFile>()
                            .like(PaperFile::getOriginalName, kw).select(PaperFile::getId))
                    .stream().map(PaperFile::getId).collect(Collectors.toList());
            List<Long> templateIds = templateMapper.selectList(new LambdaQueryWrapper<FormatTemplate>()
                            .like(FormatTemplate::getName, kw).select(FormatTemplate::getId))
                    .stream().map(FormatTemplate::getId).collect(Collectors.toList());
            if (userIds.isEmpty() && fileIds.isEmpty() && templateIds.isEmpty()) {
                return PageResult.of(0, Collections.emptyList());
            }
            wrapper.and(w -> {
                boolean first = true;
                if (!userIds.isEmpty()) {
                    w.in(FormatTask::getUserId, userIds);
                    first = false;
                }
                if (!fileIds.isEmpty()) {
                    if (first) w.in(FormatTask::getFileId, fileIds);
                    else w.or().in(FormatTask::getFileId, fileIds);
                    first = false;
                }
                if (!templateIds.isEmpty()) {
                    if (first) w.in(FormatTask::getTemplateId, templateIds);
                    else w.or().in(FormatTask::getTemplateId, templateIds);
                }
            });
        }
        wrapper.orderByDesc(FormatTask::getCreateTime);
        IPage<FormatTask> page = taskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), toTaskVOs(page.getRecords()));
    }

    private List<AdminTaskVO> toTaskVOs(List<FormatTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = tasks.stream().map(FormatTask::getUserId).distinct().collect(Collectors.toList());
        List<Long> fileIds = tasks.stream().map(FormatTask::getFileId).distinct().collect(Collectors.toList());
        List<Long> templateIds = tasks.stream().map(FormatTask::getTemplateId).distinct().collect(Collectors.toList());
        Map<Long, String> usernameMap = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, userIds))
                .stream().collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        Map<Long, String> fileNameMap = paperFileMapper.selectList(new LambdaQueryWrapper<PaperFile>()
                        .in(PaperFile::getId, fileIds))
                .stream().collect(Collectors.toMap(PaperFile::getId, PaperFile::getOriginalName, (a, b) -> a));
        Map<Long, String> templateNameMap = templateMapper.selectList(new LambdaQueryWrapper<FormatTemplate>()
                        .in(FormatTemplate::getId, templateIds))
                .stream().collect(Collectors.toMap(FormatTemplate::getId, FormatTemplate::getName, (a, b) -> a));

        List<AdminTaskVO> result = new ArrayList<>();
        for (FormatTask t : tasks) {
            AdminTaskVO vo = new AdminTaskVO();
            vo.setId(t.getId());
            vo.setUserId(t.getUserId());
            vo.setUsername(usernameMap.getOrDefault(t.getUserId(), "-"));
            vo.setTemplateId(t.getTemplateId());
            vo.setTemplateName(templateNameMap.getOrDefault(t.getTemplateId(), "-"));
            vo.setOriginalName(fileNameMap.getOrDefault(t.getFileId(), "-"));
            vo.setStatus(t.getStatus());
            vo.setProgress(t.getProgress());
            vo.setErrorMsg(t.getErrorMsg());
            vo.setCreateTime(t.getCreateTime());
            vo.setFinishTime(t.getFinishTime());
            result.add(vo);
        }
        return result;
    }
}
