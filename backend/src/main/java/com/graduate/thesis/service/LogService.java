package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.entity.LoginLog;
import com.graduate.thesis.entity.OperLog;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.mapper.LoginLogMapper;
import com.graduate.thesis.mapper.OperLogMapper;
import com.graduate.thesis.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志: 操作日志 / 登录日志
 */
@Service
public class LogService {

    private final OperLogMapper operLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public LogService(OperLogMapper operLogMapper,
                      LoginLogMapper loginLogMapper,
                      UserMapper userMapper,
                      ObjectMapper objectMapper) {
        this.operLogMapper = operLogMapper;
        this.loginLogMapper = loginLogMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    // ==================== 操作日志 ====================

    public void recordOper(Long userId, String module, String action, String method,
                           String params, boolean success, String errorMsg, long costMs) {
        try {
            OperLog log = new OperLog();
            log.setUserId(userId);
            if (userId != null) {
                User u = userMapper.selectById(userId);
                if (u != null) {
                    log.setUsername(u.getUsername());
                }
            }
            log.setModule(truncate(module, 60));
            log.setAction(truncate(action, 120));
            log.setMethod(truncate(method, 200));
            log.setParams(truncate(params, 4000));
            log.setIp(getClientIp());
            log.setStatus(success);
            log.setErrorMsg(truncate(errorMsg, 1000));
            log.setCostMs(costMs);
            log.setCreateTime(LocalDateTime.now());
            operLogMapper.insert(log);
        } catch (Exception ignore) {
            // 日志写入失败不影响主流程
        }
    }

    public PageResult<OperLog> listOperLogs(int pageNum, int pageSize, String keyword, Boolean status) {
        LambdaQueryWrapper<OperLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(OperLog::getUsername, kw).or().like(OperLog::getModule, kw)
                    .or().like(OperLog::getAction, kw).or().like(OperLog::getIp, kw));
        }
        if (status != null) {
            wrapper.eq(OperLog::getStatus, status);
        }
        wrapper.orderByDesc(OperLog::getId);
        IPage<OperLog> page = operLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void deleteOperLogs(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            operLogMapper.deleteBatchIds(ids);
        }
    }

    // ==================== 登录日志 ====================

    public void recordLogin(Long userId, String username, boolean success, String message) {
        try {
            LoginLog log = new LoginLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setIp(getClientIp());
            log.setUserAgent(getUserAgent());
            log.setStatus(success);
            log.setMessage(truncate(message, 200));
            log.setCreateTime(LocalDateTime.now());
            loginLogMapper.insert(log);
        } catch (Exception ignore) {
        }
    }

    public PageResult<LoginLog> listLoginLogs(int pageNum, int pageSize, String keyword, Boolean status) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(LoginLog::getUsername, kw).or().like(LoginLog::getIp, kw));
        }
        if (status != null) {
            wrapper.eq(LoginLog::getStatus, status);
        }
        wrapper.orderByDesc(LoginLog::getId);
        IPage<LoginLog> page = loginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void deleteLoginLogs(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            loginLogMapper.deleteBatchIds(ids);
        }
    }

    public static String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
            ip = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            return attrs.getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }

    /** 通用对象转 JSON 字符串(不可序列化的参数跳过) */
    public String toJson(Object arg) {
        if (arg == null) {
            return null;
        }
        if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                || arg instanceof javax.servlet.http.HttpSession
                || arg instanceof org.springframework.web.multipart.MultipartFile) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(arg);
        } catch (Exception e) {
            return "[unserializable]";
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
