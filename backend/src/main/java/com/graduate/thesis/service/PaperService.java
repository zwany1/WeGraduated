package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.DiffItem;
import com.graduate.thesis.dto.PaperFormatDTO;
import com.graduate.thesis.engine.FormatEngine;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.mapper.PaperFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 论文服务: 上传 / 排版 / 下载
 */
@Slf4j
@Service
public class PaperService {

    private final PaperFileMapper paperFileMapper;
    private final FormatTaskMapper taskMapper;
    private final StorageService storageService;
    private final TemplateService templateService;
    private final FormatEngine formatEngine;
    private final DiffService diffService;
    private final TaskProgressService progressService;
    private final TeamService teamService;
    // 自引用代理: 使 @Async runFormat 生效(避免自调用绕过代理)
    private final PaperService self;

    public PaperService(PaperFileMapper paperFileMapper,
                        FormatTaskMapper taskMapper,
                        StorageService storageService,
                        TemplateService templateService,
                        FormatEngine formatEngine,
                        DiffService diffService,
                        TaskProgressService progressService,
                        TeamService teamService,
                        @Lazy PaperService self) {
        this.paperFileMapper = paperFileMapper;
        this.taskMapper = taskMapper;
        this.storageService = storageService;
        this.templateService = templateService;
        this.formatEngine = formatEngine;
        this.diffService = diffService;
        this.progressService = progressService;
        this.teamService = teamService;
        this.self = self;
    }

    public PaperFile upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".docx")) {
            throw new BusinessException("仅支持 .docx 文件(旧版 .doc 请先另存为 .docx)");
        }
        if (file.getSize() > 40L * 1024 * 1024) {
            throw new BusinessException("文件过大(超过 40MB)，请拆分后重新上传");
        }
        String relative = storageService.store(file, "upload");
        PaperFile paperFile = new PaperFile();
        paperFile.setUserId(userId);
        paperFile.setOriginalName(original);
        paperFile.setStoredPath(relative);
        paperFile.setFileSize(file.getSize());
        paperFile.setCreateTime(LocalDateTime.now());
        paperFileMapper.insert(paperFile);
        return paperFile;
    }

    public FormatTask startFormat(Long userId, PaperFormatDTO dto) {
        PaperFile paperFile = paperFileMapper.selectById(dto.getFileId());
        if (paperFile == null || !paperFile.getUserId().equals(userId)) {
            throw new BusinessException(404, "论文文件不存在");
        }
        Long teamId = templateService.getOwned(dto.getTemplateId(), userId).getTeamId();

        FormatTask task = new FormatTask();
        task.setUserId(userId);
        task.setFileId(dto.getFileId());
        task.setTemplateId(dto.getTemplateId());
        task.setTeamId(teamId);
        task.setStatus(FormatTask.STATUS_PENDING);
        task.setProgress(0);
        task.setRetryCount(0);
        task.setCreateTime(LocalDateTime.now());
        taskMapper.insert(task);
        // 任务入队: 由 TaskScheduler 按并发上限从队列派发执行, 不再直接异步执行
        return task;
    }

    /** 批量排版: 为多篇论文批量创建排版任务(单次上限 50) */
    public List<FormatTask> startFormatBatch(Long userId, Long templateId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException("请选择要排版的论文");
        }
        if (fileIds.size() > 50) {
            throw new BusinessException("单次批量排版最多 50 篇");
        }
        List<FormatTask> tasks = new java.util.ArrayList<>();
        for (Long fileId : fileIds) {
            PaperFormatDTO dto = new PaperFormatDTO();
            dto.setFileId(fileId);
            dto.setTemplateId(templateId);
            tasks.add(startFormat(userId, dto));
        }
        return tasks;
    }

    /** 失败原因人性化: 把底层异常翻译成用户能理解的话 */
    private String friendlyFormatError(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            return "排版失败，请重试";
        }
        String m = msg;
        if (m.contains("文档过大") || m.contains("40MB")) {
            return m;
        }
        if (m.contains("图片") && (m.contains("无法") || m.contains("不支持") || m.contains("读取"))) {
            return "文档中存在无法读取的图片，请将图片转为 JPG/PNG 后重试";
        }
        if (m.contains("document.xml") || m.contains("XWPF") || m.contains("docx4j") || m.contains("无法解析") || m.contains("不是有效的")) {
            return "文档无法解析，请确认上传的是有效的 Word 文档(.docx)";
        }
        // 去掉包名/类名噪音, 截断过长
        m = m.replaceAll("com\\.graduate\\.thesis\\.[\\w.]+", "").replaceAll("\\s+", " ").trim();
        if (m.length() > 90) {
            m = m.substring(0, 90) + "...";
        }
        return m.isEmpty() ? "排版失败，请重试" : ("排版失败：" + m);
    }

    @Async("formatExecutor")
    public void runFormat(Long taskId) {
        FormatTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(FormatTask.STATUS_PROCESSING);
        task.setProgress(5);
        taskMapper.updateById(task);
        progressService.publish(taskId, Map.of("type", "progress", "progress", 5, "status", FormatTask.STATUS_PROCESSING,
                "stage", "prepare", "stageText", "正在准备文档"));
        try {
            PaperFile paperFile = paperFileMapper.selectById(task.getFileId());
            RuleSet ruleSet = RuleSet.from(
                    templateService.getOwned(task.getTemplateId(), task.getUserId()),
                    templateService.listRules(task.getTemplateId()));
            File source = storageService.load(paperFile.getStoredPath());
            // 超大文档保护: 超过阈值直接失败, 避免长时间占用内存/CPU
            if (source.length() > 40L * 1024 * 1024) {
                throw new BusinessException(400, "文档过大(超过 40MB)，请拆分后重新上传");
            }
            progressService.publish(taskId, Map.of("type", "progress", "progress", 10, "status", FormatTask.STATUS_PROCESSING,
                    "stage", "formatting", "stageText", "正在识别标题并应用格式规则"));
            File result = formatEngine.format(source, ruleSet, progress -> {
                task.setProgress(progress);
                taskMapper.updateById(task);
                progressService.publish(taskId, Map.of("type", "progress", "progress", progress, "status", FormatTask.STATUS_PROCESSING,
                        "stage", "formatting", "stageText", "正在应用格式规则"));
            });
            String resultPath = storageService.storeResult(task.getUserId(), result);

            task.setStatus(FormatTask.STATUS_SUCCESS);
            task.setProgress(100);
            task.setResultPath(resultPath);
            task.setFinishTime(LocalDateTime.now());
            task.setErrorMsg(null);
            taskMapper.updateById(task);
            progressService.publish(taskId, Map.of("type", "progress", "progress", 100, "status", FormatTask.STATUS_SUCCESS,
                    "stage", "done", "stageText", "排版完成"));
        } catch (Exception e) {
            log.error("排版失败 taskId={}", taskId, e);
            // 失败自动重试 1 次(非致命错误, 如临时资源/超时等), 避免用户手动重复提交
            int retry = task.getRetryCount() == null ? 0 : task.getRetryCount();
            if (retry < 1) {
                retry++;
                task.setRetryCount(retry);
                task.setStatus(FormatTask.STATUS_PENDING);
                task.setProgress(0);
                task.setErrorMsg(null);
                taskMapper.updateById(task);
                progressService.publish(taskId, Map.of("type", "progress", "progress", 0, "status", FormatTask.STATUS_PENDING, "retry", true));
                log.warn("排版失败, 第 {} 次自动重试 taskId={}, 已重置为待处理, 等待调度器重新派发", retry, taskId);
            } else {
                task.setStatus(FormatTask.STATUS_FAILED);
                task.setErrorMsg(friendlyFormatError(e.getMessage()));
                task.setFinishTime(LocalDateTime.now());
                try {
                    taskMapper.updateById(task);
                } catch (Exception ue) {
                    log.warn("写入任务失败状态失败 taskId={}", taskId, ue);
                }
                progressService.publish(taskId, Map.of("type", "progress", "status", FormatTask.STATUS_FAILED, "error", task.getErrorMsg()));
            }
        }
    }

    public FormatTask getTask(Long userId, Long taskId) {
        FormatTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        boolean mine = task.getUserId().equals(userId);
        boolean teamAccess = task.getTeamId() != null && teamService.isMember(task.getTeamId(), userId);
        if (!mine && !teamAccess) {
            throw new BusinessException(403, "无权访问该任务");
        }
        return task;
    }

    // ==================== 管理员: 任务管理 ====================

    /** 任务详情(含源文件名) */
    public FormatTask getTaskDetail(Long taskId) {
        FormatTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        PaperFile pf = paperFileMapper.selectById(task.getFileId());
        if (pf != null) {
            task.setOriginalName(pf.getOriginalName());
        }
        return task;
    }

    /** 重跑任务: 以原文件/模板新建任务 */
    public FormatTask rerun(Long taskId) {
        FormatTask old = taskMapper.selectById(taskId);
        if (old == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (FormatTask.STATUS_PROCESSING.equals(old.getStatus())) {
            throw new BusinessException(400, "任务正在执行中, 请稍后再试");
        }
        FormatTask task = new FormatTask();
        task.setUserId(old.getUserId());
        task.setFileId(old.getFileId());
        task.setTemplateId(old.getTemplateId());
        task.setTeamId(old.getTeamId());
        task.setStatus(FormatTask.STATUS_PENDING);
        task.setProgress(0);
        task.setCreateTime(LocalDateTime.now());
        taskMapper.insert(task);
        self.runFormat(task.getId());
        return task;
    }

    /** 取消任务(仅待处理/处理中) */
    public void cancel(Long taskId) {
        FormatTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (!FormatTask.STATUS_PENDING.equals(task.getStatus())
                && !FormatTask.STATUS_PROCESSING.equals(task.getStatus())) {
            throw new BusinessException(400, "仅待处理/处理中的任务可取消");
        }
        task.setStatus(FormatTask.STATUS_FAILED);
        task.setErrorMsg("任务已被管理员取消");
        task.setFinishTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    public List<FormatTask> listTasks(Long userId) {
        List<Long> teamIds = teamService.myTeamIds(userId);
        List<FormatTask> tasks;
        if (teamIds.isEmpty()) {
            tasks = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                    .eq(FormatTask::getUserId, userId)
                    .orderByDesc(FormatTask::getId));
        } else {
            tasks = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                    .and(w -> w.eq(FormatTask::getUserId, userId)
                            .or().in(FormatTask::getTeamId, teamIds))
                    .orderByDesc(FormatTask::getId));
        }
        // 批量取文件名, 避免逐任务 N+1 查询
        java.util.Set<Long> fileIds = tasks.stream().map(FormatTask::getFileId)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, String> names = new java.util.HashMap<>();
        if (!fileIds.isEmpty()) {
            for (PaperFile pf : paperFileMapper.selectBatchIds(fileIds)) {
                names.put(pf.getId(), pf.getOriginalName());
            }
        }
        for (FormatTask task : tasks) {
            task.setOriginalName(names.getOrDefault(task.getFileId(), String.valueOf(task.getFileId())));
        }
        return tasks;
    }

    public File loadResult(Long userId, Long taskId) {
        FormatTask task = getTask(userId, taskId);
        if (!FormatTask.STATUS_SUCCESS.equals(task.getStatus()) || task.getResultPath() == null) {
            throw new BusinessException(400, "任务尚未完成");
        }
        return storageService.load(task.getResultPath());
    }

    /**
     * 加载原始上传的 docx 文件(用于排版前后对比)。任务已校验团队可见, 其源文件一并可见。
     */
    public File loadOriginal(Long userId, Long taskId) {
        FormatTask task = getTask(userId, taskId);
        PaperFile paperFile = paperFileMapper.selectById(task.getFileId());
        if (paperFile == null) {
            throw new BusinessException(404, "原始文件不存在");
        }
        return storageService.load(paperFile.getStoredPath());
    }

    /**
     * 排版差异分析: 对比排版前后 docx 格式差异(纯 POI)
     */
    public List<DiffItem> listDiffs(Long userId, Long taskId) {
        FormatTask task = getTask(userId, taskId);
        if (!FormatTask.STATUS_SUCCESS.equals(task.getStatus()) || task.getResultPath() == null) {
            throw new BusinessException(400, "任务尚未完成");
        }
        PaperFile paperFile = paperFileMapper.selectById(task.getFileId());
        if (paperFile == null || !paperFile.getUserId().equals(userId)) {
            throw new BusinessException(404, "原始文件不存在");
        }
        File original = storageService.load(paperFile.getStoredPath());
        File formatted = storageService.load(task.getResultPath());
        return diffService.diff(original, formatted);
    }
}
