package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.XComponent;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XCloseable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * docx -> PDF 转换服务
 *
 * 优先使用 LibreOffice UNO 常驻进程(Bootstrap 自动启动并复用实例), 首次启动后每次转换秒级;
 * UNO 不可用时回退命令行冷启动(--headless --convert-to)。
 */
@Service
public class DocxPdfService {

    private static final Logger log = LoggerFactory.getLogger(DocxPdfService.class);

    private final String soffice;
    private final Path workDir;

    private final Object initLock = new Object();
    private final Object convertLock = new Object();
    private XComponentLoader unoLoader;

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
        // Windows 常见路径: 优先 soffice.com(控制台版, 命令行转换稳定)
        File winCom1 = new File("C:\\Program Files\\LibreOffice\\program\\soffice.com");
        File winCom2 = new File("C:\\Program Files (x86)\\LibreOffice\\program\\soffice.com");
        if (winCom1.exists()) return winCom1.getAbsolutePath();
        if (winCom2.exists()) return winCom2.getAbsolutePath();
        File win1 = new File("C:\\Program Files\\LibreOffice\\program\\soffice.exe");
        File win2 = new File("C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe");
        if (win1.exists()) return win1.getAbsolutePath();
        if (win2.exists()) return win2.getAbsolutePath();
        // Linux/macOS 常见命令
        return "libreoffice";
    }

    /**
     * 转换 docx 为 PDF, 返回 PDF 文件。UNO 失败自动回退命令行。
     */
    public File convert(File docx) {
        if (docx == null || !docx.exists()) {
            throw new BusinessException(404, "源文档不存在");
        }
        try {
            return convertUno(docx);
        } catch (Exception e) {
            log.warn("LibreOffice UNO 转换失败, 回退命令行: {}", e.getMessage());
            return convertCli(docx);
        }
    }

    // ==================== UNO 常驻转换 ====================

    private File convertUno(File docx) throws Exception {
        XComponentLoader loader = unoLoader();
        synchronized (convertLock) {
            String inUrl = docx.toURI().toURL().toString();
            PropertyValue[] loadProps = {prop("Hidden", true)};
            XComponent doc = null;
            try {
                doc = loader.loadComponentFromURL(inUrl, "_blank", 0, loadProps);
                Path outDir = Files.createTempDirectory(workDir, "conv_");
                String base = docx.getName();
                int idx = base.lastIndexOf('.');
                if (idx > 0) base = base.substring(0, idx);
                File pdf = new File(outDir.toFile(), base + ".pdf");
                PropertyValue[] storeProps = {prop("FilterName", "writer_pdf_Export")};
                XStorable storable = UnoRuntime.queryInterface(XStorable.class, doc);
                if (storable == null) {
                    throw new BusinessException(500, "UNO 文档不支持导出");
                }
                storable.storeToURL(pdf.toURI().toURL().toString(), storeProps);
                if (!pdf.exists()) {
                    throw new BusinessException(500, "UNO 转换未生成 PDF");
                }
                return pdf;
            } finally {
                closeDoc(doc);
            }
        }
    }

    private XComponentLoader unoLoader() throws Exception {
        if (unoLoader != null) return unoLoader;
        synchronized (initLock) {
            if (unoLoader != null) return unoLoader;
            Exception last = null;
            for (int i = 0; i < 60; i++) {
                try {
                    // 自动定位/启动并连接本地 LibreOffice, 后续调用复用同一实例
                    XComponentContext ctx = Bootstrap.bootstrap();
                    Object desktop = ctx.getServiceManager()
                            .createInstanceWithContext("com.sun.star.frame.Desktop", ctx);
                    unoLoader = UnoRuntime.queryInterface(XComponentLoader.class, desktop);
                    log.info("LibreOffice UNO 常驻转换就绪");
                    return unoLoader;
                } catch (Exception e) {
                    last = e;
                    Thread.sleep(500);
                }
            }
            throw new BusinessException(500, "LibreOffice UNO 连接失败: " + (last == null ? "超时" : last.getMessage()));
        }
    }

    private void closeDoc(XComponent doc) {
        if (doc == null) return;
        try {
            XCloseable closeable = UnoRuntime.queryInterface(XCloseable.class, doc);
            if (closeable != null) {
                closeable.close(false);
                return;
            }
        } catch (Exception ignore) {
        }
        try {
            doc.dispose();
        } catch (Exception ignore) {
        }
    }

    private PropertyValue prop(String name, Object value) {
        PropertyValue pv = new PropertyValue();
        pv.Name = name;
        pv.Value = value;
        return pv;
    }

    // ==================== 命令行回退 ====================

    private File convertCli(File docx) {
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
            if (!proc.waitFor(120, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                throw new BusinessException(500, "PDF 转换超时");
            }
            String name = docx.getName();
            String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
            File pdf = new File(outDir.toFile(), base + ".pdf");
            if (!pdf.exists()) {
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

    // ==================== .doc 旧格式兼容 ====================

    /**
     * 用 LibreOffice 将 .doc 旧格式转换为 .docx(排版引擎仅支持 docx)。
     * 服务器未安装/未配置 LibreOffice 时返回明确错误。
     */
    public File convertDocToDocx(File doc) {
        if (doc == null || !doc.exists()) {
            throw new BusinessException(404, "源文档不存在");
        }
        try {
            Path outDir = Files.createTempDirectory(workDir, "doc_conv_");
            ProcessBuilder pb = new ProcessBuilder(
                    soffice,
                    "--headless",
                    "--norestore",
                    "--convert-to",
                    "docx",
                    "--outdir",
                    outDir.toAbsolutePath().toString(),
                    doc.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            try (java.io.InputStream is = proc.getInputStream()) {
                byte[] buf = new byte[4096];
                while (is.read(buf) != -1) {
                    // discard
                }
            }
            if (!proc.waitFor(120, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                throw new BusinessException(500, ".doc 转换超时");
            }
            File[] out = outDir.toFile().listFiles((d, n) -> n.endsWith(".docx"));
            if (out != null && out.length > 0) {
                return out[0];
            }
            throw new BusinessException(500, "无法将 .doc 转换为 .docx：服务器未安装或未正确配置 LibreOffice，请联系管理员");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, ".doc 转换为 .docx 失败: " + e.getMessage());
        }
    }
}
