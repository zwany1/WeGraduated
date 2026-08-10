package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.PaperFormatDTO;
import com.graduate.thesis.engine.FormatEngine;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.mapper.PaperFileMapper;
import lombok.extern.slf4j.Slf4j;
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
    private final DocxPdfService docxPdfService;

    public PaperService(PaperFileMapper paperFileMapper,
                        FormatTaskMapper taskMapper,
                        StorageService storageService,
                        TemplateService templateService,
                        FormatEngine formatEngine,
                        DocxPdfService docxPdfService) {
        this.paperFileMapper = paperFileMapper;
        this.taskMapper = taskMapper;
        this.storageService = storageService;
        this.templateService = templateService;
        this.formatEngine = formatEngine;
        this.docxPdfService = docxPdfService;
    }

    public PaperFile upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        String original = file.getOriginalFilename();
        if (original == null || (!original.toLowerCase().endsWith(".docx") && !original.toLowerCase().endsWith(".doc"))) {
            throw new BusinessException("仅支持 .docx 文件");
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
        templateService.getOwned(dto.getTemplateId(), userId);

        FormatTask task = new FormatTask();
        task.setUserId(userId);
        task.setFileId(dto.getFileId());
        task.setTemplateId(dto.getTemplateId());
        task.setStatus(FormatTask.STATUS_PENDING);
        task.setProgress(0);
        task.setCreateTime(LocalDateTime.now());
        taskMapper.insert(task);
        runFormat(task.getId());
        return task;
    }

    @Async
    public void runFormat(Long taskId) {
        FormatTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(FormatTask.STATUS_PROCESSING);
        task.setProgress(5);
        taskMapper.updateById(task);
        try {
            PaperFile paperFile = paperFileMapper.selectById(task.getFileId());
            RuleSet ruleSet = RuleSet.from(
                    templateService.getOwned(task.getTemplateId(), task.getUserId()),
                    templateService.listRules(task.getTemplateId()));
            File source = storageService.load(paperFile.getStoredPath());

            File result = formatEngine.format(source, ruleSet, progress -> {
                task.setProgress(progress);
                taskMapper.updateById(task);
            });
            String resultPath = storageService.storeResult(task.getUserId(), result);

            task.setStatus(FormatTask.STATUS_SUCCESS);
            task.setProgress(100);
            task.setResultPath(resultPath);
            task.setFinishTime(LocalDateTime.now());
            taskMapper.updateById(task);

            // 转 PDF 预览缓存(失败不影响排版结果)
            try {
                File pdf = docxPdfService.convert(result);
                String pdfPath = storageService.storePdf(task.getUserId(), pdf);
                task.setPdfPath(pdfPath);
                taskMapper.updateById(task);
            } catch (Exception pe) {
                log.warn("PDF 转换失败 taskId={}, 预览不可用", taskId, pe);
            }
        } catch (Exception e) {
            log.error("排版失败 taskId={}", taskId, e);
            task.setStatus(FormatTask.STATUS_FAILED);
            task.setErrorMsg(e.getMessage());
            task.setFinishTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    public FormatTask getTask(Long userId, Long taskId) {
        FormatTask task = taskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new BusinessException(404, "任务不存在");
        }
        return task;
    }

    public List<FormatTask> listTasks(Long userId) {
        List<FormatTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<FormatTask>()
                .eq(FormatTask::getUserId, userId)
                .orderByDesc(FormatTask::getId));
        Map<Long, String> names = new java.util.HashMap<>();
        for (FormatTask task : tasks) {
            if (!names.containsKey(task.getFileId())) {
                PaperFile pf = paperFileMapper.selectById(task.getFileId());
                names.put(task.getFileId(), pf != null ? pf.getOriginalName() : String.valueOf(task.getFileId()));
            }
            task.setOriginalName(names.get(task.getFileId()));
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
     * 加载预览 PDF: 优先缓存 pdfPath, 否则即时转换 docx->pdf(不写缓存)
     */
    public File loadPreviewPdf(Long userId, Long taskId) {
        FormatTask task = getTask(userId, taskId);
        if (!FormatTask.STATUS_SUCCESS.equals(task.getStatus()) || task.getResultPath() == null) {
            throw new BusinessException(400, "任务尚未完成");
        }
        if (task.getPdfPath() != null) {
            try {
                return storageService.load(task.getPdfPath());
            } catch (BusinessException ignore) {
                // 缓存文件丢失, 走即时转换
            }
        }
        File result = storageService.load(task.getResultPath());
        return docxPdfService.convert(result);
    }
}
