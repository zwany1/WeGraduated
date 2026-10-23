package com.graduate.thesis;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.entity.PaperFile;
import com.graduate.thesis.mapper.PaperFileMapper;
import com.graduate.thesis.service.PaperService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 票 01：上传入口必须拦下「扩展名是 .docx 但内容不是 Word」的文件，
 * 且拒绝时不能在存储或数据库留下任何痕迹。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PaperServiceUploadTest {

    private static final long USER_ID = 18L;

    @Autowired
    private PaperService paperService;

    @Autowired
    private PaperFileMapper paperFileMapper;

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
    void acceptsGenuineDocx() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("D:/project/Graduated/tmp_v3/edge/e1_normal.docx"));
        MockMultipartFile real = new MockMultipartFile("file", "real.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes);

        PaperFile saved = paperService.upload(USER_ID, real);
        assertNotNull(saved.getId(), "正常 docx 必须上传成功");
        assertEquals("real.docx", saved.getOriginalName());
        paperFileMapper.deleteById(saved.getId());
    }

    private long countFiles() {
        Long c = paperFileMapper.selectCount(null);
        return c == null ? 0 : c;
    }
}
