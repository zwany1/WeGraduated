package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.LoginResponse;
import com.graduate.thesis.dto.ResetPasswordDTO;
import com.graduate.thesis.dto.UserAuthDTO;
import com.graduate.thesis.dto.UserProfileDTO;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    /** 锁定时间(毫秒) */
    private static final long LOCK_MILLIS = 10 * 60 * 1000L;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final EmailCodeService emailCodeService;
    private final FormatTemplateMapper templateMapper;
    private final FormatRuleMapper ruleMapper;
    private final FormatTaskMapper taskMapper;
    private final PaperFileMapper paperFileMapper;
    private final StorageService storageService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 登录失败记录: 登录名(用户名或邮箱) -> 失败次数 */
    private final ConcurrentHashMap<String, AtomicInteger> failCount = new ConcurrentHashMap<>();
    /** 锁定记录: 登录名 -> 解锁时间戳 */
    private final ConcurrentHashMap<String, Long> lockUntil = new ConcurrentHashMap<>();

    public UserService(UserMapper userMapper, JwtUtil jwtUtil, EmailCodeService emailCodeService,
                       FormatTemplateMapper templateMapper, FormatRuleMapper ruleMapper,
                       FormatTaskMapper taskMapper, PaperFileMapper paperFileMapper,
                       StorageService storageService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.emailCodeService = emailCodeService;
        this.templateMapper = templateMapper;
        this.ruleMapper = ruleMapper;
        this.taskMapper = taskMapper;
        this.paperFileMapper = paperFileMapper;
        this.storageService = storageService;
    }

    public LoginResponse resetPassword(ResetPasswordDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }
        emailCodeService.verify(email, dto.getEmailCode());
        checkPasswordStrength(dto.getNewPassword());
        user.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        // 重置后失效该用户所有旧 token
        jwtUtil.revokeAllForUser(user.getId());
        return buildLoginResponse(user);
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
        } else if (isUsernameTaken(username)) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setNickname(username);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return buildLoginResponse(user);
    }

    public LoginResponse login(UserAuthDTO dto) {
        String account = dto.getEmail() != null && !dto.getEmail().trim().isEmpty()
                ? dto.getEmail().trim().toLowerCase() : dto.getUsername();
        if (account == null || account.isEmpty()) {
            throw new BusinessException("请输入用户名或邮箱");
        }
        // 限流检查
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
            if (times >= MAX_FAILURES) {
                lockUntil.put(account, System.currentTimeMillis() + LOCK_MILLIS);
                failCount.remove(account);
                throw new BusinessException("登录失败次数过多，账号已锁定 10 分钟");
            }
            throw new BusinessException("用户名/邮箱或密码错误，还可尝试 " + (MAX_FAILURES - times) + " 次");
        }
        // 成功则清零
        failCount.remove(account);
        lockUntil.remove(account);
        return buildLoginResponse(user);
    }

    public void logout(Long userId, String token) {
        if (token != null && !token.isEmpty()) {
            jwtUtil.revoke(token);
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
            storageService.delete(t.getPdfPath());
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
        // 4. 用户本身
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

    private void checkPasswordStrength(String password) {
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
        resp.setToken(jwtUtil.generate(user.getId()));
        return resp;
    }

    public UserProfileDTO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        return dto;
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
        userMapper.updateById(user);
        UserProfileDTO result = new UserProfileDTO();
        result.setUsername(user.getUsername());
        result.setEmail(user.getEmail());
        result.setNickname(user.getNickname());
        result.setAvatar(user.getAvatar());
        return result;
    }
}
