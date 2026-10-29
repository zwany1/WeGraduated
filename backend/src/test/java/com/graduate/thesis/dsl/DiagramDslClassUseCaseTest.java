package com.graduate.thesis.dsl;

import com.graduate.thesis.dto.ClassConfig;
import com.graduate.thesis.dto.UseCaseConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 票 04：类图与用例图方言。
 *
 * <p>这两种图嵌套最深，最容易出现的退路是「把成员当纯文本存」——
 * 所以这里既校验结构(属性/方法分开、可见性保真)，也校验往返一致。</p>
 */
class DiagramDslClassUseCaseTest {

    private final DiagramDslService dsl = new DiagramDslService();

    private static final String CLASS_EXAMPLE = String.join("\n",
            "class 论文 {",
            "  - 标题: String",
            "  + 提交()",
            "}",
            "论文 -- 学生: 属于");

    private static final String USECASE_EXAMPLE = String.join("\n",
            "(学生): 提交论文, 查看进度",
            "提交论文 -include-> 格式校验");

    // ---------- 类图 ----------

    @Test
    void 类图解析出类属性方法与关系() {
        DslParseResult r = dsl.parse("CLASS", CLASS_EXAMPLE);
        assertTrue(r.isOk(), () -> "解析应成功，实际: " + (r.getError() == null ? "" : r.getError().getMessage()));
        ClassConfig c = (ClassConfig) r.getConfig();

        assertEquals(2, c.getClasses().size(), "关系另一端的「学生」应被自动补成类");
        ClassConfig.ClassItem paper = classByName(c, "论文");
        assertNotNull(paper);
        assertEquals(1, paper.getAttributes().size());
        assertEquals("标题", paper.getAttributes().get(0).getName());
        assertEquals("String", paper.getAttributes().get(0).getType());
        assertEquals("-", paper.getAttributes().get(0).getVisibility(), "可见性必须保真, 不能退化成纯文本");

        assertEquals(1, paper.getMethods().size());
        assertEquals("提交", paper.getMethods().get(0).getName());
        assertEquals("+", paper.getMethods().get(0).getVisibility());

        assertEquals(1, c.getRelations().size());
        ClassConfig.ClassRelation rel = c.getRelations().get(0);
        assertEquals("论文", classNameOf(c, rel.getSource()));
        assertEquals("学生", classNameOf(c, rel.getTarget()));
        assertEquals("association", rel.getType());
        assertEquals("属于", rel.getLabel());
    }

    @Test
    void 类图方法缺可见性前缀时报错定位到行() {
        String bad = String.join("\n",
                "class 论文 {",
                "  - 标题: String",
                "  提交()",
                "}");
        DslParseResult r = dsl.parse("CLASS", bad);
        assertFalse(r.isOk());
        assertEquals(3, r.getError().getLine());
        assertTrue(r.getError().getSnippet().contains("提交()"), "片段应包含出错内容: " + r.getError().getSnippet());
        assertTrue(r.getError().getMessage().contains("可见性"), "提示应说明缺可见性符号: " + r.getError().getMessage());
    }

    @Test
    void 类图关系箭头映射到关系类型且能带基数() {
        String text = String.join("\n",
                "class 论文",
                "class 学位论文",
                "class 章节",
                "学位论文 <|-- 论文: 泛化",
                "论文 *-- 章节 [1 n]: 包含");
        DslParseResult r = dsl.parse("CLASS", text);
        assertTrue(r.isOk(), () -> String.valueOf(r.getError() == null ? "" : r.getError().getMessage()));
        ClassConfig c = (ClassConfig) r.getConfig();
        assertEquals("inheritance", c.getRelations().get(0).getType());
        assertEquals("composition", c.getRelations().get(1).getType());
        assertEquals("1", c.getRelations().get(1).getLeft());
        assertEquals("n", c.getRelations().get(1).getRight());
        assertEquals("包含", c.getRelations().get(1).getLabel());
    }

    @Test
    void 类图往返一致() {
        ClassConfig c = (ClassConfig) dsl.parse("CLASS", "@title 论文管理类图\n" + CLASS_EXAMPLE).getConfig();
        String text = dsl.render("CLASS", c);
        DslParseResult again = dsl.parse("CLASS", text);
        assertTrue(again.isOk(), "自己渲染的文本必须能被自己解析, 文本:\n" + text);
        String text2 = dsl.render("CLASS", again.getConfig());
        assertEquals(text, text2, "往返后文本应稳定");
    }

    // ---------- 用例图 ----------

    @Test
    void 用例图解析出参与者用例与include关系() {
        DslParseResult r = dsl.parse("USECASE", USECASE_EXAMPLE);
        assertTrue(r.isOk(), () -> "解析应成功，实际: " + (r.getError() == null ? "" : r.getError().getMessage()));
        UseCaseConfig c = (UseCaseConfig) r.getConfig();

        assertEquals(1, c.getActors().size());
        assertEquals("学生", c.getActors().get(0).getName());

        // 参与者的 2 个用例 + include 目标(格式校验)
        assertEquals(3, c.getUsecases().size(), namesOfUseCases(c));

        long assoc = c.getRelations().stream().filter(x -> "association".equals(x.getType())).count();
        assertEquals(2, assoc, "参与者行里的两个用例应生成两条关联");

        UseCaseConfig.RelationConfig include = c.getRelations().stream()
                .filter(x -> "include".equals(x.getType())).findFirst().orElse(null);
        assertNotNull(include, "应有 include 关系");
        assertEquals("提交论文", ucNameOf(c, include.getSource()));
        assertEquals("格式校验", ucNameOf(c, include.getTarget()));
    }

    @Test
    void 用例图支持extend与裸参与者() {
        String text = String.join("\n",
                "(教师)",
                "(学生): 提交论文",
                "审核论文 -extend-> 提交论文");
        DslParseResult r = dsl.parse("USECASE", text);
        assertTrue(r.isOk());
        UseCaseConfig c = (UseCaseConfig) r.getConfig();
        assertEquals(2, c.getActors().size());
        assertTrue(c.getRelations().stream().anyMatch(x -> "extend".equals(x.getType())));
        assertEquals(2, c.getUsecases().size(), "提交论文 + 审核论文");
    }

    @Test
    void 用例图无法识别的行报错定位() {
        DslParseResult r = dsl.parse("USECASE", "(学生): 提交论文\n这行什么都不像");
        assertFalse(r.isOk());
        assertEquals(2, r.getError().getLine());
        assertTrue(r.getError().getHint().contains("用例图语法"));
    }

    @Test
    void 用例图往返一致() {
        UseCaseConfig c = (UseCaseConfig) dsl.parse("USECASE", "@title 论文管理系统\n" + USECASE_EXAMPLE).getConfig();
        String text = dsl.render("USECASE", c);
        DslParseResult again = dsl.parse("USECASE", text);
        assertTrue(again.isOk(), "自己渲染的文本必须能被自己解析, 文本:\n" + text);
        assertEquals(text, dsl.render("USECASE", again.getConfig()), "往返后文本应稳定");
    }

    // ---------- 辅助 ----------

    private ClassConfig.ClassItem classByName(ClassConfig c, String name) {
        return c.getClasses().stream().filter(x -> name.equals(x.getName())).findFirst().orElse(null);
    }

    private String classNameOf(ClassConfig c, String id) {
        return c.getClasses().stream().filter(x -> id.equals(x.getId())).findFirst()
                .map(ClassConfig.ClassItem::getName).orElse("<" + id + ">");
    }

    private String namesOfUseCases(UseCaseConfig c) {
        return c.getUsecases().stream().map(UseCaseConfig.UseCaseConfigItem::getName).toList().toString();
    }

    private String ucNameOf(UseCaseConfig c, String id) {
        return c.getUsecases().stream().filter(x -> id.equals(x.getId())).findFirst()
                .map(UseCaseConfig.UseCaseConfigItem::getName).orElse("<" + id + ">");
    }

    @SuppressWarnings("unused")
    private List<String> unusedHelper() {
        return List.of();
    }
}
