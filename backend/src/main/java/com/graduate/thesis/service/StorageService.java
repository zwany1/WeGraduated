package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储服务(本地磁盘)
 */
@Service
public class StorageService {

    private final Path root;

    public StorageService(@Value("${thesis.storage.dir}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    /**
     * 保存文件, 返回相对路径
     */
    public String store(MultipartFile file, String category) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String ext = extension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        String relative = category + "/" + date + "/" + filename;
        try {
            Path target = root.resolve(relative).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("非法路径");
            }
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            return relative;
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败: " + e.getMessage());
        }
    }

    /**
     * 根据相对路径取文件
     */
    public File load(String relativePath) {
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root) || !Files.exists(target)) {
            throw new BusinessException(404, "文件不存在");
        }
        return target.toFile();
    }

    /**
     * 保存排版结果文件, 返回相对路径
     */
    public String storeResult(Long userId, File file) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String ext = extension(file.getName());
        String filename = "result_" + userId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ext;
        String relative = "result/" + date + "/" + filename;
        return storeCopy(relative, file);
    }

    /**
     * 保存预览 PDF, 返回相对路径
     */
    public String storePdf(Long userId, File pdf) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "preview_" + userId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ".pdf";
        String relative = "result/" + date + "/" + filename;
        return storeCopy(relative, pdf);
    }

    private String storeCopy(String relative, File file) {
        try {
            Path target = root.resolve(relative).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("非法路径");
            }
            Files.createDirectories(target.getParent());
            Files.copy(file.toPath(), target);
            return relative;
        } catch (IOException e) {
            throw new BusinessException(500, "结果保存失败: " + e.getMessage());
        }
    }

    public String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
