package com.graduate.thesis.engine.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 排版体检报告: 排版时由结构识别器与引擎一并产出, 随任务存储,
 * 供结果页展示"文档结构与被改动概况", 以及未匹配标题的引导修复
 */
@Data
public class FormatReport {

    /** 一级标题(章)数量 */
    private int chapterCount;

    /** 二级/三级标题数量 */
    private int sectionCount;

    /** 图题注数量 */
    private int figureCount;

    /** 表题注数量 */
    private int tableCount;

    /** 参考文献条目数量 */
    private int referenceCount;

    /** 正文段落数量(非空) */
    private int bodyParagraphs;

    /** 无编号短行被启发式自动识别为标题的数量 */
    private int autoFixedHeadings;

    /** 未匹配标题正则、已按正文处理的疑似标题行 */
    private List<Suspect> suspects = new ArrayList<>();

    /** 本次排版实际使用的关键格式参数快照(标题正则与各级字体字号), 供版本历史展示 */
    private java.util.Map<String, Object> usedConfig;

    /**
     * 疑似标题行: index 为原文档段落序号, 修复重排时用作覆盖键
     */
    @Data
    public static class Suspect {
        /** 原文档段落序号(从 0 起) */
        private int index;
        /** 段落文本 */
        private String text;
        /** 依据编号形态猜测的标题级别(1/2/3) */
        private int guessedLevel;

        public Suspect() {
        }

        public Suspect(int index, String text, int guessedLevel) {
            this.index = index;
            this.text = text;
            this.guessedLevel = guessedLevel;
        }
    }
}
