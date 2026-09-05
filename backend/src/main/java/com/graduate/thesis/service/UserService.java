package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.LoginDTO;
import com.graduate.thesis.dto.LoginResponse;
import com.graduate.thesis.dto.ResetPasswordDTO;
import com.graduate.thesis.dto.UserAuthDTO;
import com.graduate.thesis.dto.UserInfoResponse;
import com.graduate.thesis.dto.UserProfileDTO;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户服务
 */
@Service
public class UserService {

    /** 最大连续失败次数 */
    private static final int MAX_FAILURES = 5;
    /** 失败达到此次数后要求图形验证码(降低正常用户登录摩擦) */
    private static final int CAPTCHA_THRESHOLD = 2;
    /** 锁定时间(毫秒) */
    private static final long LOCK_MILLIS = 10 * 60 * 1000L;
    /** 同一 IP 最大失败次数(按 IP 限流) */
    private static final int IP_MAX_FAILURES = 15;
    /** 登录限流 Map 容量上限, 超限清理防止内存累积 */
    private static final int LOGIN_MAP_MAX = 10000;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final EmailCodeService emailCodeService;
    private final FormatTemplateMapper templateMapper;
    private final FormatRuleMapper ruleMapper;
    private final FormatTaskMapper taskMapper;
    private final PaperFileMapper paperFileMapper;
    private final StorageService storageService;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionService permissionService;
    private final LogService logService;
    private final LoginSessionService sessionService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 登录失败记录: 登录名(用户名或邮箱) -> 失败次数 */
    private final ConcurrentHashMap<String, AtomicInteger> failCount = new ConcurrentHashMap<>();
    /** 锁定记录: 登录名 -> 解锁时间戳 */
    private final ConcurrentHashMap<String, Long> lockUntil = new ConcurrentHashMap<>();
    /** IP 失败记录: IP -> 失败次数(防止换个用户名继续爆破) */
    private final ConcurrentHashMap<String, AtomicInteger> ipFailCount = new ConcurrentHashMap<>();
    /** IP 锁定记录: IP -> 解锁时间戳 */
    private final ConcurrentHashMap<String, Long> ipLockUntil = new ConcurrentHashMap<>();

    public UserService(UserMapper userMapper, JwtUtil jwtUtil, EmailCodeService emailCodeService,
                       FormatTemplateMapper templateMapper, FormatRuleMapper ruleMapper,
                       FormatTaskMapper taskMapper, PaperFileMapper paperFileMapper,
                       StorageService storageService, RoleMapper roleMapper,
                       UserRoleMapper userRoleMapper, PermissionService permissionService,
                       LogService logService, LoginSessionService sessionService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.emailCodeService = emailCodeService;
        this.templateMapper = templateMapper;
        this.ruleMapper = ruleMapper;
        this.taskMapper = taskMapper;
        this.paperFileMapper = paperFileMapper;
        this.storageService = storageService;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.permissionService = permissionService;
        this.logService = logService;
        this.sessionService = sessionService;
    }

    /** 邮箱是否已绑定账号(发码场景校验用) */
    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        Long c = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email.trim().toLowerCase()));
        return c != null && c > 0;
    }

    public LoginResponse resetPassword(ResetPasswordDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (user == null) {
            throw new BusinessException("该邮箱未绑定任何账号，无法通过邮箱重置密码；请确认邮箱，或联系管理员处理");
        }
        // 封禁账号不得通过免登录的重置流程重新获得登录态
        if (Boolean.FALSE.equals(user.getStatus())) {
            throw new BusinessException("该账号已被禁用，无法重置密码，请联系管理员");
        }
        emailCodeService.verify(email, dto.getEmailCode());
        checkPasswordStrength(dto.getNewPassword());
        user.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        // 重置后失效该用户所有旧 token
        jwtUtil.revokeAllForUser(user.getId());
        logService.recordLogin(user.getId(), user.getUsername(), true, "重置密码成功");
        LoginResponse resp = buildLoginResponse(user);
        sessionService.createSession(resp.getToken(), user.getId(), user.getUsername(), null,
                jwtUtil.getExpiration(resp.getToken()).getTime());
        return resp;
    }

    public LoginResponse register(UserAuthDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();
        Long emailExists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (emailExists != null && emailExists > 0) {
            throw new BusinessException("该邮箱已被注册");
        }
        checkPasswordStrength(dto.getPassword());

        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (username.isEmpty()) {
            username = autoGenerateUsername(email);
        } else if (isReservedUsername(username)) {
            throw new BusinessException("该用户名不可用");
        } else if (isUsernameTaken(username)) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setNickname(username);
        user.setRole(User.ROLE_USER);
        user.setStatus(true);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        assignDefaultUserRole(user.getId());
        logService.recordLogin(user.getId(), username, true, "注册成功");
        LoginResponse resp = buildLoginResponse(user);
        sessionService.createSession(resp.getToken(), user.getId(), username, null,
                jwtUtil.getExpiration(resp.getToken()).getTime());
        return resp;
    }

    /** 新注册用户绑定内置普通用户角色 */
    private void assignDefaultUserRole(Long userId) {
        Role userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleKey, Role.KEY_USER).last("LIMIT 1"));
        if (userRole != null) {
            Long exists = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, userId).eq(UserRole::getRoleId, userRole.getId()));
            if (exists == null || exists == 0) {
                userRoleMapper.insert(new UserRole(userId, userRole.getId()));
            }
        }
    }

    public LoginResponse login(LoginDTO dto, String ip) {
        sweepLoginMaps();
        String account = dto.getAccount() == null ? "" : dto.getAccount().trim().toLowerCase();
        if (account.isEmpty()) {
            throw new BusinessException("请输入用户名或邮箱");
        }
        // IP 维度限流(防止换用户名爆破)
        if (ip != null && !ip.isEmpty()) {
            Long ipLocked = ipLockUntil.get(ip);
            if (ipLocked != null && System.currentTimeMillis() < ipLocked) {
                long minutes = (ipLocked - System.currentTimeMillis()) / 60000 + 1;
                throw new BusinessException("尝试过于频繁，请 " + minutes + " 分钟后再试");
            }
            ipLockUntil.remove(ip);
        }
        // 账号维度限流
        Long lockedUntil = lockUntil.get(account);
        if (lockedUntil != null && System.currentTimeMillis() < lockedUntil) {
            long minutes = (lockedUntil - System.currentTimeMillis()) / 60000 + 1;
            throw new BusinessException("登录失败次数过多，账号已锁定，请 " + minutes + " 分钟后再试");
        }
        lockUntil.remove(account);

        User user = findUserByAccount(account);
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            AtomicInteger counter = failCount.computeIfAbsent(account, k -> new AtomicInteger());
            int times = counter.incrementAndGet();
            boolean ipLockedNow = false;
            if (ip != null && !ip.isEmpty()) {
                AtomicInteger ipCounter = ipFailCount.computeIfAbsent(ip, k -> new AtomicInteger());
                int ipTimes = ipCounter.incrementAndGet();
                if (ipTimes >= IP_MAX_FAILURES) {
                    ipLockUntil.put(ip, System.currentTimeMillis() + LOCK_MILLIS);
                    ipFailCount.remove(ip);
                    ipLockedNow = true;
                }
            }
            if (times >= MAX_FAILURES) {
                lockUntil.put(account, System.currentTimeMillis() + LOCK_MILLIS);
                failCount.remove(account);
                logService.recordLogin(null, account, false, "登录失败次数过多, 账号已锁定");
                throw new BusinessException("登录失败次数过多，账号已锁定 10 分钟");
            }
            if (ipLockedNow) {
                logService.recordLogin(null, account, false, "IP 尝试过于频繁, 已临时限制");
                throw new BusinessException("尝试过于频繁，请 10 分钟后再试");
            }
            logService.recordLogin(null, account, false, "用户名/邮箱或密码错误");
            throw new BusinessException("用户名/邮箱或密码错误");
        }
        // 封禁检查
        if (user.getStatus() != null && !user.getStatus()) {
            logService.recordLogin(user.getId(), user.getUsername(), false, "账号已被禁用");
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        // 成功则清零
        failCount.remove(account);
        lockUntil.remove(account);
        if (ip != null && !ip.isEmpty()) {
            ipFailCount.remove(ip);
            ipLockUntil.remove(ip);
        }
        logService.recordLogin(user.getId(), user.getUsername(), true, "登录成功");
        LoginResponse resp = buildLoginResponse(user);
        sessionService.createSession(resp.getToken(), user.getId(), user.getUsername(), ip,
                jwtUtil.getExpiration(resp.getToken()).getTime());
        return resp;
    }

    /** 清理登录限流 Map 的过期项与超限条目, 防止内存累积 */
    private void sweepLoginMaps() {
        long now = System.currentTimeMillis();
        lockUntil.entrySet().removeIf(e -> e.getValue() < now);
        ipLockUntil.entrySet().removeIf(e -> e.getValue() < now);
        if (failCount.size() > LOGIN_MAP_MAX) {
            failCount.clear();
        }
        if (ipFailCount.size() > LOGIN_MAP_MAX) {
            ipFailCount.clear();
        }
    }

    /** 登录是否需要图形验证码(策略: 始终要求, 防止自动化登录尝试) */
    public boolean needCaptcha(String account) {
        return account != null && !account.isEmpty();
    }

    public void logout(Long userId, String token) {
        if (token != null && !token.isEmpty()) {
            jwtUtil.revoke(token);
            sessionService.removeByToken(token);
        }
    }

    /**
     * 注销账号: 删除该用户全部数据(模板/规则/任务/论文文件)及磁盘文件
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 1. 模板与规则
        List<FormatTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<FormatTemplate>().eq(FormatTemplate::getUserId, userId));
        for (FormatTemplate t : templates) {
            ruleMapper.delete(new LambdaQueryWrapper<FormatRule>()
                    .eq(FormatRule::getTemplateId, t.getId()));
        }
        templateMapper.delete(new LambdaQueryWrapper<FormatTemplate>()
                .eq(FormatTemplate::getUserId, userId));
        // 2. 任务与磁盘文件
        List<FormatTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<FormatTask>().eq(FormatTask::getUserId, userId));
        for (FormatTask t : tasks) {
            storageService.delete(t.getResultPath());
        }
        taskMapper.delete(new LambdaQueryWrapper<FormatTask>()
                .eq(FormatTask::getUserId, userId));
        // 3. 上传的论文文件
        List<PaperFile> files = paperFileMapper.selectList(
                new LambdaQueryWrapper<PaperFile>().eq(PaperFile::getUserId, userId));
        for (PaperFile f : files) {
            storageService.delete(f.getStoredPath());
        }
        paperFileMapper.delete(new LambdaQueryWrapper<PaperFile>()
                .eq(PaperFile::getUserId, userId));
        // 4. 用户角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));
        // 5. 用户本身
        userMapper.deleteById(userId);
        jwtUtil.revokeAllForUser(userId);
    }

    private User findUserByAccount(String account) {
        User byUsername = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, account));
        if (byUsername != null) {
            return byUsername;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, account.toLowerCase()));
    }

    private boolean isUsernameTaken(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        return count != null && count > 0;
    }

    /** 保留用户名(与系统管理员账号冲突), 禁止普通注册占用 */
    private boolean isReservedUsername(String username) {
        return "admin".equalsIgnoreCase(username);
    }

    /** 未填用户名时自动生成: 邮箱前缀，冲突则追加随机数字 */
    private String autoGenerateUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9_]", "");
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        if (base.isEmpty()) {
            base = "user";
        }
        for (int i = 0; i < 10; i++) {
            String candidate = base + ThreadLocalRandom.current().nextInt(100, 9999);
            if (!isUsernameTaken(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("用户名生成失败，请手动填写用户名");
    }

    public void checkPasswordStrength(String password) {
        if (password == null || password.length() < 8 || password.length() > 64) {
            throw new BusinessException("密码长度需为 8-64 位");
        }
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        if (!(hasLetter && hasDigit)) {
            throw new BusinessException("密码必须同时包含字母和数字");
        }
    }

    private LoginResponse buildLoginResponse(User user) {
        LoginResponse resp = new LoginResponse();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setEmail(user.getEmail());
        resp.setRole(user.getRole() == null ? User.ROLE_USER : user.getRole());
        List<String> roleKeys = new ArrayList<>(permissionService.getUserRoleKeys(user.getId()));
        resp.setRoles(roleKeys);
        resp.setPerms(permissionService.getUserPerms(user.getId()));
        resp.setToken(jwtUtil.generate(user.getId()));
        return resp;
    }

    public UserProfileDTO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setBio(user.getBio());
        dto.setGender(user.getGender());
        dto.setSchool(user.getSchool());
        dto.setMajor(user.getMajor());
        dto.setCity(user.getCity());
        dto.setPhone(user.getPhone());
        return dto;
    }

    /** 当前用户权限信息 */
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        UserInfoResponse resp = new UserInfoResponse();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setEmail(user.getEmail());
        resp.setRole(user.getRole() == null ? User.ROLE_USER : user.getRole());
        resp.setRoles(new ArrayList<>(permissionService.getUserRoleKeys(userId)));
        resp.setPerms(permissionService.getUserPerms(userId));
        return resp;
    }

    public UserProfileDTO updateProfile(Long userId, UserProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (dto.getNickname() != null) {
            String nick = dto.getNickname().trim();
            if (nick.length() > 30) {
                throw new BusinessException("昵称不能超过30个字符");
            }
            if (nick.isEmpty()) {
                throw new BusinessException("昵称不能为空");
            }
            user.setNickname(nick);
        }
        if (dto.getAvatar() != null) {
            // 空串视为清除头像
            user.setAvatar(dto.getAvatar().trim().isEmpty() ? null : dto.getAvatar().trim());
        }
        if (dto.getBio() != null) {
            String bio = dto.getBio().trim();
            if (bio.length() > 200) {
                throw new BusinessException("个人简介不能超过200个字符");
            }
            user.setBio(bio.isEmpty() ? null : bio);
        }
        if (dto.getGender() != null) {
            if (dto.getGender() < 0 || dto.getGender() > 2) {
                throw new BusinessException("性别取值不合法");
            }
            user.setGender(dto.getGender() == 0 ? null : dto.getGender());
        }
        if (dto.getSchool() != null) {
            String s = dto.getSchool().trim();
            if (s.length() > 80) throw new BusinessException("学校名称过长");
            user.setSchool(s.isEmpty() ? null : s);
        }
        if (dto.getMajor() != null) {
            String s = dto.getMajor().trim();
            if (s.length() > 80) throw new BusinessException("学院/专业名称过长");
            user.setMajor(s.isEmpty() ? null : s);
        }
        if (dto.getCity() != null) {
            String s = dto.getCity().trim();
            if (s.length() > 40) throw new BusinessException("城市名称过长");
            user.setCity(s.isEmpty() ? null : s);
        }
        if (dto.getPhone() != null) {
            String s = dto.getPhone().trim();
            if (!s.isEmpty() && !s.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException("手机号格式不正确");
            }
            user.setPhone(s.isEmpty() ? null : s);
        }
        userMapper.updateById(user);
        UserProfileDTO result = new UserProfileDTO();
        result.setUsername(user.getUsername());
        result.setEmail(user.getEmail());
        result.setNickname(user.getNickname());
        result.setAvatar(user.getAvatar());
        result.setBio(user.getBio());
        result.setGender(user.getGender());
        result.setSchool(user.getSchool());
        result.setMajor(user.getMajor());
        result.setCity(user.getCity());
        result.setPhone(user.getPhone());
        return result;
    }

    /** 换绑邮箱: 新邮箱必须未注册、验证码正确、旧密码确认 */
    public void changeEmail(Long userId, String newEmail, String code, String oldPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        String normalized = newEmail == null ? "" : newEmail.trim().toLowerCase();
        if (normalized.isEmpty() || !normalized.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new BusinessException("新邮箱格式不正确");
        }
        if (user.getEmail() != null && normalized.equals(user.getEmail().toLowerCase())) {
            throw new BusinessException("新邮箱与当前邮箱相同");
        }
        Long other = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, normalized)
                .ne(User::getId, userId));
        if (other != null && other > 0) {
            throw new BusinessException("该邮箱已被其他账号绑定");
        }
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("当前密码不正确");
        }
        emailCodeService.verify(newEmail.trim(), code);
        user.setEmail(normalized);
        userMapper.updateById(user);
    }

    /** 修改密码: 验证码(发到当前邮箱) + 旧密码校验 */
    public void changePassword(Long userId, String oldPassword, String newPassword, String emailCode) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new BusinessException("当前账号未绑定邮箱，请联系管理员重置");
        }
        if (oldPassword == null || !encoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("当前密码不正确");
        }
        // 与注册/重置统一的口令强度策略
        checkPasswordStrength(newPassword);
        if (encoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        emailCodeService.verify(user.getEmail(), emailCode);
        user.setPassword(encoder.encode(newPassword));
        userMapper.updateById(user);
        // 改密后吊销该用户全部旧 token, 被泄露的旧会话立即失效
        jwtUtil.revokeAllForUser(user.getId());
    }
}
