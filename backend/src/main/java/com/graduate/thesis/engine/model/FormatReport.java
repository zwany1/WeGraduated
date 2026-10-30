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

    /**
     * 被自动识别为标题的段落清单(段落序号 + 文本 + 级别), 供作者回溯"改了哪几段"。
     * 历史报告 JSON 没有该字段, 反序列化后为空列表。
     */
    private List<AutoFixed> autoFixedHeadingItems = new ArrayList<>();

    /** 没有题注、因此未自动编号的图数量(避免生成只有编号没有题名的"裸题注") */
    private int uncaptionedFigures;

    /** 没有题注、因此未自动编号的表数量 */
    private int uncaptionedTables;

    /** 有题注文字但与图表不相邻(被空行/段落隔开)、已按正文保留的题注数量 */
    private int orphanCaptions;

    /** 题注编号因章节顺序/合并变化而被重新编排的数量(章号或序号与原稿不同) */
    private int renumberedCaptions;

    /**
     * 题注改号样例, 最多 5 条, 形如「图3-1 → 图5-1」。
     * 正文里的「如图3-1所示」这类交叉引用不会自动更新, 前端据此提示用户核对。
     */
    private List<String> captionRenumberSamples = new ArrayList<>();

    /**
     * 题注实际位置与模板 captionPosition(above/below) 不一致的数量。
     * 引擎只对已有题注重编号、不移动段落(移动会打乱图文关系与分页), 所以这里只做提示。
     */
    private int captionPositionMismatches;

    /** 位置不符的题注样例, 最多 5 条 */
    private List<String> captionPositionSamples = new ArrayList<>();

    /** 未匹配标题正则、已按正文处理的疑似标题行 */
    private List<Suspect> suspects = new ArrayList<>();

    /**
     * 结构评级: OK / WARN / EMPTY。
     * 历史任务的报告 JSON 里没有这个字段, 反序列化后为 null, 前端据此回退旧渲染。
     */
    private String structureLevel;

    /** 结构告警的用户可读说明; 结构正常时为 null */
    private String structureMessage;

    /** 本次排版实际使用的关键格式参数快照(标题正则与各级字体字号), 供版本历史展示 */
    private java.util.Map<String, Object> usedConfig;

    /**
     * 计算结构评级与提示文案。
     *
     * <p>这是「结构是否正常」的<b>唯一判定点</b>：前端与其它端只读取 structureLevel / structureMessage，
     * 不得各自再实现一遍条件判断。</p>
     *
     * <p>之所以必须由后端判定：空文档排版任务状态是 SUCCESS（引擎不报错），
     * 仅凭「任务是否失败」无法提示用户。</p>
     */
    public void evaluateStructure() {
        if (chapterCount == 0 && bodyParagraphs == 0) {
            structureLevel = "EMPTY";
            structureMessage = "未检测到任何正文内容：当前文档除空白段落外没有可排版的内容，"
                    + "请确认上传的是正确的论文文件（排版任务本身会显示成功，但结果是一份空白文档）";
        } else if (chapterCount == 0) {
            structureLevel = "WARN";
            structureMessage = "未识别到章节标题：正文已按普通段落排版。请确认标题使用了 Word 的"
                    + "「标题 1 / 2 / 3」样式，或写成 1 / 1.1 / 1.1.1 的编号形式";
        } else {
            structureLevel = "OK";
            structureMessage = null;
        }
    }

    /**
     * 被自动识别为标题的段落: index 为原文档段落序号
     */
    @Data
    public static class AutoFixed {
        /** 原文档段落序号(从 0 起) */
        private int index;
        /** 段落文本 */
        private String text;
        /** 识别出的标题级别(1-5) */
        private int level;

        public AutoFixed() {
        }

        public AutoFixed(int index, String text, int level) {
            this.index = index;
            this.text = text;
            this.level = level;
        }
    }

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
