package com.graduate.thesis.config;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 管理后台登录护栏: 仅校验已登录(登录校验由 LoginInterceptor 完成).
 * 具体接口权限由 @RequiresPerms 按按钮级权限标识校验.
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return true;
    }
}
