package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.PaperFormatDTO;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.service.DocxPdfService;
import com.graduate.thesis.service.PaperService;
import com.graduate.thesis.service.TaskProgressService;
import com.graduate.thesis.util.JwtUtil;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final DocxPdfService docxPdfService;
    private final TaskProgressService progressService;
    private final FormatTaskMapper taskMapper;
    private final JwtUtil jwtUtil;

    public PaperController(PaperService paperService, DocxPdfService docxPdfService,
                           TaskProgressService progressService,
                           FormatTaskMapper taskMapper, JwtUtil jwtUtil) {
        this.paperService = paperService;
        this.docxPdfService = docxPdfService;
        this.progressService = progressService;
        this.taskMapper = taskMapper;
        this.jwtUtil = jwtUtil;
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
     * 排版进度 SSE 推送(替代轮询). 浏览器 EventSource 无法带 header, 故用 query token 手动鉴权,
     * 该路径已在 WebConfig 白名单排除登录拦截器.
     */
    @GetMapping("/task/{id}/progress")
    public SseEmitter progress(@PathVariable Long id,
                               @RequestParam(value = "token", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new com.graduate.thesis.common.BusinessException(401, "未登录或登录已过期");
        }
        Long userId;
        try {
            if (jwtUtil.isRevoked(token)) {
                throw new com.graduate.thesis.common.BusinessException(401, "登录已失效，请重新登录");
            }
            userId = jwtUtil.parseUserId(token);
        } catch (com.graduate.thesis.common.BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new com.graduate.thesis.common.BusinessException(401, "未登录或登录已过期");
        }
        FormatTask task = taskMapper.selectById(id);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new com.graduate.thesis.common.BusinessException(404, "任务不存在");
        }
        return progressService.subscribe(id);
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

    /**
     * 在线预览排版结果: 返回 PDF(优先缓存, 无缓存则即时转换)
     */
    @GetMapping("/preview/{taskId}")
    public ResponseEntity<FileSystemResource> preview(@PathVariable Long taskId) {
        File pdf = paperService.loadPreviewPdf(UserContext.get(), taskId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + encode("preview.pdf"))
                .contentType(MediaType.APPLICATION_PDF)
                .body(new FileSystemResource(pdf));
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

    /**
     * 下载排版结果的 PDF 版本
     */
    @GetMapping("/download-pdf/{taskId}")
    public ResponseEntity<FileSystemResource> downloadPdf(@PathVariable Long taskId) {
        File pdf = paperService.loadPreviewPdf(UserContext.get(), taskId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encode("已排版论文.pdf"))
                .contentType(MediaType.APPLICATION_PDF)
                .body(new FileSystemResource(pdf));
    }

    private String encode(String name) {
        try {
            return URLEncoder.encode(name, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return name;
        }
    }
}
