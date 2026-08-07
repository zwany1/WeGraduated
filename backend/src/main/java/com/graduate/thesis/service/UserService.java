package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.LoginResponse;
import com.graduate.thesis.dto.UserAuthDTO;
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

    private String encrypt(String username, String password) {
        String raw = password + ":" + username;
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
