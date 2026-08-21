package com.graduate.thesis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 排版任务专用线程池: 限制并发上限, 替代默认 SimpleAsyncTaskExecutor(每次新建线程, 无上限, 高并发 OOM).
 * 队列满时由调用线程同步执行(CallerRunsPolicy), 形成天然背压.
 */
@Configuration
public class AsyncConfig {

    @Bean("formatExecutor")
    public ThreadPoolTaskExecutor formatExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("format-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.initialize();
        return exec;
    }
}
