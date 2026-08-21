package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 数据库备份: 调用 mysqldump 导出 SQL, 支持手动/每日定时备份, 列表/下载/删除.
 */
@Slf4j
@Service
public class BackupService {

    private final String jdbcUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final Path backupDir;
    /** 最多保留备份份数 */
    private final int maxKeep;

    public BackupService(@Value("${spring.datasource.url}") String jdbcUrl,
                         @Value("${spring.datasource.username}") String dbUsername,
                         @Value("${spring.datasource.password}") String dbPassword,
                         @Value("${thesis.storage.dir}") String storageDir,
                         @Value("${thesis.backup.max-keep:7}") int maxKeep) {
        this.jdbcUrl = jdbcUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.maxKeep = Math.max(1, maxKeep);
        this.backupDir = Paths.get(storageDir).toAbsolutePath().normalize().resolve("backup");
    }

    /** 每日凌晨 2 点自动备份 */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 2 * * ?")
    public void scheduledBackup() {
        try {
            backupNow();
        } catch (Exception e) {
            log.warn("[Backup] 定时备份失败: {}", e.getMessage());
        }
    }

    /** 立即执行一次备份, 返回备份文件名 */
    public String backupNow() {
        try {
            DbInfo info = parseDbInfo(jdbcUrl);
            Files.createDirectories(backupDir);
            String filename = "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
            File target = backupDir.resolve(filename).toFile();

            List<String> cmd = new ArrayList<>();
            cmd.add(mysqldumpPath());
            cmd.add("-h"); cmd.add(info.host);
            cmd.add("-P"); cmd.add(info.port);
            cmd.add("-u"); cmd.add(dbUsername);
            cmd.add("--password=" + dbPassword);
            cmd.add("--single-transaction");
            cmd.add("--default-character-set=utf8mb4");
            cmd.add(info.database);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);
            Process process = pb.start();
            try (java.io.InputStream is = process.getInputStream();
                 java.io.OutputStream os = new java.io.FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    os.write(buf, 0, n);
                }
            }
            int exit = process.waitFor();
            if (exit != 0 || target.length() == 0) {
                Files.deleteIfExists(target.toPath());
                throw new BusinessException(500, "备份失败(mysqldump 退出码 " + exit + ")，请确认服务器已安装并配置 mysqldump");
            }
            cleanupOld();
            log.info("[Backup] 数据库备份完成: {}", filename);
            return filename;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Backup] 备份失败", e);
            throw new BusinessException(500, "备份失败: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> listBackups() {
        List<Map<String, Object>> result = new ArrayList<>();
        File[] files = backupDir.toFile().listFiles((d, n) -> n.endsWith(".sql"));
        if (files == null) {
            return result;
        }
        java.util.Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (File f : files) {
            result.add(Map.of(
                    "name", f.getName(),
                    "size", f.length(),
                    "time", LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(f.lastModified()),
                            java.time.ZoneId.systemDefault())
            ));
        }
        return result;
    }

    public File loadBackup(String filename) {
        Path p = backupDir.resolve(filename).normalize();
        if (!p.startsWith(backupDir) || !Files.exists(p)) {
            throw new BusinessException(404, "备份文件不存在");
        }
        return p.toFile();
    }

    public void deleteBackup(String filename) {
        Path p = backupDir.resolve(filename).normalize();
        if (!p.startsWith(backupDir) || !Files.exists(p)) {
            throw new BusinessException(404, "备份文件不存在");
        }
        try {
            Files.deleteIfExists(p);
        } catch (Exception e) {
            throw new BusinessException(500, "删除备份失败");
        }
    }

    /** 恢复备份: 调用 mysql 客户端导入 sql, 高危操作 */
    public void restore(String filename) {
        File backupFile = loadBackup(filename);
        try {
            DbInfo info = parseDbInfo(jdbcUrl);
            List<String> cmd = new ArrayList<>();
            cmd.add("mysql");
            cmd.add("-h"); cmd.add(info.host);
            cmd.add("-P"); cmd.add(info.port);
            cmd.add("-u"); cmd.add(dbUsername);
            cmd.add("--password=" + dbPassword);
            cmd.add("--default-character-set=utf8mb4");
            cmd.add(info.database);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);
            Process process = pb.start();
            try (java.io.InputStream is = new java.io.FileInputStream(backupFile);
                 java.io.OutputStream os = process.getOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    os.write(buf, 0, n);
                }
                os.flush();
            }
            int exit = process.waitFor();
            if (exit != 0) {
                throw new BusinessException(500, "恢复失败(mysql 退出码 " + exit + ")，请确认服务器已安装 mysql 客户端");
            }
            log.info("[Backup] 数据库恢复完成: {}", filename);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Backup] 恢复失败", e);
            throw new BusinessException(500, "恢复失败: " + e.getMessage());
        }
    }

    /** 保留最近 maxKeep 份, 删除更旧的(每次备份后清理, 实现定期清理) */
    private void cleanupOld() {
        File[] files = backupDir.toFile().listFiles((d, n) -> n.endsWith(".sql"));
        if (files == null || files.length <= maxKeep) {
            return;
        }
        java.util.Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i < files.length - maxKeep; i++) {
            try {
                Files.deleteIfExists(files[i].toPath());
            } catch (Exception ignore) {
            }
        }
    }

    private String mysqldumpPath() {
        // 优先用环境/系统 PATH 中的 mysqldump, 兼容 Windows 常见安装路径
        return "mysqldump";
    }

    private DbInfo parseDbInfo(String url) {
        try {
            String sub = url.substring(url.indexOf("://") + 3);
            int slash = sub.indexOf('/');
            String hostPort = slash < 0 ? sub : sub.substring(0, slash);
            String rest = slash < 0 ? "" : sub.substring(slash + 1);
            String db = rest.contains("?") ? rest.substring(0, rest.indexOf('?')) : rest;
            db = URLDecoder.decode(db, StandardCharsets.UTF_8.name());
            String host = hostPort;
            String port = "3306";
            if (hostPort.contains(":")) {
                int c = hostPort.indexOf(':');
                host = hostPort.substring(0, c);
                port = hostPort.substring(c + 1);
            }
            DbInfo info = new DbInfo();
            info.host = host;
            info.port = port;
            info.database = db;
            return info;
        } catch (Exception e) {
            throw new BusinessException(500, "数据库连接配置解析失败");
        }
    }

    private static class DbInfo {
        String host;
        String port;
        String database;
    }
}
