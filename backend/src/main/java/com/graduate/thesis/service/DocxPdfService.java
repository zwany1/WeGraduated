package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * docx -> PDF 转换服务(调用 LibreOffice headless 模式)
 *
 * LibreOffice 需已安装, 通过配置指定 soffice 路径
 */
@Service
public class DocxPdfService {

    private final String soffice;
    private final Path workDir;

    public DocxPdfService(@Value("${thesis.libreoffice.path:}") String soffice) {
        this.soffice = resolveSoffice(soffice);
        this.workDir = Paths.get(System.getProperty("java.io.tmpdir"), "thesis_pdf").toAbsolutePath();
        try {
            Files.createDirectories(workDir);
        } catch (Exception ignore) {
        }
    }

    private String resolveSoffice(String configured) {
        if (configured != null && !configured.trim().isEmpty()) {
            return configured.trim();
        }
        // Windows 常见路径
        File win1 = new File("C:\\Program Files\\LibreOffice\\program\\soffice.exe");
        File win2 = new File("C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe");
        if (win1.exists()) return win1.getAbsolutePath();
        if (win2.exists()) return win2.getAbsolutePath();
        // Linux/macOS 常见命令
        return "libreoffice";
    }

    /**
     * 转换 docx 为 PDF, 返回 PDF 文件
     */
    public File convert(File docx) {
        if (docx == null || !docx.exists()) {
            throw new BusinessException(404, "源文档不存在");
        }
        try {
            Path outDir = Files.createTempDirectory(workDir, "conv_");
            ProcessBuilder pb = new ProcessBuilder(
                    soffice,
                    "--headless",
                    "--norestore",
                    "--convert-to",
                    "pdf",
                    "--outdir",
                    outDir.toAbsolutePath().toString(),
                    docx.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            // 消耗输出流, 防止缓冲区阻塞
            try (java.io.InputStream is = proc.getInputStream()) {
                byte[] buf = new byte[4096];
                while (is.read(buf) != -1) {
                    // discard
                }
            }
            if (!proc.waitFor(60, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                throw new BusinessException(500, "PDF 转换超时");
            }
            String name = docx.getName();
            String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
            File pdf = new File(outDir.toFile(), base + ".pdf");
            if (!pdf.exists()) {
                // 也可能输出为原文件名.pdf
                File[] files = outDir.toFile().listFiles((d, n) -> n.endsWith(".pdf"));
                if (files != null && files.length > 0) {
                    pdf = files[0];
                }
            }
            if (!pdf.exists()) {
                throw new BusinessException(500, "PDF 转换失败: 未生成输出文件");
            }
            return pdf;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "PDF 转换失败: " + e.getMessage());
        }
    }
}
