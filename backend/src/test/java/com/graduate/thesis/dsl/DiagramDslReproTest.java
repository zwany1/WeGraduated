package com.graduate.thesis.dsl;

import com.graduate.thesis.dto.SequenceConfig;
import org.junit.jupiter.api.Test;

/**
 * 重现线上"第 2 行无法识别"问题: 用前端 SEQ_EXAMPLE 的确切文本 + 各种可疑变体逐一试。
 */
class DiagramDslReproTest {

    private final DiagramDslService svc = new DiagramDslService();

    private void probe(String name, String text) {
        DslParseResult r = svc.parse("SEQUENCE", text);
        if (r.isOk()) {
            SequenceConfig c = (SequenceConfig) r.getConfig();
            System.out.println("[OK  ] " + name + " -> 参与者=" + c.getParticipants().size()
                    + " 消息=" + c.getMessages().size()
                    + " 首消息 from=" + (c.getMessages().isEmpty() ? "-" : c.getMessages().get(0).getFrom()));
        } else {
            DslParseResult.DslError e = r.getError();
            System.out.println("[FAIL] " + name + " -> line=" + e.getLine()
                    + " snippet=[" + e.getSnippet() + "]");
        }
    }

    @Test
    void 重现() {
        // 1. 前端 SEQ_EXAMPLE 的精确文本
        probe("前端示例原样", String.join("\n",
                "@title 用户登录时序图",
                "用户 -> 系统: 提交登录信息",
                "系统 -> 数据库: 查询账号",
                "数据库 --> 系统: 返回查询结果",
                "系统 --> 用户: 登录成功"));

        // 2. 全角空格分隔(中文输入法常见)
        probe("全角空格分隔", "用户\u3000->\u3000系统: 提交登录信息");

        // 3. 全角冒号
        probe("全角冒号", "用户 -> 系统：提交登录信息");

        // 4. 全角空格 + 全角冒号
        probe("全角空格加全角冒号", "用户\u3000->\u3000系统：提交登录信息");

        // 5. 行尾全角空格
        probe("行尾全角空格", "用户 -> 系统: 提交登录信息\u3000");

        // 6. 零宽空格夹在名字与箭头之间
        probe("零宽空格", "用户\u200B->\u200B系统: 提交登录信息");

        // 7. CRLF
        probe("CRLF", "用户 -> 系统: 登录\r\n系统 --> 用户: 成功");

        // 8. 多个连续半角空格
        probe("多空格", "用户  ->  系统: 提交登录信息");

        // 9. 中文箭头(输入法候选词可能带出)
        probe("中文长箭头", "用户 ——> 系统: 提交登录信息");

        // 10. 全角加号连字符(输入法把 - 转成 —)
        probe("em dash", "用户 —> 系统: 提交登录信息");

        // 11. @title 行单独验证
        probe("只有title行", "@title 用户登录时序图");

        // 12. title 后紧跟消息(无空行)
        probe("title加消息", "@title 用户登录时序图\n用户 -> 系统: 提交登录信息");
    }
}
