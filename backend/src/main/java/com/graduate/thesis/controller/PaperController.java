package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.PaperFormatDTO;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.service.PaperService;
import com.graduate.thesis.service.TaskProgressService;
import com.graduate.thesis.service.ProgressTicketService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.io.File;
import java.net.URLEncoder;
import java.util.List;

/**
 * 论文接口: 上传/排版/任务/下载/预览
 */
@RestController
@RequestMapping("/paper")
public class PaperController {

    private final PaperService paperService;
    private final TaskProgressService progressService;
    private final FormatTaskMapper taskMapper;
    private final ProgressTicketService ticketService;

    public PaperController(PaperService paperService,
                           TaskProgressService progressService,
                           FormatTaskMapper taskMapper, ProgressTicketService ticketService) {
        this.paperService = paperService;
        this.progressService = progressService;
        this.taskMapper = taskMapper;
        this.ticketService = ticketService;
    }

    @PostMapping("/upload")
    public Result<PaperFile> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(paperService.upload(UserContext.get(), file));
    }

    @PostMapping("/format")
    public Result<FormatTask> format(@Valid @RequestBody PaperFormatDTO dto) {
        return Result.ok(paperService.startFormat(UserContext.get(), dto));
    }

    /** 批量排版: 一次为多篇论文创建任务 */
    @PostMapping("/format-batch")
    public Result<List<FormatTask>> formatBatch(@Valid @RequestBody com.graduate.thesis.dto.PaperFormatBatchDTO dto) {
        return Result.ok(paperService.startFormatBatch(UserContext.get(), dto.getTemplateId(), dto.getFileIds()));
    }

    @GetMapping("/task/{id}")
    public Result<FormatTask> task(@PathVariable Long id) {
        return Result.ok(paperService.getTask(UserContext.get(), id));
    }

    /**
     * 排版进度 SSE 推送(替代轮询). 该路径已排除登录拦截器, 用一次性票据手动鉴权.
     */
    @GetMapping("/task/{id}/progress")
    public SseEmitter progress(@PathVariable Long id,
                               @RequestParam(value = "ticket", required = false) String ticket) {
        if (!ticketService.consume(ticket, id)) {
            throw new com.graduate.thesis.common.BusinessException(401, "凭据已失效，请重新加载任务");
        }
        return progressService.subscribe(id);
    }

    /** 签发排版进度 SSE 的一次性票据(走登录拦截器, 校验任务归属) */
    @PostMapping("/task/{id}/progress-ticket")
    public Result<java.util.Map<String, String>> progressTicket(@PathVariable Long id) {
        Long userId = UserContext.get();
        FormatTask task = taskMapper.selectById(id);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new com.graduate.thesis.common.BusinessException(404, "任务不存在");
        }
        return Result.ok(java.util.Collections.singletonMap("ticket", ticketService.generate(userId, id)));
    }

    /**
     * 排版差异分析: 返回差异段落列表(含 PDF 页码与坐标)
     */
    @GetMapping("/diff/{taskId}")
    public Result<List<com.graduate.thesis.dto.DiffItem>> diff(@PathVariable Long taskId) {
        return Result.ok(paperService.listDiffs(UserContext.get(), taskId));
    }

    @GetMapping("/tasks")
    public Result<List<FormatTask>> tasks() {
        return Result.ok(paperService.listTasks(UserContext.get()));
    }

    @GetMapping("/download/{taskId}")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long taskId) {
        File file = paperService.loadResult(UserContext.get(), taskId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encode("已排版论文.docx"))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }

    /** 批量下载多个已排版任务的结果, 打包成 zip */
    @PostMapping("/download-batch")
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> downloadBatch(
            @RequestBody java.util.Map<String, java.util.List<Long>> body) {
        java.util.List<Long> ids = body.get("taskIds");
        java.util.List<FormatTask> tasks = paperService.loadResultsBatch(UserContext.get(), ids);
        org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody srb = out -> {
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(out)) {
                java.util.Set<String> used = new java.util.HashSet<>();
                for (FormatTask t : tasks) {
                    File f = paperService.resultFileOf(t);
                    String base = t.getOriginalName() == null ? ("task_" + t.getId()) : t.getOriginalName();
                    base = base.replaceAll("[\\\\/:*?\"<>|]", "_");
                    if (!base.toLowerCase().endsWith(".docx")) {
                        base += ".docx";
                    }
                    String name = base;
                    int n = 2;
                    while (used.contains(name)) {
                        name = base.substring(0, base.lastIndexOf('.')) + "_" + n + ".docx";
                        n++;
                    }
                    used.add(name);
                    zos.putNextEntry(new java.util.zip.ZipEntry(name));
                    java.nio.file.Files.copy(f.toPath(), zos);
                    zos.closeEntry();
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encode("已排版论文批量.zip"))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(srb);
    }

    /**
     * 原始上传文档(排版前 docx), 用于前后对比
     */
    @GetMapping("/download-original/{taskId}")
    public ResponseEntity<FileSystemResource> downloadOriginal(@PathVariable Long taskId) {
        File file = paperService.loadOriginal(UserContext.get(), taskId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + encode("original.docx"))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }

    /** 删除排版任务: 级联清理结果文件, 原文档无其他任务引用时一并删除 */
    @DeleteMapping("/task/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        paperService.deleteTask(UserContext.get(), id);
        return Result.ok();
    }

    /** 我的上传文档列表(含关联任务数) */
    @GetMapping("/files")
    public Result<List<PaperFile>> files() {
        return Result.ok(paperService.listFiles(UserContext.get()));
    }

    /** 删除上传文档: 级联删其所有关联任务(含结果文件) */
    @DeleteMapping("/file/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        paperService.deleteFile(UserContext.get(), id);
        return Result.ok();
    }

    private String encode(String name) {
        try {
            return URLEncoder.encode(name, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return name;
        }
    }
}
