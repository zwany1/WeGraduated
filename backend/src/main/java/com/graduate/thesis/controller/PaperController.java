package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.PaperFormatDTO;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.service.PaperService;
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

import javax.validation.Valid;
import java.io.File;
import java.net.URLEncoder;
import java.util.List;

/**
 * 论文接口: 上传/排版/任务/下载
 */
@RestController
@RequestMapping("/paper")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @PostMapping("/upload")
    public Result<PaperFile> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(paperService.upload(UserContext.get(), file));
    }

    @PostMapping("/format")
    public Result<FormatTask> format(@Valid @RequestBody PaperFormatDTO dto) {
        return Result.ok(paperService.startFormat(UserContext.get(), dto));
    }

    @GetMapping("/task/{id}")
    public Result<FormatTask> task(@PathVariable Long id) {
        return Result.ok(paperService.getTask(UserContext.get(), id));
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

    private String encode(String name) {
        try {
            return URLEncoder.encode(name, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return name;
        }
    }
}
