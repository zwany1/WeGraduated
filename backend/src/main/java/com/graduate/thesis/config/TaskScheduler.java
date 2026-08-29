package com.graduate.thesis.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.service.PaperService;
import com.graduate.thesis.service.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 排版任务调度器: 按并发上限从队列(PENDING)取出任务异步执行, 避免大量任务同时跑占满资源.
 * 并发上限由系统参数 task.max.concurrent 控制(默认 4).
 */
@Slf4j
@Component("formatTaskDispatcher")
public class TaskScheduler {

    private final FormatTaskMapper taskMapper;
    private final PaperService paperService;
    private final SystemService systemService;

    public TaskScheduler(FormatTaskMapper taskMapper,
                         PaperService paperService,
                         SystemService systemService) {
        this.taskMapper = taskMapper;
        this.paperService = paperService;
        this.systemService = systemService;
    }

    @Scheduled(fixedDelayString = "${thesis.scheduler.interval:1500}")
    public void dispatch() {
        try {
            int maxConcurrent = maxConcurrent();
            long running = countRunning();
            int slots = (int) (maxConcurrent - running);
            if (slots <= 0) {
                return;
            }
            List<FormatTask> pending = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                    .eq(FormatTask::getStatus, FormatTask.STATUS_PENDING)
                    .orderByAsc(FormatTask::getId)
                    .last("LIMIT " + slots));
            for (FormatTask task : pending) {
                // CAS 原子抢占: 只把状态从 PENDING 改成 PROCESSING 成功才派发, 避免并发/重试重复执行同一任务
                int affected = taskMapper.update(null, new LambdaUpdateWrapper<FormatTask>()
                        .eq(FormatTask::getId, task.getId())
                        .eq(FormatTask::getStatus, FormatTask.STATUS_PENDING)
                        .set(FormatTask::getStatus, FormatTask.STATUS_PROCESSING));
                if (affected <= 0) {
                    continue;
                }
                try {
                    paperService.runFormat(task.getId());
                } catch (Exception e) {
                    log.warn("派发排版任务失败 taskId={}", task.getId(), e);
                }
            }
        } catch (Exception e) {
            log.warn("[TaskScheduler] 任务派发异常: {}", e.getMessage());
        }
    }

    private long countRunning() {
        Long c = taskMapper.selectCount(new LambdaQueryWrapper<FormatTask>()
                .eq(FormatTask::getStatus, FormatTask.STATUS_PROCESSING));
        return c == null ? 0 : c;
    }

    /** 并发上限配置缓存: 该配置几乎不变, 不必每 tick 查一次 t_config, 60s 刷新一次 */
    private volatile int cachedMaxConcurrent = 4;
    private volatile long configLoadedAt = 0L;
    private static final long CONFIG_TTL_MILLIS = 60_000L;

    private int maxConcurrent() {
        long now = System.currentTimeMillis();
        if (now - configLoadedAt > CONFIG_TTL_MILLIS) {
            try {
                String v = systemService.getConfigValue("task.max.concurrent");
                if (v != null && v.trim().matches("\\d+")) {
                    int n = Integer.parseInt(v.trim());
                    if (n > 0 && n <= 20) {
                        cachedMaxConcurrent = n;
                    }
                }
                configLoadedAt = now;
            } catch (Exception ignore) {
                // 查询失败沿用上次缓存值
            }
        }
        return cachedMaxConcurrent;
    }
}
