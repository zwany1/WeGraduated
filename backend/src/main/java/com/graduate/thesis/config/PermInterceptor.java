package com.graduate.thesis.config;

import com.graduate.thesis.annotation.RequiresPerms;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.service.PermissionService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 按钮级权限拦截器: 校验方法/类上的 @RequiresPerms 注解.
 * 登录校验由 LoginInterceptor 先行完成.
 */
@Component
public class PermInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    public PermInterceptor(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiresPerms requires = handlerMethod.getMethodAnnotation(RequiresPerms.class);
        if (requires == null) {
            requires = handlerMethod.getBeanType().getAnnotation(RequiresPerms.class);
        }
        if (requires == null) {
            return true;
        }
        Long userId = UserContext.get();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        if (!permissionService.hasPerms(userId, requires.value(), requires.logical())) {
            throw new BusinessException(403, "无操作权限");
        }
        return true;
    }
}
