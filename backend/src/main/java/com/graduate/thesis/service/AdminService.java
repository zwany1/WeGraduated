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
import com.graduate.thesis.dto.admin.UserDetailVO;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.FormatTemplate;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.entity.Role;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.entity.UserRole;
import com.graduate.thesis.mapper.FormatRuleMapper;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.mapper.FormatTemplateMapper;
import com.graduate.thesis.mapper.PaperFileMapper;
import com.graduate.thesis.mapper.RoleMapper;
import com.graduate.thesis.mapper.UserMapper;
import com.graduate.thesis.mapper.UserRoleMapper;
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
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionService permissionService;
    private final DbRetryService dbRetryService;

    public AdminService(UserMapper userMapper,
                        FormatTemplateMapper templateMapper,
                        FormatRuleMapper ruleMapper,
                        FormatTaskMapper taskMapper,
                        PaperFileMapper paperFileMapper,
                        UserService userService,
                        JwtUtil jwtUtil,
                        RoleMapper roleMapper,
                        UserRoleMapper userRoleMapper,
                        PermissionService permissionService,
                        DbRetryService dbRetryService) {
        this.userMapper = userMapper;
        this.templateMapper = templateMapper;
        this.ruleMapper = ruleMapper;
        this.taskMapper = taskMapper;
        this.paperFileMapper = paperFileMapper;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.permissionService = permissionService;
        this.dbRetryService = dbRetryService;
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
        Map<Long, List<Long>> userRoleMap = buildUserRoleMap(ids);

        List<AdminUserVO> result = new ArrayList<>();
        for (User u : users) {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setEmail(u.getEmail());
            vo.setNickname(u.getNickname());
            vo.setRole(u.getRole() == null ? User.ROLE_USER : u.getRole());
            vo.setStatus(u.getStatus() == null ? Boolean.TRUE : u.getStatus());
            List<Long> roleIds = userRoleMap.getOrDefault(u.getId(), Collections.emptyList());
            vo.setRoleIds(roleIds);
            vo.setRoleNames(roleNamesOf(roleIds));
            vo.setCreateTime(u.getCreateTime());
            vo.setTemplateCount(templateCnt.getOrDefault(u.getId(), 0L));
            vo.setTaskCount(taskCnt.getOrDefault(u.getId(), 0L));
            vo.setPaperCount(paperCnt.getOrDefault(u.getId(), 0L));
            result.add(vo);
        }
        return result;
    }

    private Map<Long, List<Long>> buildUserRoleMap(List<Long> userIds) {
        Map<Long, List<Long>> map = new HashMap<>();
        List<UserRole> rows = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .in(UserRole::getUserId, userIds));
        for (UserRole ur : rows) {
            map.computeIfAbsent(ur.getUserId(), k -> new ArrayList<>()).add(ur.getRoleId());
        }
        return map;
    }

    private List<String> roleNamesOf(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream().map(r -> r.getRoleName() == null ? r.getRoleKey() : r.getRoleName())
                .collect(Collectors.toList());
    }

    /** 修改用户主角色(兼容旧接口, 同步 RBAC 角色分配) */
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
        if (User.ROLE_USER.equals(role) && permissionService.isAdmin(userId)) {
            long adminCount = adminUserCount();
            if (adminCount <= 1) {
                throw new BusinessException(400, "至少保留一名管理员");
            }
        }
        user.setRole(role);
        userMapper.updateById(user);
        // 同步 RBAC 角色: 设为管理员 -> 授予 admin 角色; 取消管理员 -> 移除 admin 角色
        Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleKey, Role.KEY_ADMIN).last("LIMIT 1"));
        Role userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleKey, Role.KEY_USER).last("LIMIT 1"));
        if (adminRole != null) {
            boolean hasAdmin = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, userId).eq(UserRole::getRoleId, adminRole.getId())) > 0;
            if (User.ROLE_ADMIN.equals(role) && !hasAdmin) {
                userRoleMapper.insert(new UserRole(userId, adminRole.getId()));
            } else if (User.ROLE_USER.equals(role) && hasAdmin) {
                userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId).eq(UserRole::getRoleId, adminRole.getId()));
            }
        }
        if (userRole != null && User.ROLE_USER.equals(role)) {
            boolean hasUser = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, userId).eq(UserRole::getRoleId, userRole.getId())) > 0;
            if (!hasUser) {
                userRoleMapper.insert(new UserRole(userId, userRole.getId()));
            }
        }
        // 权限变化后使其旧 token 全部失效, 需重新登录
        jwtUtil.revokeAllForUser(userId);
    }

    /** 给用户分配角色(全量覆盖) */
    @Transactional
    public void assignUserRoles(Long userId, List<Long> roleIds, Long operatorId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (userId.equals(operatorId)) {
            throw new BusinessException(400, "不能修改当前登录账号的角色");
        }
        List<Long> target = roleIds == null ? Collections.emptyList() : roleIds.stream().distinct().collect(Collectors.toList());
        // 防止将最后一个管理员降级
        boolean targetHasAdmin = targetHasAdmin(target);
        if (!targetHasAdmin && permissionService.isAdmin(userId)) {
            long adminCount = adminUserCount();
            if (adminCount <= 1) {
                throw new BusinessException(400, "至少保留一名管理员");
            }
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (Long roleId : target) {
            if (roleMapper.selectById(roleId) != null) {
                userRoleMapper.insert(new UserRole(userId, roleId));
            }
        }
        // 同步旧主角色列, 保证旧逻辑兼容
        Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleKey, Role.KEY_ADMIN).last("LIMIT 1"));
        if (adminRole != null && target.contains(adminRole.getId())) {
            user.setRole(User.ROLE_ADMIN);
        } else {
            user.setRole(User.ROLE_USER);
        }
        userMapper.updateById(user);
        jwtUtil.revokeAllForUser(userId);
    }

    private boolean targetHasAdmin(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleKey, Role.KEY_ADMIN).last("LIMIT 1"));
        return adminRole != null && roleIds.contains(adminRole.getId());
    }

    private long adminUserCount() {
        Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleKey, Role.KEY_ADMIN).last("LIMIT 1"));
        if (adminRole == null) {
            return 0;
        }
        return safeCount(userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRoleId, adminRole.getId())));
    }

    /** 封禁/启用用户 */
    @Transactional
    public void updateUserStatus(Long userId, Object status, Long operatorId) {
        if (userId.equals(operatorId)) {
            throw new BusinessException(400, "不能封禁当前登录账号");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        boolean enabled = status != null && (Boolean.TRUE.equals(status)
                || "true".equalsIgnoreCase(String.valueOf(status)) || "1".equals(String.valueOf(status)));
        user.setStatus(enabled);
        userMapper.updateById(user);
        if (!enabled) {
            // 封禁后使其所有 token 失效
            jwtUtil.revokeAllForUser(userId);
        }
    }

    /** 管理员重置用户密码 */
    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BusinessException(400, "密码长度至少 6 位");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        user.setPassword(encoder.encode(newPassword));
        userMapper.updateById(user);
        jwtUtil.revokeAllForUser(userId);
    }

    /** 用户详情: 用户 + 模板/任务/文件 */
    public UserDetailVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole() == null ? User.ROLE_USER : user.getRole());
        vo.setStatus(user.getStatus() == null ? Boolean.TRUE : user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setRoleNames(roleNamesOf(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)).stream().map(UserRole::getRoleId).collect(Collectors.toList())));

        List<FormatTemplate> templates = templateMapper.selectList(new LambdaQueryWrapper<FormatTemplate>()
                .eq(FormatTemplate::getUserId, userId).orderByDesc(FormatTemplate::getUpdateTime));
        List<FormatTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                .eq(FormatTask::getUserId, userId).orderByDesc(FormatTask::getId));
        List<PaperFile> papers = paperFileMapper.selectList(new LambdaQueryWrapper<PaperFile>()
                .eq(PaperFile::getUserId, userId).orderByDesc(PaperFile::getId));

        vo.setTemplateCount(templates.size());
        vo.setTaskCount(tasks.size());
        vo.setPaperCount(papers.size());

        vo.setTemplates(templates.stream().map(t -> {
            UserDetailVO.Item it = new UserDetailVO.Item();
            it.setId(t.getId());
            it.setName(t.getName());
            it.setTime(t.getUpdateTime());
            return it;
        }).collect(Collectors.toList()));

        Map<Long, String> fileNameMap = papers.stream().collect(Collectors.toMap(
                PaperFile::getId, PaperFile::getOriginalName, (a, b) -> a));
        vo.setTasks(tasks.stream().map(t -> {
            UserDetailVO.Item it = new UserDetailVO.Item();
            it.setId(t.getId());
            it.setName(fileNameMap.getOrDefault(t.getFileId(), String.valueOf(t.getFileId())));
            it.setStatus(t.getStatus());
            it.setExtra(t.getErrorMsg());
            it.setTime(t.getCreateTime());
            return it;
        }).collect(Collectors.toList()));

        vo.setPapers(papers.stream().map(p -> {
            UserDetailVO.Item it = new UserDetailVO.Item();
            it.setId(p.getId());
            it.setName(p.getOriginalName());
            it.setTime(p.getCreateTime());
            return it;
        }).collect(Collectors.toList()));
        return vo;
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

    // ==================== 模板市场 ====================

    public PageResult<Map<String, Object>> listMarketTemplates(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<FormatTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(FormatTemplate::getName, keyword.trim());
        }
        wrapper.orderByDesc(FormatTemplate::getUpdateTime);
        IPage<FormatTemplate> page = templateMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Long> userIds = page.getRecords().stream().map(FormatTemplate::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> usernameMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream().collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        List<Map<String, Object>> records = new ArrayList<>();
        for (FormatTemplate t : page.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("userId", t.getUserId());
            m.put("username", usernameMap.getOrDefault(t.getUserId(), "-"));
            m.put("isPublic", Boolean.TRUE.equals(t.getIsPublic()));
            m.put("recommended", Boolean.TRUE.equals(t.getRecommended()));
            m.put("category", t.getCategory());
            m.put("downloadCount", t.getDownloadCount() == null ? 0 : t.getDownloadCount());
            m.put("ratingAvg", t.getRatingAvg() == null ? 0.0 : t.getRatingAvg().doubleValue());
            m.put("ratingCount", t.getRatingCount() == null ? 0 : t.getRatingCount());
            m.put("publicTime", t.getPublicTime());
            m.put("ruleCount", ruleMapper.selectCount(new LambdaQueryWrapper<FormatRule>()
                    .eq(FormatRule::getTemplateId, t.getId())));
            m.put("taskCount", countByKey(taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                            .eq(FormatTask::getTemplateId, t.getId()).select(FormatTask::getTemplateId)),
                    FormatTask::getTemplateId).getOrDefault(t.getId(), 0L));
            m.put("updateTime", t.getUpdateTime());
            records.add(m);
        }
        return PageResult.of(page.getTotal(), records);
    }

    /** 模板市场审核: 管理员查看任意模板完整详情(含全部配置与格式规则) */
    public Map<String, Object> marketTemplateDetail(Long id) {
        FormatTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(404, "模板不存在");
        }
        User owner = userMapper.selectById(t.getUserId());
        List<FormatRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<FormatRule>()
                .eq(FormatRule::getTemplateId, id));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("userId", t.getUserId());
        m.put("username", owner == null ? "-" : owner.getUsername());
        m.put("isPublic", Boolean.TRUE.equals(t.getIsPublic()));
        m.put("recommended", Boolean.TRUE.equals(t.getRecommended()));
        m.put("category", t.getCategory());
        m.put("downloadCount", t.getDownloadCount() == null ? 0 : t.getDownloadCount());
        m.put("ratingAvg", t.getRatingAvg() == null ? 0.0 : t.getRatingAvg().doubleValue());
        m.put("ratingCount", t.getRatingCount() == null ? 0 : t.getRatingCount());
        m.put("publicTime", t.getPublicTime());
        m.put("createTime", t.getCreateTime());
        m.put("updateTime", t.getUpdateTime());
        m.put("generateToc", Boolean.TRUE.equals(t.getGenerateToc()));
        m.put("pageConfig", t.getPageConfig());
        m.put("headingPatterns", t.getHeadingPatterns());
        m.put("coverConfig", t.getCoverConfig());
        m.put("referenceConfig", t.getReferenceConfig());
        m.put("rules", rules);
        return m;
    }

    /** 上架/下架/推荐模板 */
    @Transactional
    public void setMarketTemplate(Long id, Boolean isPublic, Boolean recommended, String category) {
        FormatTemplate template = dbRetryService.execute(() -> templateMapper.selectById(id));
        if (template == null) {
            throw new BusinessException(404, "模板不存在或已被删除");
        }
        if (isPublic != null) {
            template.setIsPublic(isPublic);
            if (Boolean.TRUE.equals(isPublic)) {
                template.setPublicTime(template.getPublicTime() == null ? LocalDateTime.now() : template.getPublicTime());
            } else {
                template.setPublicTime(null);
                template.setRecommended(false);
            }
        }
        if (recommended != null) {
            template.setRecommended(recommended);
        }
        if (category != null && !category.trim().isEmpty()) {
            template.setCategory(category.trim());
        }
        template.setUpdateTime(LocalDateTime.now());
        try {
            Integer rows = dbRetryService.execute(() -> templateMapper.updateById(template));
            if (rows == null || rows == 0) {
                // 并发下模板可能已被删除/变更, 返回可重试的明确错误而非 500
                throw new BusinessException(409, "模板状态已变更，请刷新后重试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "模板状态更新失败，请重试");
        }
    }

    // ==================== 报表导出 ====================

    /** 导出用户数据(Excel 行数据) */
    public List<AdminUserVO> listAllUsersForExport() {
        return toUserVOs(userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime)));
    }

    /** 导出模板数据 */
    public List<AdminTemplateVO> listAllTemplatesForExport() {
        return toTemplateVOs(templateMapper.selectList(new LambdaQueryWrapper<FormatTemplate>()
                .orderByDesc(FormatTemplate::getUpdateTime)));
    }

    /** 导出任务数据 */
    public List<AdminTaskVO> listAllTasksForExport() {
        return toTaskVOs(taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                .orderByDesc(FormatTask::getId)));
    }

    // ==================== 统计增强 ====================

    /** 任务成功率(近30天) */
    public Map<String, Object> taskSuccessRate() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long total = safeCount(taskMapper.selectCount(new LambdaQueryWrapper<FormatTask>()
                .ge(FormatTask::getCreateTime, since)));
        long success = safeCount(taskMapper.selectCount(new LambdaQueryWrapper<FormatTask>()
                .eq(FormatTask::getStatus, FormatTask.STATUS_SUCCESS)
                .ge(FormatTask::getCreateTime, since)));
        long failed = safeCount(taskMapper.selectCount(new LambdaQueryWrapper<FormatTask>()
                .eq(FormatTask::getStatus, FormatTask.STATUS_FAILED)
                .ge(FormatTask::getCreateTime, since)));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("success", success);
        m.put("failed", failed);
        m.put("rate", total == 0 ? 0 : Math.round(success * 100.0 / total));
        return m;
    }

    /** 失败原因 TOP */
    public List<Map<String, Object>> failureReasons() {
        List<FormatTask> failed = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                .eq(FormatTask::getStatus, FormatTask.STATUS_FAILED)
                .isNotNull(FormatTask::getErrorMsg)
                .orderByDesc(FormatTask::getId)
                .last("LIMIT 500"));
        Map<String, Long> reasonCount = new HashMap<>();
        for (FormatTask t : failed) {
            String msg = t.getErrorMsg();
            String reason = "未知错误";
            if (msg != null) {
                if (msg.contains("Zip bomb") || msg.contains("zip")) reason = "压缩炸弹/超大文件";
                else if (msg.contains("NullPointer") || msg.contains("null")) reason = "空指针/文件解析异常";
                else if (msg.contains("content type") || msg.contains("InvalidFormat")) reason = "文件格式损坏";
                else if (msg.contains("already exists") || msg.contains("part name")) reason = "文档结构异常";
                else if (msg.contains("时间") || msg.contains("cancel")) reason = "取消/超时";
                else reason = msg.length() > 40 ? msg.substring(0, 40) : msg;
            }
            reasonCount.merge(reason, 1L, Long::sum);
        }
        return reasonCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("reason", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).collect(Collectors.toList());
    }

    /** 模板使用量 TOP */
    public List<Map<String, Object>> topTemplates() {
        List<FormatTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                .ge(FormatTask::getCreateTime, LocalDateTime.now().minusDays(30)));
        Map<Long, Long> count = new HashMap<>();
        for (FormatTask t : tasks) {
            if (t.getTemplateId() != null) {
                count.merge(t.getTemplateId(), 1L, Long::sum);
            }
        }
        List<Long> templateIds = count.keySet().stream().sorted((a, b) -> Long.compare(count.get(b), count.get(a)))
                .limit(10).collect(Collectors.toList());
        Map<Long, String> nameMap = templateIds.isEmpty() ? Collections.emptyMap()
                : templateMapper.selectBatchIds(templateIds).stream()
                .collect(Collectors.toMap(FormatTemplate::getId, FormatTemplate::getName, (a, b) -> a));
        return templateIds.stream().map(id -> {
            Map<String, Object> m = new HashMap<>();
            m.put("templateId", id);
            m.put("name", nameMap.getOrDefault(id, "-"));
            m.put("count", count.get(id));
            return m;
        }).collect(Collectors.toList());
    }

    /** 用户活跃 TOP */
    public List<Map<String, Object>> topUsers() {
        List<FormatTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                .ge(FormatTask::getCreateTime, LocalDateTime.now().minusDays(30)));
        Map<Long, Long> count = new HashMap<>();
        for (FormatTask t : tasks) {
            count.merge(t.getUserId(), 1L, Long::sum);
        }
        List<Long> userIds = count.keySet().stream().sorted((a, b) -> Long.compare(count.get(b), count.get(a)))
                .limit(10).collect(Collectors.toList());
        Map<Long, String> nameMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        return userIds.stream().map(id -> {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", id);
            m.put("username", nameMap.getOrDefault(id, "-"));
            m.put("count", count.get(id));
            return m;
        }).collect(Collectors.toList());
    }

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
