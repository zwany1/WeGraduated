package com.graduate.thesis.service;

import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 数据库操作重试封装: 对瞬时 DB 异常(DataAccessException)自动重试, 重试耗尽后抛出原异常.
 */
@Service
public class DbRetryService {

    private final RetryTemplate retryTemplate;

    public DbRetryService(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }

    /** 执行可能瞬时失败的 DB 操作, 自动重试(默认 3 次) */
    public <T> T execute(Supplier<T> action) {
        return retryTemplate.execute(context -> action.get());
    }

    /** 执行返回 void 的 DB 操作, 自动重试 */
    public void run(Runnable action) {
        retryTemplate.execute(context -> {
            action.run();
            return null;
        });
    }
}
