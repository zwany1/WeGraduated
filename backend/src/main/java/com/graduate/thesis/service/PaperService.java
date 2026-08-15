package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.DiffItem;
import com.graduate.thesis.dto.PaperFormatDTO;
import com.graduate.thesis.engine.DocItem;
import com.graduate.thesis.engine.FormatEngine;
import com.graduate.thesis.engine.ParagraphKind;
import com.graduate.thesis.engine.StructureDetector;
import com.graduate.thesis.engine.model.RuleSet;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.mapper.PaperFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
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
    private final DiffService diffService;
    private final TaskProgressService progressService;
    // 自引用代理: 使 @Async runFormat 生效(避免自调用绕过代理)
    private final PaperService self;

    public PaperService(PaperFileMapper paperFileMapper,
                        FormatTaskMapper taskMapper,
                        StorageService storageService,
                        TemplateService templateService,
                        FormatEngine formatEngine,
                        DocxPdfService docxPdfService,
                        DiffService diffService,
                        TaskProgressService progressService,
                        @Lazy PaperService self) {
        this.paperFileMapper = paperFileMapper;
        this.taskMapper = taskMapper;
        this.storageService = storageService;
        this.templateService = templateService;
        this.formatEngine = formatEngine;
        this.docxPdfService = docxPdfService;
        this.diffService = diffService;
        this.progressService = progressService;
        this.self = self;
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
        self.runFormat(task.getId());
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
        progressService.publish(taskId, Map.of("type", "progress", "progress", 5, "status", FormatTask.STATUS_PROCESSING));
        try {
            PaperFile paperFile = paperFileMapper.selectById(task.getFileId());
            RuleSet ruleSet = RuleSet.from(
                    templateService.getOwned(task.getTemplateId(), task.getUserId()),
                    templateService.listRules(task.getTemplateId()));
            File source = storageService.load(paperFile.getStoredPath());

            File result = formatEngine.format(source, ruleSet, progress -> {
                task.setProgress(progress);
                taskMapper.updateById(task);
                progressService.publish(taskId, Map.of("type", "progress", "progress", progress, "status", FormatTask.STATUS_PROCESSING));
            });
            String resultPath = storageService.storeResult(task.getUserId(), result);

            task.setStatus(FormatTask.STATUS_SUCCESS);
            task.setProgress(100);
            task.setResultPath(resultPath);
            task.setFinishTime(LocalDateTime.now());
            task.setErrorMsg(null);
            task.setSummary(buildSummary(source, ruleSet));
            taskMapper.updateById(task);
            progressService.publish(taskId, Map.of("type", "progress", "progress", 100, "status", FormatTask.STATUS_SUCCESS, "summary", task.getSummary()));

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
                log.warn("排版失败, 第 {} 次自动重试 taskId={}", retry, taskId);
                self.runFormat(taskId);
            } else {
                task.setStatus(FormatTask.STATUS_FAILED);
                String em = e.getMessage();
                task.setErrorMsg(em == null ? null : (em.length() > 2000 ? em.substring(0, 2000) : em));
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

    /**
     * 排版校验摘要: 统计源文档中被识别/待排版的结构(标题/正文/图表题注), 供前端展示排版覆盖情况.
     */
    private String buildSummary(File source, RuleSet ruleSet) {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(source))) {
            List<DocItem> items = new StructureDetector().detect(doc, ruleSet);
            long h1 = items.stream().filter(i -> i.getKind() == ParagraphKind.HEADING1 && !i.isFrontMatter()).count();
            long h2 = items.stream().filter(i -> i.getKind() == ParagraphKind.HEADING2 && !i.isFrontMatter()).count();
            long h3 = items.stream().filter(i -> i.getKind() == ParagraphKind.HEADING3 && !i.isFrontMatter()).count();
            long body = items.stream().filter(i -> i.getKind() == ParagraphKind.BODY && !i.isFrontMatter()
                    && !i.getText().trim().isEmpty()).count();
            long caption = items.stream().filter(i -> (i.getKind() == ParagraphKind.FIGURE_CAPTION
                    || i.getKind() == ParagraphKind.TABLE_CAPTION || i.getKind() == ParagraphKind.IMAGE)
                    && !i.isFrontMatter()).count();
            long front = items.stream().filter(DocItem::isFrontMatter).count();
            return "一级标题" + h1 + "个、二级标题" + h2 + "个、三级标题" + h3 + "个、正文段落" + body + "个、图表题注" + caption
                    + "个；第一章前内容保留 " + front + " 段";
        } catch (Exception e) {
            return null;
        }
    }

    public FormatTask getTask(Long userId, Long taskId) {
        FormatTask task = taskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new BusinessException(404, "任务不存在");
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
     * 加载原始上传的 docx 文件(用于排版前后对比)
     */
    public File loadOriginal(Long userId, Long taskId) {
        FormatTask task = getTask(userId, taskId);
        PaperFile paperFile = paperFileMapper.selectById(task.getFileId());
        if (paperFile == null || !paperFile.getUserId().equals(userId)) {
            throw new BusinessException(404, "原始文件不存在");
        }
        return storageService.load(paperFile.getStoredPath());
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
        File pdf = docxPdfService.convert(result);
        // 即时转换结果写入缓存, 下次预览直接读取(首次转换较慢, 之后秒开)
        try {
            String pdfPath = storageService.storePdf(task.getUserId(), pdf);
            task.setPdfPath(pdfPath);
            taskMapper.updateById(task);
        } catch (Exception ignore) {
            // 缓存写入失败不影响本次预览
        }
        return pdf;
    }

    /**
     * 排版差异分析: 对比排版前后 docx 格式差异(纯 POI, 不依赖 LibreOffice)
     * 有缓存 PDF 时附页码坐标(增强), 无缓存/转换不可用时差异仍正常返回, 前端走文本定位
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
        // 仅使用已缓存的 PDF(不触发即时转换), 无缓存则差异仅文本定位
        File pdf = null;
        if (task.getPdfPath() != null) {
            try {
                pdf = storageService.load(task.getPdfPath());
            } catch (Exception ignore) {
                pdf = null;
            }
        }
        return diffService.diff(original, formatted, pdf);
    }
}
