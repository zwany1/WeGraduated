package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.ForgotPasswordDTO;
import com.graduate.thesis.dto.LoginResponse;
import com.graduate.thesis.dto.UserAuthDTO;
import com.graduate.thesis.dto.UserProfileDTO;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.mapper.UserMapper;
import com.graduate.thesis.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
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
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 登录失败记录: username -> 失败次数 */
    private final ConcurrentHashMap<String, AtomicInteger> failCount = new ConcurrentHashMap<>();
    /** 锁定记录: username -> 解锁时间戳 */
    private final ConcurrentHashMap<String, Long> lockUntil = new ConcurrentHashMap<>();

    public UserService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse register(UserAuthDTO dto) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已存在");
        }
        checkPasswordStrength(dto.getPassword());

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setNickname(dto.getUsername());
        if (dto.getSecurityQuestion() != null && !dto.getSecurityQuestion().trim().isEmpty()
                && dto.getSecurityAnswer() != null && !dto.getSecurityAnswer().trim().isEmpty()) {
            user.setSecurityQuestion(dto.getSecurityQuestion().trim());
            user.setSecurityAnswer(encoder.encode(dto.getSecurityAnswer().trim()));
        }
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return buildLoginResponse(user);
    }

    public LoginResponse login(UserAuthDTO dto) {
        String username = dto.getUsername();
        // 限流检查
        Long lockedUntil = lockUntil.get(username);
        if (lockedUntil != null && System.currentTimeMillis() < lockedUntil) {
            long minutes = (lockedUntil - System.currentTimeMillis()) / 60000 + 1;
            throw new BusinessException("登录失败次数过多，账号已锁定，请 " + minutes + " 分钟后再试");
        }
        lockUntil.remove(username);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            AtomicInteger counter = failCount.computeIfAbsent(username, k -> new AtomicInteger());
            int times = counter.incrementAndGet();
            if (times >= MAX_FAILURES) {
                lockUntil.put(username, System.currentTimeMillis() + LOCK_MILLIS);
                failCount.remove(username);
                throw new BusinessException("登录失败次数过多，账号已锁定 10 分钟");
            }
            throw new BusinessException("用户名或密码错误，还可尝试 " + (MAX_FAILURES - times) + " 次");
        }
        // 成功则清零
        failCount.remove(username);
        lockUntil.remove(username);
        return buildLoginResponse(user);
    }

    public LoginResponse forgotPassword(ForgotPasswordDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || user.getSecurityAnswer() == null || !encoder.matches(dto.getAnswer(), user.getSecurityAnswer())) {
            throw new BusinessException("用户名或密保答案错误");
        }
        checkPasswordStrength(dto.getNewPassword());
        user.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        // 重置后清除该用户已发出的 token
        jwtUtil.revokeAllForUser(user.getId());
        return buildLoginResponse(user);
    }

    public void logout(Long userId, String token) {
        if (token != null && !token.isEmpty()) {
            jwtUtil.revoke(token);
        }
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
        resp.setSecurityQuestion(user.getSecurityQuestion());
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
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setSecurityQuestion(user.getSecurityQuestion());
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
        if (dto.getSecurityQuestion() != null) {
            String q = dto.getSecurityQuestion().trim();
            if (q.isEmpty()) {
                user.setSecurityQuestion(null);
                user.setSecurityAnswer(null);
            } else {
                if (dto.getSecurityAnswer() == null || dto.getSecurityAnswer().trim().isEmpty()) {
                    throw new BusinessException("请填写密保答案");
                }
                user.setSecurityQuestion(q);
                user.setSecurityAnswer(encoder.encode(dto.getSecurityAnswer().trim()));
            }
        }
        userMapper.updateById(user);
        UserProfileDTO result = new UserProfileDTO();
        result.setUsername(user.getUsername());
        result.setNickname(user.getNickname());
        result.setAvatar(user.getAvatar());
        result.setSecurityQuestion(user.getSecurityQuestion());
        return result;
    }
}
