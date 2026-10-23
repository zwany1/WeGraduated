package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.dto.DiagramVO;
import com.graduate.thesis.dto.SequenceConfig;
import com.graduate.thesis.dsl.DiagramDslService;
import com.graduate.thesis.dsl.DslParseResult;
import com.graduate.thesis.service.ActivityDiagramRuleEngine;
import com.graduate.thesis.service.ArchitectureRuleEngine;
import com.graduate.thesis.service.ClassDiagramRuleEngine;
import com.graduate.thesis.service.DiagramGenerator;
import com.graduate.thesis.service.SequenceRuleEngine;
import com.graduate.thesis.service.SwimlaneRuleEngine;
import com.graduate.thesis.service.UseCaseRuleEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * 票 01：DSL 文本必须真的接进生成接口。
 *
 * <p>Service 层单测证明不了「Controller 有调用它」—— 解析层写得再对，
 * 接口不接线用户也用不到。这里锁定接线：文本进来要走解析、要走规则引擎，
 * 解析失败要返回结构化错误且不碰引擎。</p>
 */
class DiagramControllerDslTest {

    @TempDir
    Path storage;

    private DiagramController controller(SequenceRuleEngine seqEngine) {
        return new DiagramController(storage.toString(),
                Mockito.mock(DiagramGenerator.class),
                Mockito.mock(ArchitectureRuleEngine.class),
                Mockito.mock(SwimlaneRuleEngine.class),
                Mockito.mock(UseCaseRuleEngine.class),
                seqEngine,
                Mockito.mock(ClassDiagramRuleEngine.class),
                Mockito.mock(ActivityDiagramRuleEngine.class),
                new DiagramDslService());
    }

    private static DiagramController.GenDTO dto(String type, String dsl) {
        DiagramController.GenDTO d = new DiagramController.GenDTO();
        d.setType(type);
        d.setDsl(dsl);
        return d;
    }

    @Test
    void 传入合法文本时解析后交给规则引擎出图() {
        SequenceRuleEngine seq = Mockito.mock(SequenceRuleEngine.class);
        DiagramVO vo = new DiagramVO();
        vo.setType("SEQUENCE");
        Mockito.when(seq.build(any(SequenceConfig.class))).thenReturn(vo);

        Result<Object> r = controller(seq).generate(dto("SEQUENCE",
                "用户 -> 系统: 登录请求\n系统 --> 用户: 登录成功"));

        assertEquals(200, r.getCode());
        assertSame(vo, r.getData(), "应返回规则引擎产出的图");
        Mockito.verify(seq).build(any(SequenceConfig.class));
    }

    @Test
    void 文本有语法错误时返回行号且不触碰规则引擎() {
        SequenceRuleEngine seq = Mockito.mock(SequenceRuleEngine.class);

        Result<Object> r = controller(seq).generate(dto("SEQUENCE",
                "用户 -> 系统: 登录请求\n系统 >> 数据库: 查询"));

        assertEquals(400, r.getCode());
        assertTrue(r.getData() instanceof DslParseResult.DslError,
                "失败时 data 应是结构化错误，实际: " + r.getData());
        DslParseResult.DslError err = (DslParseResult.DslError) r.getData();
        assertEquals(2, err.getLine(), "行号应指向第 2 行");
        assertTrue(err.getSnippet().contains(">>"));
        Mockito.verifyNoInteractions(seq);
    }

    @Test
    void 三线表文本被拒绝并指向三线表页面() {
        Result<Object> r = controller(Mockito.mock(SequenceRuleEngine.class))
                .generate(dto("TABLE3", "表3-1 实验数据"));

        assertEquals(400, r.getCode());
        assertTrue(r.getMessage().contains("三线表"), "实际: " + r.getMessage());
    }

    @Test
    void 未开放方言的图类型提示尚未开放而非报错崩溃() {
        Result<Object> r = controller(Mockito.mock(SequenceRuleEngine.class))
                .generate(dto("CLASS", "class 论文 { }"));

        assertEquals(400, r.getCode());
        assertTrue(r.getMessage().contains("尚未开放"), "实际: " + r.getMessage());
    }

    @Test
    void 不传文本时行为与改造前一致() {
        SequenceRuleEngine seq = Mockito.mock(SequenceRuleEngine.class);
        DiagramController.GenDTO d = new DiagramController.GenDTO();
        d.setType("SEQUENCE");
        SequenceConfig cfg = new SequenceConfig();
        d.setSequence(cfg);
        DiagramVO vo = new DiagramVO();
        Mockito.when(seq.build(cfg)).thenReturn(vo);

        Result<Object> r = controller(seq).generate(d);

        assertEquals(200, r.getCode());
        assertSame(vo, r.getData(), "没传 dsl 时应完全走原结构化配置链路");
    }
}
