package com.graduate.thesis.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.mapper.PaperFileMapper;
import com.graduate.thesis.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 定时任务: 超时任务回收 + 孤儿文件清理
 */
@Slf4j
@Component
public class TaskCleaner {

    private static final int TASK_TIMEOUT_HOURS = 2;

    private final FormatTaskMapper taskMapper;
    private final PaperFileMapper paperFileMapper;
    private final StorageService storageService;
    private final int keepDays;

    public TaskCleaner(FormatTaskMapper taskMapper,
                       PaperFileMapper paperFileMapper,
                       StorageService storageService,
                       @Value("${thesis.storage.keep-days:30}") int keepDays) {
        this.taskMapper = taskMapper;
        this.paperFileMapper = paperFileMapper;
        this.storageService = storageService;
        this.keepDays = keepDays;
    }

    /** 每 6 小时回收一次超时任务 */
    @Scheduled(fixedDelay = 6 * 3600 * 1000L)
    public void recycleTimeoutTasks() {
        try {
            List<FormatTask> stuck = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                    .in(FormatTask::getStatus, FormatTask.STATUS_PENDING, FormatTask.STATUS_PROCESSING)
                    .lt(FormatTask::getCreateTime, LocalDateTime.now().minusHours(TASK_TIMEOUT_HOURS)));
            for (FormatTask task : stuck) {
                task.setStatus(FormatTask.STATUS_FAILED);
                task.setErrorMsg("任务执行超时, 已被系统回收");
                task.setFinishTime(LocalDateTime.now());
                taskMapper.updateById(task);
            }
            if (!stuck.isEmpty()) {
                log.info("[Cleaner] 已回收 {} 个超时任务", stuck.size());
            }
        } catch (Exception e) {
            log.warn("[Cleaner] 超时任务回收失败: {}", e.getMessage());
        }
    }

    /** 每天凌晨清理孤儿文件 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOrphanFiles() {
        try {
            Set<String> referenced = new HashSet<>();
            for (FormatTask t : taskMapper.selectList(null)) {
                if (t.getResultPath() != null) {
                    referenced.add(t.getResultPath());
                }
                if (t.getPdfPath() != null) {
                    referenced.add(t.getPdfPath());
                }
            }
            for (PaperFile f : paperFileMapper.selectList(null)) {
                referenced.add(f.getStoredPath());
            }
            int deleted = storageService.cleanupOrphans(referenced, keepDays);
            if (deleted > 0) {
                log.info("[Cleaner] 已清理 {} 个孤儿文件", deleted);
            }
        } catch (Exception e) {
            log.warn("[Cleaner] 孤儿文件清理失败: {}", e.getMessage());
        }
    }
}
