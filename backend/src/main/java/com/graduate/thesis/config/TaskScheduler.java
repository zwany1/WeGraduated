package com.graduate.thesis.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @Scheduled(fixedDelay = 500)
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

    private int maxConcurrent() {
        try {
            String v = systemService.getConfigValue("task.max.concurrent");
            if (v != null && v.trim().matches("\\d+")) {
                int n = Integer.parseInt(v.trim());
                if (n > 0 && n <= 20) {
                    return n;
                }
            }
        } catch (Exception ignore) {
        }
        return 4;
    }
}
