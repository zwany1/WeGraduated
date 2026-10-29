package com.graduate.thesis.dsl;

import com.graduate.thesis.dto.ArchitectureConfig;
import com.graduate.thesis.dto.FlowConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 票 03：流程图与架构图方言。
 *
 * <p>验收标准里 FLOW 那条写「产出 5 个节点」，但示例文本里出现的是
 * 开始 / 提交论文 / 格式检查 / 合格? / 通过 / 退回修改 共 6 个不同名字 ——
 * 按「一个名字一个节点」的语义只能是 6。这里按语义断言 6，并在票单里标注了这处不一致。</p>
 */
class DiagramDslFlowArchTest {

    private final DiagramDslService dsl = new DiagramDslService();

    private static final String FLOW_EXAMPLE = String.join("\n",
            "开始 -> 提交论文",
            "提交论文 -> 格式检查",
            "格式检查 -> 合格?",
            "合格? -> 通过: 是",
            "合格? -> 退回修改: 否");

    private static final String ARCH_EXAMPLE = String.join("\n",
            "表现层",
            "  Vue3 前端",
            "服务层",
            "  SpringBoot",
            "  MySQL");

    @Test
    void 流程图示例产出节点类型与分支标签正确() {
        DslParseResult r = dsl.parse("FLOW", FLOW_EXAMPLE);
        assertTrue(r.isOk(), () -> "解析应成功，实际: " + (r.getError() == null ? "" : r.getError().getMessage()));

        FlowConfig c = (FlowConfig) r.getConfig();
        List<FlowConfig.Node> nodes = c.getNodes();
        assertEquals(6, nodes.size(), "6 个不同名字就是 6 个节点: " + names(nodes));

        assertEquals("start", typeOf(nodes, "开始"));
        assertEquals("action", typeOf(nodes, "提交论文"));
        assertEquals("action", typeOf(nodes, "格式检查"));
        assertEquals("condition", typeOf(nodes, "合格?"), "名字带 ? 应识别为判断");
        assertEquals("action", typeOf(nodes, "通过"));
        assertEquals("action", typeOf(nodes, "退回修改"));

        assertEquals(5, c.getEdges().size());
        assertEquals("是", labelOf(c, "合格?", "通过"));
        assertEquals("否", labelOf(c, "合格?", "退回修改"));
    }

    @Test
    void 流程图方括号写法也识别为判断() {
        DslParseResult r = dsl.parse("FLOW", "开始 -> [余额够?]\n[余额够?] -> 扣款");
        assertTrue(r.isOk());
        FlowConfig c = (FlowConfig) r.getConfig();
        assertEquals("condition", typeOf(c.getNodes(), "余额够?"), "方括号里的名字也是判断");
    }

    @Test
    void 流程图缺少连接符时给出行号与片段() {
        DslParseResult r = dsl.parse("FLOW", "开始 -> 提交\n提交 > 审核");
        assertFalse(r.isOk());
        assertEquals(2, r.getError().getLine());
        assertTrue(r.getError().getSnippet().contains("提交"));
        assertTrue(r.getError().getHint().contains("流程图语法"));
    }

    @Test
    void 流程图往返一致() {
        FlowConfig c = (FlowConfig) dsl.parse("FLOW", FLOW_EXAMPLE).getConfig();
        String text = dsl.render("FLOW", c);
        FlowConfig again = (FlowConfig) dsl.parse("FLOW", text).getConfig();

        assertEquals(names(c.getNodes()), names(again.getNodes()), "节点名与顺序应一致, 文本:\n" + text);
        assertEquals(c.getEdges().size(), again.getEdges().size());
        c.getNodes().forEach(n -> assertEquals(n.getType(), typeOf(again.getNodes(), n.getName())));
        for (FlowConfig.Edge e : c.getEdges()) {
            String expected = e.getLabel() == null ? "" : e.getLabel();
            assertEquals(expected, labelOf(again, nameOf(c, e.getFrom()), nameOf(c, e.getTo())));
        }
    }

    @Test
    void 架构图缩进表示层级() {
        DslParseResult r = dsl.parse("ARCH", ARCH_EXAMPLE);
        assertTrue(r.isOk(), () -> "解析应成功，实际: " + (r.getError() == null ? "" : r.getError().getMessage()));
        ArchitectureConfig c = (ArchitectureConfig) r.getConfig();
        assertEquals(2, c.getLayers().size());
        assertEquals("表现层", c.getLayers().get(0).getName());
        assertEquals(1, c.getLayers().get(0).getComponents().size());
        assertEquals("Vue3 前端", c.getLayers().get(0).getComponents().get(0).getName());
        assertEquals("服务层", c.getLayers().get(1).getName());
        assertEquals(2, c.getLayers().get(1).getComponents().size());
        assertEquals("SpringBoot", c.getLayers().get(1).getComponents().get(0).getName());
        assertEquals("MySQL", c.getLayers().get(1).getComponents().get(1).getName());
    }

    @Test
    void 架构图同层组件缩进不一致时报错并指到该行() {
        String bad = String.join("\n",
                "表现层",
                "  Vue3 前端",
                "服务层",
                "   SpringBoot",
                "  MySQL");
        DslParseResult r = dsl.parse("ARCH", bad);
        assertFalse(r.isOk());
        // 本层的第一个组件(第 4 行, 3 格)定义了该层缩进, 第 5 行的 2 格才是"不一致"的那行
        assertEquals(5, r.getError().getLine(), "应指到与同层首个组件缩进不同的那一行");
        assertTrue(r.getError().getHint().contains("缩进"), "提示应说明缩进问题: " + r.getError().getHint());
    }

    @Test
    void 架构图组件没有归属层时报错() {
        DslParseResult r = dsl.parse("ARCH", "  Vue3 前端\n表现层");
        assertFalse(r.isOk());
        assertEquals(1, r.getError().getLine());
        assertTrue(r.getError().getMessage().contains("层"), r.getError().getMessage());
    }

    @Test
    void 架构图往返一致() {
        ArchitectureConfig c = (ArchitectureConfig) dsl.parse("ARCH", "@title 会员系统\n" + ARCH_EXAMPLE).getConfig();
        String text = dsl.render("ARCH", c);
        ArchitectureConfig again = (ArchitectureConfig) dsl.parse("ARCH", text).getConfig();

        assertEquals("会员系统", again.getSystemName());
        assertEquals(c.getLayers().size(), again.getLayers().size());
        for (int i = 0; i < c.getLayers().size(); i++) {
            assertEquals(c.getLayers().get(i).getName(), again.getLayers().get(i).getName());
            assertEquals(
                    c.getLayers().get(i).getComponents().stream().map(ArchitectureConfig.ComponentConfig::getName).toList(),
                    again.getLayers().get(i).getComponents().stream().map(ArchitectureConfig.ComponentConfig::getName).toList(),
                    "第 " + (i + 1) + " 层组件应一致, 文本:\n" + text);
        }
    }

    // ---------- 断言辅助 ----------

    private List<String> names(List<FlowConfig.Node> nodes) {
        return nodes.stream().map(FlowConfig.Node::getName).toList();
    }

    private String typeOf(List<FlowConfig.Node> nodes, String name) {
        return nodes.stream().filter(n -> n.getName().equals(name)).findFirst()
                .map(FlowConfig.Node::getType).orElse("<缺失:" + name + ">");
    }

    private String nameOf(FlowConfig c, String id) {
        return c.getNodes().stream().filter(n -> n.getId().equals(id)).findFirst()
                .map(FlowConfig.Node::getName).orElse(String.valueOf(id));
    }

    private String labelOf(FlowConfig c, String from, String to) {
        String fid = c.getNodes().stream().filter(n -> n.getName().equals(from)).findFirst().map(FlowConfig.Node::getId).orElse("");
        String tid = c.getNodes().stream().filter(n -> n.getName().equals(to)).findFirst().map(FlowConfig.Node::getId).orElse("");
        // 注意: Stream.findFirst() 遇到 null 元素会抛 NPE, 无标签边的 label 就是 null
        return c.getEdges().stream()
                .filter(e -> fid.equals(e.getFrom()) && tid.equals(e.getTo()))
                .findFirst().map(e -> e.getLabel() == null ? "" : e.getLabel())
                .orElse("<无此边:" + from + "->" + to + ">");
    }
}
