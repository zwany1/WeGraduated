package com.graduate.thesis.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 疑似标题级别判定: 报告收集与引导修复的数据基础
 */
class StructureDetectorSuspectTest {

    @Test
    void 章级形态识别为一级() {
        Assertions.assertEquals(1, StructureDetector.suspectHeadingLevel("第二章 系统设计"));
        Assertions.assertEquals(1, StructureDetector.suspectHeadingLevel("第12章"));
        Assertions.assertEquals(1, StructureDetector.suspectHeadingLevel("三、测试方法"));
    }

    @Test
    void 多级数字编号按点数定级() {
        Assertions.assertEquals(2, StructureDetector.suspectHeadingLevel("1.1 研究背景"));
        Assertions.assertEquals(3, StructureDetector.suspectHeadingLevel("2.3.1 总体架构"));
        Assertions.assertEquals(5, StructureDetector.suspectHeadingLevel("1.2.3.4.5 细节"));
    }

    @Test
    void 正文句子与空值不误判() {
        Assertions.assertEquals(0, StructureDetector.suspectHeadingLevel(""));
        Assertions.assertEquals(0, StructureDetector.suspectHeadingLevel(null));
        Assertions.assertEquals(0, StructureDetector.suspectHeadingLevel("1.1 研究背景。本文主要讨论"));
        Assertions.assertEquals(0, StructureDetector.suspectHeadingLevel("通过实验验证了系统性能。"));
        Assertions.assertEquals(0, StructureDetector.suspectHeadingLevel("这是一段很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长很长的正文内容不能是标题"));
    }
}
