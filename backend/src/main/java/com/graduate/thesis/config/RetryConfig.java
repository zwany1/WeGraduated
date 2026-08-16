package com.graduate.thesis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.dao.DataAccessException;

import java.util.Collections;

/**
 * 重试配置: 对瞬时数据库异常(连接中断/锁超时等 DataAccessException)自动重试,
 * 避免偶发 500。供 DbRetryService 编程式使用。
 */
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate dbRetryTemplate() {
        RetryPolicy policy = new SimpleRetryPolicy(3,
                Collections.<Class<? extends Throwable>, Boolean>singletonMap(DataAccessException.class, true));
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(200);
        backOff.setMultiplier(2);
        backOff.setMaxInterval(1000);
        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(policy);
        template.setBackOffPolicy(backOff);
        return template;
    }
}
