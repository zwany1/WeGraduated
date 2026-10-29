package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.dto.ArchitectureConfig;
import com.graduate.thesis.dto.DiagramVO;
import com.graduate.thesis.dto.FlowConfig;
import com.graduate.thesis.dsl.DiagramDslService;
import com.graduate.thesis.service.ActivityDiagramRuleEngine;
import com.graduate.thesis.service.ArchitectureRuleEngine;
import com.graduate.thesis.service.ClassDiagramRuleEngine;
import com.graduate.thesis.service.DiagramGenerator;
import com.graduate.thesis.service.FlowRuleEngine;
import com.graduate.thesis.service.SequenceRuleEngine;
import com.graduate.thesis.service.SwimlaneRuleEngine;
import com.graduate.thesis.service.UseCaseRuleEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * 票 03 接线：文本进来要真的走到对应引擎，而不是被静默丢弃。
 *
 * <p>同时锁住一条约束：FLOW 不传文本时的自然语言入口必须保持原样
 * （仍然走 DiagramGenerator，行为不变）。</p>
 */
class DiagramControllerFlowArchDslTest {

    @TempDir
    Path storage;

    private DiagramController controller(DiagramGenerator generator,
                                        ArchitectureRuleEngine arch,
                                        FlowRuleEngine flow) {
        return new DiagramController(storage.toString(),
                generator,
                arch,
                Mockito.mock(SwimlaneRuleEngine.class),
                Mockito.mock(UseCaseRuleEngine.class),
                Mockito.mock(SequenceRuleEngine.class),
                Mockito.mock(ClassDiagramRuleEngine.class),
                Mockito.mock(ActivityDiagramRuleEngine.class),
                flow,
                new DiagramDslService());
    }

    private static DiagramController.GenDTO flowDsl(String dsl) {
        DiagramController.GenDTO d = new DiagramController.GenDTO();
        d.setType("FLOW");
        d.setDsl(dsl);
        return d;
    }

    @Test
    void 流程图文本解析后交给流程图引擎() {
        FlowRuleEngine flow = Mockito.mock(FlowRuleEngine.class);
        DiagramVO vo = new DiagramVO();
        vo.setType("FLOW");
        Mockito.when(flow.build(any(FlowConfig.class))).thenReturn(vo);

        Result<Object> r = controller(Mockito.mock(DiagramGenerator.class), Mockito.mock(ArchitectureRuleEngine.class), flow)
                .generate(flowDsl("开始 -> 提交论文\n提交论文 -> 格式检查\n格式检查 -> 合格?\n合格? -> 通过: 是\n合格? -> 退回修改: 否"));

        assertEquals(200, r.getCode());
        assertSame(vo, r.getData());

        ArgumentCaptor<FlowConfig> cap = ArgumentCaptor.forClass(FlowConfig.class);
        Mockito.verify(flow).build(cap.capture());
        FlowConfig cfg = cap.getValue();
        assertEquals(6, cfg.getNodes().size());
        assertTrue(cfg.getNodes().stream().anyMatch(n -> "合格?".equals(n.getName()) && "condition".equals(n.getType())),
                "判断节点应带 condition 类型");
        assertEquals(5, cfg.getEdges().size());
    }

    @Test
    void 架构图文本解析后走架构引擎() {
        ArchitectureRuleEngine arch = Mockito.mock(ArchitectureRuleEngine.class);
        DiagramVO vo = new DiagramVO();
        vo.setType("ARCH");
        Mockito.when(arch.build(any(ArchitectureConfig.class))).thenReturn(vo);

        DiagramController.GenDTO d = new DiagramController.GenDTO();
        d.setType("ARCH");
        d.setDsl("表现层\n  Vue3 前端\n服务层\n  SpringBoot\n  MySQL");

        Result<Object> r = controller(Mockito.mock(DiagramGenerator.class), arch, Mockito.mock(FlowRuleEngine.class)).generate(d);

        assertEquals(200, r.getCode());
        ArgumentCaptor<ArchitectureConfig> cap = ArgumentCaptor.forClass(ArchitectureConfig.class);
        Mockito.verify(arch).build(cap.capture());
        assertEquals(2, cap.getValue().getLayers().size());
        assertEquals(1, cap.getValue().getLayers().get(0).getComponents().size());
        assertEquals(2, cap.getValue().getLayers().get(1).getComponents().size());
    }

    @Test
    void 不传文本时流程图仍走原来的自然语言入口() {
        DiagramGenerator gen = Mockito.mock(DiagramGenerator.class);
        FlowRuleEngine flow = Mockito.mock(FlowRuleEngine.class);
        DiagramVO vo = new DiagramVO();
        vo.setType("FLOW");
        Mockito.when(gen.generate(anyString(), anyString())).thenReturn(vo);

        DiagramController.GenDTO d = new DiagramController.GenDTO();
        d.setType("FLOW");
        d.setDescription("用户登录，校验密码，生成令牌");

        Result<Object> r = controller(gen, Mockito.mock(ArchitectureRuleEngine.class), flow).generate(d);

        assertEquals(200, r.getCode());
        Mockito.verify(gen).generate("FLOW", "用户登录，校验密码，生成令牌");
        Mockito.verifyNoInteractions(flow);
    }
}
