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
     * 删除文件(不存在时静默)
     */
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return;
        }
        try {
            Path target = root.resolve(relativePath).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignore) {
            // 删除失败不影响账号注销主流程
        }
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

    /**
     * 清理孤立派生文件(未被任何任务/文件引用, 且超过 keepDays 天).
     * 仅清理 upload / result 两类目录下的文件, 不触碰引用中的文件.
     * @return 删除的文件数
     */
    public int cleanupOrphans(java.util.Set<String> referenced, int keepDays) {
        int deleted = 0;
        long cutoff = System.currentTimeMillis() - keepDays * 24L * 3600 * 1000L;
        for (String category : new String[]{"upload", "result"}) {
            Path dir = root.resolve(category);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (java.util.stream.Stream<Path> stream = Files.walk(dir)) {
                for (Path p : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                    try {
                        String relative = category + "/" + root.relativize(p).toString().replace('\\', '/');
                        if (referenced.contains(relative)) {
                            continue;
                        }
                        if (Files.getLastModifiedTime(p).toMillis() < cutoff) {
                            Files.deleteIfExists(p);
                            deleted++;
                        }
                    } catch (Exception ignore) {
                    }
                }
            } catch (IOException ignore) {
            }
        }
        return deleted;
    }
}
