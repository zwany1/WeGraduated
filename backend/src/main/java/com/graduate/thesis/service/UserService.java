package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.LoginResponse;
import com.graduate.thesis.dto.UserAuthDTO;
import com.graduate.thesis.dto.UserProfileDTO;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.mapper.UserMapper;
import com.graduate.thesis.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 用户服务
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

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
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encrypt(dto.getUsername(), dto.getPassword()));
        user.setNickname(dto.getUsername());
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return buildLoginResponse(user);
    }

    public LoginResponse login(UserAuthDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || !encrypt(dto.getUsername(), dto.getPassword()).equals(user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        LoginResponse resp = new LoginResponse();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
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
        result.setNickname(user.getNickname());
        result.setAvatar(user.getAvatar());
        return result;
    }

    private String encrypt(String username, String password) {
        String raw = password + ":" + username;
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
