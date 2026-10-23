package com.graduate.thesis.dsl;

import com.graduate.thesis.dto.SequenceConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 票 01：系统图文本 DSL（时序图打通全链路）。
 *
 * <p>纯模型单测，不启 Spring、不连库 —— 与仓库既有测试风格一致。</p>
 */
class DiagramDslServiceTest {

    private final DiagramDslService svc = new DiagramDslService();

    private static final String SEQ_TEXT = String.join("\n",
            "用户 -> 系统: 登录请求",
            "系统 -> 数据库: 查询账号",
            "数据库 --> 系统: 返回结果",
            "系统 --> 用户: 登录成功");

    private SequenceConfig parsed(String text) {
        DslParseResult r = svc.parse("SEQUENCE", text);
        assertTrue(r.isOk(), "应解析成功，实际错误: "
                + (r.getError() == null ? "无" : r.getError().getMessage()));
        return (SequenceConfig) r.getConfig();
    }

    @Test
    void 时序图文本解析出参与者与消息() {
        SequenceConfig c = parsed(SEQ_TEXT);

        // 参与者按首次出现顺序推导: 用户(行1 from) / 系统(行1 to) / 数据库(行2 to)
        assertEquals(3, c.getParticipants().size());
        assertEquals("用户", c.getParticipants().get(0).getName());
        assertEquals("系统", c.getParticipants().get(1).getName());
        assertEquals("数据库", c.getParticipants().get(2).getName());

        assertEquals(4, c.getMessages().size());
        assertEquals("request", c.getMessages().get(0).getType());
        assertEquals("request", c.getMessages().get(1).getType());
        assertEquals("return", c.getMessages().get(2).getType());
        assertEquals("return", c.getMessages().get(3).getType());

        assertEquals("用户", c.getMessages().get(0).getFrom());
        assertEquals("系统", c.getMessages().get(0).getTo());
        assertEquals("登录请求", c.getMessages().get(0).getText());
    }

    @Test
    void 注释与空行被忽略() {
        SequenceConfig c = parsed(String.join("\n",
                "# 这是一条注释",
                "",
                "用户 -> 系统: 登录请求",
                "",
                "系统 --> 用户: 登录成功"));
        assertEquals(2, c.getParticipants().size());
        assertEquals(2, c.getMessages().size());
    }

    @Test
    void 显式声明参与者可固定顺序() {
        SequenceConfig c = parsed(String.join("\n",
                "@participants 数据库, 用户, 系统",
                "用户 -> 系统: 登录请求"));
        assertEquals(3, c.getParticipants().size());
        assertEquals("数据库", c.getParticipants().get(0).getName());
        assertEquals("用户", c.getParticipants().get(1).getName());
        assertEquals("系统", c.getParticipants().get(2).getName());
    }

    @Test
    void 语法错误返回行号与无法识别的片段() {
        DslParseResult r = svc.parse("SEQUENCE", String.join("\n",
                "用户 -> 系统: 登录请求",
                "系统 -> 数据库: 查询账号",
                "数据库 >> 系统: 返回结果"));

        assertFalse(r.isOk());
        assertNotNull(r.getError());
        assertEquals(3, r.getError().getLine(), "错误行号应指向第 3 行");
        assertTrue(r.getError().getSnippet().contains(">>"),
                "片段应含无法识别的符号，实际: " + r.getError().getSnippet());
        assertNotNull(r.getError().getHint(), "必须给出该图的语法提示");
    }

    @Test
    void 往返一致参与者与消息不丢失() {
        SequenceConfig original = parsed(SEQ_TEXT);

        String text = svc.render("SEQUENCE", original);
        SequenceConfig roundTrip = parsed(text);

        assertEquals(original.getParticipants().size(), roundTrip.getParticipants().size());
        for (int i = 0; i < original.getParticipants().size(); i++) {
            assertEquals(original.getParticipants().get(i).getName(),
                    roundTrip.getParticipants().get(i).getName());
        }
        assertEquals(original.getMessages().size(), roundTrip.getMessages().size());
        for (int i = 0; i < original.getMessages().size(); i++) {
            SequenceConfig.MessageConfig a = original.getMessages().get(i);
            SequenceConfig.MessageConfig b = roundTrip.getMessages().get(i);
            assertEquals(a.getFrom(), b.getFrom());
            assertEquals(a.getTo(), b.getTo());
            assertEquals(a.getText(), b.getText());
            assertEquals(a.getType(), b.getType());
        }
    }

    @Test
    void 标题在往返中保留() {
        SequenceConfig c = parsed(String.join("\n",
                "@title 用户登录时序图",
                "用户 -> 系统: 登录请求"));
        assertEquals("用户登录时序图", c.getTitle());

        SequenceConfig roundTrip = parsed(svc.render("SEQUENCE", c));
        assertEquals("用户登录时序图", roundTrip.getTitle(), "render 后 title 不应丢失");
    }

    @Test
    void 只有参与者没有消息时往返也不丢() {
        SequenceConfig c = new SequenceConfig();
        c.setTitle("仅参与者");
        List<SequenceConfig.ParticipantConfig> ps = new ArrayList<>();
        for (String n : new String[]{"学生", "教师"}) {
            SequenceConfig.ParticipantConfig p = new SequenceConfig.ParticipantConfig();
            p.setId(n);
            p.setName(n);
            ps.add(p);
        }
        c.setParticipants(ps);

        SequenceConfig roundTrip = parsed(svc.render("SEQUENCE", c));
        assertEquals(2, roundTrip.getParticipants().size());
        assertEquals("学生", roundTrip.getParticipants().get(0).getName());
        assertEquals("教师", roundTrip.getParticipants().get(1).getName());
        assertTrue(roundTrip.getMessages().isEmpty());
    }

    @Test
    void 三线表被明确拒绝并指向三线表页面() {
        DslParseResult r = svc.parse("TABLE3", "表3-1 实验数据");
        assertFalse(r.isOk());
        assertTrue(r.getError().getMessage().contains("三线表"),
                "提示应指向三线表页面，实际: " + r.getError().getMessage());
    }

    @Test
    void 尚未开放的图类型与非法类型区分对待() {
        DslParseResult notYet = svc.parse("FLOW", "开始 -> 结束");
        assertFalse(notYet.isOk());
        assertTrue(notYet.getError().getMessage().contains("尚未开放"),
                "未实现的图应说明尚未开放，实际: " + notYet.getError().getMessage());

        DslParseResult unknown = svc.parse("NOT_A_DIAGRAM", "x");
        assertFalse(unknown.isOk());
        assertNotNull(unknown.getError().getMessage());
    }

    @Test
    void 空输入给出明确提示而非静默成功() {
        DslParseResult r = svc.parse("SEQUENCE", "   ");
        assertFalse(r.isOk(), "空输入不应判定为解析成功");
        assertNotNull(r.getError().getMessage());
    }

    /** 输入法把 "-" 打成 em dash / 全角减号时, 视觉上无法分辨, 解析器必须兜住 */
    @Test
    void 输入法变体箭头长破折号与全角减号都能解析() {
        for (String arrow : new String[]{"—>", "——>", "–>", "－>", "- >", "→"}) {
            DslParseResult r = svc.parse("SEQUENCE", "用户 " + arrow + " 系统: 登录请求");
            assertTrue(r.isOk(), "箭头变体 [" + arrow + "] 应能解析，实际: "
                    + (r.getError() == null ? "" : r.getError().getMessage()));
            SequenceConfig c = (SequenceConfig) r.getConfig();
            assertEquals(1, c.getMessages().size());
            assertEquals("用户", c.getMessages().get(0).getFrom(), "箭头 [" + arrow + "] 的 from 应干净");
            assertEquals("系统", c.getMessages().get(0).getTo());
        }
    }

    /** 全角空格是 \u3000, String.trim() 不会去掉它 —— 名字带隐形尾巴会在图上显示异常 */
    @Test
    void 全角空格分隔时参与者名字不带隐形尾巴() {
        SequenceConfig c = parsed("用户\u3000->\u3000系统: 登录请求");
        assertEquals("用户", c.getMessages().get(0).getFrom());
        assertEquals("系统", c.getMessages().get(0).getTo());
        for (SequenceConfig.ParticipantConfig p : c.getParticipants()) {
            assertFalse(p.getName().contains("\u3000"), "参与者名不应含全角空格: " + p.getName());
        }
    }
}
