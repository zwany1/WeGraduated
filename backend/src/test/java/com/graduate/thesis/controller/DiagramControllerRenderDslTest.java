package com.graduate.thesis.controller;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.dsl.DiagramDslService;
import com.graduate.thesis.service.ActivityDiagramRuleEngine;
import com.graduate.thesis.service.ArchitectureRuleEngine;
import com.graduate.thesis.service.ClassDiagramRuleEngine;
import com.graduate.thesis.service.DiagramGenerator;
import com.graduate.thesis.service.ERGraphService;
import com.graduate.thesis.service.FlowRuleEngine;
import com.graduate.thesis.service.SequenceRuleEngine;
import com.graduate.thesis.service.SwimlaneRuleEngine;
import com.graduate.thesis.service.UseCaseRuleEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 票 02：画布编辑后必须能把结构化配置换回文本（R4 的前端侧依赖这个接口）。
 *
 * <p>前端发过来的是 JSON 对象，Jackson 落成 LinkedHashMap，不是 SequenceConfig。
 * 如果接口不做转换，render 会因「配置类型与图类型不匹配」直接失败 —— 这是最容易漏的一环，
 * 所以专门锁一条「用 Map 传配置」的用例。</p>
 */
class DiagramControllerRenderDslTest {

    @TempDir
    Path storage;

    private DiagramController controller() {
        return new DiagramController(storage.toString(),
                Mockito.mock(DiagramGenerator.class),
                Mockito.mock(ArchitectureRuleEngine.class),
                Mockito.mock(SwimlaneRuleEngine.class),
                Mockito.mock(UseCaseRuleEngine.class),
                Mockito.mock(SequenceRuleEngine.class),
                Mockito.mock(ClassDiagramRuleEngine.class),
                Mockito.mock(ActivityDiagramRuleEngine.class),
                Mockito.mock(FlowRuleEngine.class),
                Mockito.mock(ERGraphService.class),
                new DiagramDslService());
    }

    /** 模拟前端 JSON：participants / messages 都是普通 Map */
    private static Map<String, Object> sequenceConfigAsJson(String title, String[] names, String[][] msgs) {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("title", title);
        List<Map<String, Object>> parts = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", "p" + (i + 1));
            p.put("name", names[i]);
            parts.add(p);
        }
        cfg.put("participants", parts);
        List<Map<String, Object>> messages = new ArrayList<>();
        for (int i = 0; i < msgs.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", "m" + (i + 1));
            m.put("from", msgs[i][0]);
            m.put("to", msgs[i][1]);
            m.put("text", msgs[i][2]);
            m.put("type", msgs[i][3]);
            messages.add(m);
        }
        cfg.put("messages", messages);
        return cfg;
    }

    @Test
    @SuppressWarnings("unchecked")
    void 画布改名后用JSON配置反向生成文本含新名字() {
        Map<String, Object> cfg = sequenceConfigAsJson("登录", new String[]{"用户", "系统"}, new String[][]{
                {"p1", "p2", "提交", "request"},
                {"p2", "p1", "成功", "return"}
        });

        DiagramController.RenderDslDTO dto = new DiagramController.RenderDslDTO();
        dto.setType("SEQUENCE");
        dto.setConfig(cfg);

        Result<Object> r = controller().renderDsl(dto);

        assertEquals(200, r.getCode());
        Map<String, Object> data = (Map<String, Object>) r.getData();
        String dsl = String.valueOf(data.get("dsl"));
        assertTrue(dsl.contains("用户 -> 系统: 提交"), "实际文本:\n" + dsl);
        assertTrue(dsl.contains("系统 --> 用户: 成功"), "返回消息应保留 --> 记号, 实际:\n" + dsl);
        assertTrue(dsl.contains("@title 登录"), "标题应回填, 实际:\n" + dsl);
    }

    @Test
    void 非法图类型返回400而非500() {
        // 8 种图在票 05 已全部支持反向生成文本, 只剩下"根本不存在的类型"该被拒
        DiagramController.RenderDslDTO dto = new DiagramController.RenderDslDTO();
        dto.setType("NOT_A_DIAGRAM");
        dto.setConfig(new LinkedHashMap<>());

        BusinessException e = assertThrows(BusinessException.class, () -> controller().renderDsl(dto));
        assertEquals(400, e.getCode());
        assertTrue(e.getMessage().contains("暂不支持"), "实际: " + e.getMessage());
    }

    @Test
    void 缺少配置时给出明确报错() {
        DiagramController.RenderDslDTO dto = new DiagramController.RenderDslDTO();
        dto.setType("SEQUENCE");
        dto.setConfig(null);

        BusinessException e = assertThrows(BusinessException.class, () -> controller().renderDsl(dto));
        assertEquals(400, e.getCode());
    }
}
