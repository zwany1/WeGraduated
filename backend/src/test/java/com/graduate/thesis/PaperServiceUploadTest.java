package com.graduate.thesis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.PaperFormatDTO;
import com.graduate.thesis.engine.model.FormatReport;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.mapper.FormatTaskMapper;
import com.graduate.thesis.mapper.PaperFileMapper;
import com.graduate.thesis.service.PaperService;
import com.graduate.thesis.service.StorageService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 票 01：上传入口必须拦下「扩展名是 .docx 但内容不是 Word」的文件，
 * 且拒绝时不能在存储或数据库留下任何痕迹。
 *
 * <p>样本现场用 POI 生成, 不依赖本机绝对路径, 保证换机器/CI 也能跑。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PaperServiceUploadTest {

    private static final long USER_ID = 18L;

    @Autowired
    private PaperService paperService;

    @Autowired
    private PaperFileMapper paperFileMapper;

    @Autowired
    private FormatTaskMapper taskMapper;

    @Autowired
    private StorageService storageService;

    @Autowired
    private com.graduate.thesis.mapper.FormatTemplateMapper templateMapper;

    /** 现场造一个合法的 .docx（含一级标题与正文），字节以 PK\x03\x04 开头 */
    private byte[] buildDocx() throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("1 绪论");
            doc.createParagraph().createRun().setText("这是用于上传回归测试的正文段落。");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void rejectsFakeDocxAndPersistsNothing() {
        long before = countFiles();
        MockMultipartFile fake = new MockMultipartFile("file", "fake.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "这不是一个真正的 Word 文件，只是改了个扩展名。".getBytes(StandardCharsets.UTF_8));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperService.upload(USER_ID, fake));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("有效的 .docx"),
                "文案应说明文件损坏或不是有效 docx，实际: " + ex.getMessage());
        assertEquals(before, countFiles(), "被拒绝的文件不得写入 t_paper_file");
    }

    @Test
    void rejectsZeroByteFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[0]);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperService.upload(USER_ID, empty));
        assertTrue(ex.getMessage().contains("不能为空"), "实际: " + ex.getMessage());
    }

    @Test
    void rejectsEncryptedOle2ContainerWithPasswordHint() {
        // 加密的 Office 文档是 OLE2/CFB 容器, 不能报"文件损坏", 否则用户会陷入"另存为仍失败"的循环
        byte[] ole2 = new byte[64];
        ole2[0] = (byte) 0xD0;
        ole2[1] = (byte) 0xCF;
        ole2[2] = 0x11;
        ole2[3] = (byte) 0xE0;
        MockMultipartFile encrypted = new MockMultipartFile("file", "encrypted.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ole2);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperService.upload(USER_ID, encrypted));
        assertTrue(ex.getMessage().contains("加密"), "应提示文档加密, 实际: " + ex.getMessage());
    }

    @Test
    void acceptsGenuineDocx() throws Exception {
        MockMultipartFile real = new MockMultipartFile("file", "real.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", buildDocx());

        PaperFile saved = paperService.upload(USER_ID, real);
        try {
            assertNotNull(saved.getId(), "正常 docx 必须上传成功");
            assertEquals("real.docx", saved.getOriginalName());
            assertTrue(saved.getFileSize() > 0);
        } finally {
            cleanup(saved);
        }
    }

    /**
     * 接线测试: 排版完成后报告里必须带 structureLevel。
     * 若把 PaperService 里 report.evaluateStructure() 那行删掉, 本用例会失败, 防止"功能静默消失"。
     */
    @Test
    void reportCarriesStructureLevelAfterFormatting() throws Exception {
        MockMultipartFile real = new MockMultipartFile("file", "wired.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", buildDocx());
        PaperFile saved = paperService.upload(USER_ID, real);
        long taskId = -1;
        try {
            PaperFormatDTO dto = new PaperFormatDTO();
            dto.setFileId(saved.getId());
            dto.setTemplateId(firstTemplateId());
            taskId = paperService.startFormat(USER_ID, dto).getId();

            FormatTask task = waitFinish(taskId, 90_000);
            assertEquals(FormatTask.STATUS_SUCCESS, task.getStatus(), "测试文档应能排版成功");

            FormatReport report = paperService.getReport(USER_ID, taskId);
            assertNotNull(report, "排版报告不应为空");
            assertNotNull(report.getStructureLevel(),
                    "报告必须带 structureLevel; 为 null 说明 evaluateStructure() 的接线断了");
            assertEquals("OK", report.getStructureLevel(), "含一级标题的文档应评为 OK");
        } finally {
            if (taskId > 0) {
                taskMapper.deleteById(taskId);
            }
            cleanup(saved);
        }
    }

    private FormatTask waitFinish(long taskId, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        FormatTask task = null;
        while (System.currentTimeMillis() < deadline) {
            task = taskMapper.selectById(taskId);
            if (task != null && (FormatTask.STATUS_SUCCESS.equals(task.getStatus())
                    || FormatTask.STATUS_FAILED.equals(task.getStatus()))) {
                return task;
            }
            Thread.sleep(1000);
        }
        if (task == null) {
            fail("任务未创建: " + taskId);
        }
        fail("任务在 " + timeoutMillis + "ms 内未完成, 当前状态: " + task.getStatus());
        return task;
    }

    private void cleanup(PaperFile f) {
        if (f == null) {
            return;
        }
        paperFileMapper.deleteById(f.getId());
        try {
            storageService.delete(f.getStoredPath());
        } catch (Exception ignore) {
            // 清理失败不影响测试结论
        }
    }

    /** 取该用户的第一份模板, 避免把模板 id 写死 */
    private Long firstTemplateId() {
        com.graduate.thesis.entity.FormatTemplate tpl = templateMapper.selectOne(
                new LambdaQueryWrapper<com.graduate.thesis.entity.FormatTemplate>()
                        .eq(com.graduate.thesis.entity.FormatTemplate::getUserId, USER_ID)
                        .last("LIMIT 1"));
        assertNotNull(tpl, "测试用户应至少有一份格式模板（注册时自动创建）");
        return tpl.getId();
    }

    /** 只统计本用户的文件, 避免同库其它数据让断言偶发失败 */
    private long countFiles() {
        Long c = paperFileMapper.selectCount(
                new LambdaQueryWrapper<PaperFile>().eq(PaperFile::getUserId, USER_ID));
        return c == null ? 0 : c;
    }
}
