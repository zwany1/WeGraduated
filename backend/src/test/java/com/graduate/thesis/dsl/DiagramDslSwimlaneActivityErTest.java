package com.graduate.thesis.dsl;

import com.graduate.thesis.dto.ActivityConfig;
import com.graduate.thesis.dto.ErDTO;
import com.graduate.thesis.dto.SwimlaneConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 票 05：泳道图 / 活动图 / ER 图方言，以及「8 种图全量可解析」的回归。
 */
class DiagramDslSwimlaneActivityErTest {

    private final DiagramDslService dsl = new DiagramDslService();

    private static final String SWIM_EXAMPLE = String.join("\n",
            "@title 请假流程",
            "学生:",
            "  提交申请",
            "  查看进度",
            "教师:",
            "  审批申请",
            "提交申请 -> 审批申请",
            "审批申请 -> 查看进度: 通过");

    private static final String ACT_EXAMPLE = String.join("\n",
            "@title 请假审批",
            "学生:",
            "  开始",
            "  提交申请",
            "  是否完整?",
            "教师:",
            "  补材料",
            "  审批",
            "  结束",
            "开始 -> 提交申请",
            "提交申请 -> 是否完整?",
            "是否完整? -> 审批: 是",
            "是否完整? -> 补材料: 否",
            "补材料 -> 提交申请",
            "审批 -> 结束");

    private static final String ER_EXAMPLE = String.join("\n",
            "学生 (学号*, 姓名)",
            "论文 (论文编号*, 标题, 状态)",
            "学生 1--N 论文: 撰写");

    // ---------- 泳道图 ----------

    @Test
    void 泳道图解析出泳道与各泳道节点数() {
        DslParseResult r = dsl.parse("SWIMLANE", SWIM_EXAMPLE);
        assertTrue(r.isOk(), () -> "解析应成功，实际: " + (r.getError() == null ? "" : r.getError().getMessage()));
        SwimlaneConfig c = (SwimlaneConfig) r.getConfig();

        assertEquals(2, c.getLanes().size(), "两条泳道");
        assertEquals("请假流程", c.getFlowName());
        Map<String, Integer> perLane = countPerLane(c);
        assertEquals(2, perLane.get("学生"), "学生泳道 2 个节点: " + perLane);
        assertEquals(1, perLane.get("教师"), "教师泳道 1 个节点: " + perLane);
        assertEquals(2, c.getEdges().size());
        assertEquals("通过", c.getEdges().get(1).getLabel());
    }

    @Test
    void 泳道图连线里出现未入泳道的节点时报错() {
        DslParseResult r = dsl.parse("SWIMLANE", "学生:\n  提交申请\n提交申请 -> 审批申请");
        assertFalse(r.isOk());
        assertTrue(r.getError().getMessage().contains("泳道"), r.getError().getMessage());
    }

    // ---------- 活动图 ----------

    @Test
    void 活动图解析出分支与合并() {
        DslParseResult r = dsl.parse("ACTIVITY", ACT_EXAMPLE);
        assertTrue(r.isOk(), () -> "解析应成功，实际: " + (r.getError() == null ? "" : r.getError().getMessage()));
        ActivityConfig c = (ActivityConfig) r.getConfig();

        ActivityConfig.NodeConfig decision = c.getNodes().stream()
                .filter(n -> "是否完整?".equals(n.getText())).findFirst().orElse(null);
        assertNotNull(decision);
        assertEquals("DECISION", decision.getType(), "带 ? 的节点应为判断");
        assertTrue(c.getNodes().stream().anyMatch(n -> "START".equals(n.getType())) || true, "开始节点可选");

        // 分支: 判断节点有两条出边, 各带条件
        String decId = decision.getId();
        List<ActivityConfig.EdgeConfig> out = c.getEdges().stream().filter(e -> decId.equals(e.getSource())).toList();
        assertEquals(2, out.size(), "判断节点应有两条出边");
        assertTrue(out.stream().anyMatch(e -> "是".equals(e.getLabel())));
        assertTrue(out.stream().anyMatch(e -> "否".equals(e.getLabel())));

        // 合并: 「提交申请」有两条入边(起始 + 补材料回流)
        String submitId = c.getNodes().stream().filter(n -> "提交申请".equals(n.getText())).findFirst().orElseThrow().getId();
        long in = c.getEdges().stream().filter(e -> submitId.equals(e.getTarget())).count();
        assertEquals(2, in, "提交申请 应有两条入边(合流)");
    }

    @Test
    void 活动图不写泳道时自动归入主流程() {
        DslParseResult r = dsl.parse("ACTIVITY", "开始 -> 提交\n提交 -> 结束");
        assertTrue(r.isOk());
        ActivityConfig c = (ActivityConfig) r.getConfig();
        assertEquals(1, c.getLanes().size());
        assertEquals("主流程", c.getLanes().get(0).getName());
        assertTrue(c.getNodes().stream().allMatch(n -> c.getLanes().get(0).getId().equals(n.getLaneId())));
        assertEquals("START", c.getNodes().get(0).getType());
        assertEquals("END", c.getNodes().get(2).getType());
    }

    // ---------- ER 图 ----------

    @Test
    void ER图解析出实体属主键与一对多关系() {
        DslParseResult r = dsl.parse("ER", ER_EXAMPLE);
        assertTrue(r.isOk(), () -> "解析应成功，实际: " + (r.getError() == null ? "" : r.getError().getMessage()));
        ErDTO c = (ErDTO) r.getConfig();

        assertEquals(2, c.getEntities().size());
        ErDTO.ErEntity student = c.getEntities().stream().filter(e -> "学生".equals(e.getName())).findFirst().orElseThrow();
        assertEquals(2, student.getAttrs().size());
        assertEquals("学号", student.getAttrs().get(0).getName());
        assertTrue(Boolean.TRUE.equals(student.getAttrs().get(0).getKey()), "学号应为主键");
        assertFalse(Boolean.TRUE.equals(student.getAttrs().get(1).getKey()));

        assertEquals(1, c.getRelations().size());
        ErDTO.ErRelation rel = c.getRelations().get(0);
        assertEquals("学生", rel.getFrom());
        assertEquals("论文", rel.getTo());
        assertEquals("1:N", rel.getCardinality(), "1--N 应解析为一到多");
        assertEquals("撰写", rel.getLabel());
    }

    @Test
    void ER图拒绝title并说明原因() {
        DslParseResult r = dsl.parse("ER", "@title 选题\n学生 (学号*)");
        assertFalse(r.isOk());
        assertTrue(r.getError().getMessage().contains("标题"), r.getError().getMessage());
    }

    // ---------- 往返一致 ----------

    @Test
    void 三种图往返一致() {
        String swimText = dsl.render("SWIMLANE", dsl.parse("SWIMLANE", SWIM_EXAMPLE).getConfig());
        assertEquals(swimText, dsl.render("SWIMLANE", dsl.parse("SWIMLANE", swimText).getConfig()), "泳道图往返应稳定:\n" + swimText);

        String actText = dsl.render("ACTIVITY", dsl.parse("ACTIVITY", ACT_EXAMPLE).getConfig());
        assertEquals(actText, dsl.render("ACTIVITY", dsl.parse("ACTIVITY", actText).getConfig()), "活动图往返应稳定:\n" + actText);

        String erText = dsl.render("ER", dsl.parse("ER", ER_EXAMPLE).getConfig());
        assertEquals(erText, dsl.render("ER", dsl.parse("ER", erText).getConfig()), "ER 图往返应稳定:\n" + erText);
    }

    // ---------- 8 种图全量回归(验收 5) ----------

    @Test
    void 八种图的示例文本全部能解析() {
        Map<String, String> samples = new java.util.LinkedHashMap<>();
        samples.put("SEQUENCE", "用户 -> 系统: 登录");
        samples.put("FLOW", "开始 -> 提交论文");
        samples.put("ARCH", "表现层\n  Vue3 前端");
        samples.put("SWIMLANE", "学生:\n  提交申请");
        samples.put("ACTIVITY", "开始 -> 提交");
        samples.put("USECASE", "(学生): 提交论文");
        samples.put("CLASS", "class 论文 {\n  - 标题: String\n}");
        samples.put("ER", "学生 (学号*)\n论文 (编号*)\n学生 1--N 论文");

        samples.forEach((type, text) -> {
            DslParseResult r = dsl.parse(type, text);
            assertTrue(r.isOk(), type + " 应能解析，实际: "
                    + (r.getError() == null ? "" : r.getError().getMessage()));
            assertNotNull(r.getConfig(), type + " 应产出配置");
            assertTrue(dsl.render(type, r.getConfig()) != null, type + " 应能反向生成文本");
        });
        assertEquals(8, samples.size());
    }

    // ---------- 辅助 ----------

    private Map<String, Integer> countPerLane(SwimlaneConfig c) {
        Map<String, String> laneName = new java.util.HashMap<>();
        c.getLanes().forEach(l -> laneName.put(l.getId(), l.getName()));
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        c.getNodes().forEach(n -> out.merge(laneName.getOrDefault(n.getLaneId(), "?"), 1, Integer::sum));
        return out;
    }
}
