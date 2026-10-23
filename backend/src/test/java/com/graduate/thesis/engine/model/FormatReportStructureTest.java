package com.graduate.thesis.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 票 02：结构评级只在这里判定一次，前端只渲染。
 * 纯模型单测，不启 Spring、不连库。
 */
class FormatReportStructureTest {

    private FormatReport report(int chapterCount, int bodyParagraphs) {
        FormatReport r = new FormatReport();
        r.setChapterCount(chapterCount);
        r.setBodyParagraphs(bodyParagraphs);
        r.evaluateStructure();
        return r;
    }

    @Test
    void emptyDocumentIsMarkedEmpty() {
        FormatReport r = report(0, 0);
        assertEquals("EMPTY", r.getStructureLevel());
        assertNotNull(r.getStructureMessage());
        assertTrue(r.getStructureMessage().contains("正文内容"), "实际: " + r.getStructureMessage());
    }

    @Test
    void bodyWithoutHeadingsIsWarn() {
        FormatReport r = report(0, 12);
        assertEquals("WARN", r.getStructureLevel());
        assertNotNull(r.getStructureMessage());
        assertTrue(r.getStructureMessage().contains("章节标题"), "实际: " + r.getStructureMessage());
        // 必须给出可操作的原因提示
        assertTrue(r.getStructureMessage().contains("样式") || r.getStructureMessage().contains("编号"),
                "提示应包含排查方向, 实际: " + r.getStructureMessage());
    }

    @Test
    void normalDocumentIsOk() {
        FormatReport r = report(5, 120);
        assertEquals("OK", r.getStructureLevel());
        assertNull(r.getStructureMessage(), "结构正常时不应有告警文案");
    }

    @Test
    void onlyChaptersWithoutBodyStillCountsAsStructured() {
        FormatReport r = report(2, 0);
        assertEquals("OK", r.getStructureLevel(), "只有标题没有正文不算空文档");
    }
}
