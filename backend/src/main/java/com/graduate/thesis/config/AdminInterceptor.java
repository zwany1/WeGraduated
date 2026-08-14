package com.graduate.thesis.config;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 管理后台权限拦截器(仅校验角色, 登录校验由 LoginInterceptor 先行完成)
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    public AdminInterceptor(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !User.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException(403, "无管理后台访问权限");
        }
        return true;
    }
}
