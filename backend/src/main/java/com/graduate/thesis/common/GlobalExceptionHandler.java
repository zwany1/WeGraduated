package com.graduate.thesis.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        // 仅 401/403 映射 HTTP 状态码(供前端拦截器跳登录/提示无权限), 其余保持 HTTP 200 + body.code
        HttpStatus status = e.getCode() == 401 ? HttpStatus.UNAUTHORIZED
                : e.getCode() == 403 ? HttpStatus.FORBIDDEN
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(400, msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        return Result.fail(400, "文件大小超过限制");
    }

    /** 请求体不是合法 JSON(如转义错误/编码错误): 明确 400, 避免落进兜底 500 掩盖真实原因 */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(400, "请求数据格式错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnknown(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后重试");
    }
}
