package com.graduate.thesis.config;

import com.graduate.thesis.annotation.OperLog;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.service.LogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志切面: 记录标注 @OperLog 的写操作
 */
@Aspect
@Component
public class OperLogAspect {

    private final LogService logService;

    public OperLogAspect(LogService logService) {
        this.logService = logService;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint pjp, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        String params = buildParams(pjp.getArgs());
        try {
            Object result = pjp.proceed();
            logService.recordOper(UserContext.get(), operLog.module(), operLog.action(),
                    methodName, params, true, null, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            logService.recordOper(UserContext.get(), operLog.module(), operLog.action(),
                    methodName, params, false, e.getMessage(), System.currentTimeMillis() - start);
            throw e;
        }
    }

    private String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (Object arg : args) {
            String json = sanitizeParams(logService.toJson(arg));
            if (json != null) {
                parts.add(json);
            }
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    /** 脱敏敏感字段(密码等), 避免明文写入操作日志 */
    private String sanitizeParams(String json) {
        if (json == null) {
            return null;
        }
        return json.replaceAll("(\"(?:password|newPassword|oldPassword|pwd)\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
    }
}
