package com.graduate.thesis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.dto.LoginResponse;
import com.graduate.thesis.dto.UserAuthDTO;
import com.graduate.thesis.entity.FormatRule;
import com.graduate.thesis.entity.FormatTemplate;
import com.graduate.thesis.mapper.FormatRuleMapper;
import com.graduate.thesis.mapper.FormatTemplateMapper;
import com.graduate.thesis.service.PresetTemplateService;
import com.graduate.thesis.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证: 新用户注册后自动获得一份桂林信息科技学院格式模板。
 * 同时把测试账号与 token 落盘, 供后续 HTTP 排版测试复用。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PresetProvisioningTest {

    @Autowired
    private UserService userService;

    @Autowired
    private FormatTemplateMapper templateMapper;

    @Autowired
    private FormatRuleMapper ruleMapper;

    @Test
    void newUserGetsGuilinPresetTemplate() throws Exception {
        long ts = System.currentTimeMillis();
        String email = "guilin" + ts + "@test.local";
        String username = "gt" + ts;

        UserAuthDTO dto = new UserAuthDTO();
        dto.setEmail(email);
        dto.setUsername(username);
        dto.setPassword("Test123456");

        LoginResponse resp = userService.register(dto);
        assertNotNull(resp.getUserId(), "注册应返回 userId");

        List<FormatTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<FormatTemplate>().eq(FormatTemplate::getUserId, resp.getUserId()));
        assertEquals(1, templates.size(), "新用户应自动获得 1 份模板");

        FormatTemplate t = templates.get(0);
        assertEquals(PresetTemplateService.GUILIN_TEMPLATE_NAME, t.getName());
        assertTrue(t.getPageConfig().contains("\"left\":2.5"), "页边距左应为 2.5cm");
        assertTrue(t.getPageConfig().contains("\"abstract\""), "应包含摘要区段配置");
        assertTrue(t.getPageConfig().contains("\"section\""), "应包含谢辞/参考文献区段配置");
        assertTrue(Boolean.TRUE.equals(t.getGenerateToc()), "应开启目录排版");
        assertTrue(Boolean.TRUE.equals(t.getGenerateAbstract()), "应开启摘要排版");

        List<FormatRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<FormatRule>().eq(FormatRule::getTemplateId, t.getId()));
        assertEquals(7, rules.size(), "应有 7 条格式规则");

        FormatRule body = rules.stream().filter(r -> "body".equals(r.getRuleType())).findFirst().orElse(null);
        assertNotNull(body, "应有正文规则");
        assertEquals("宋体", body.getFont());
        assertEquals(12.0, body.getFontSize(), 0.001);
        assertEquals("exact", body.getLineSpacingType(), "校规要求固定值行距");
        assertEquals(20, body.getLineSpacingExact().intValue());

        FormatRule figure = rules.stream().filter(r -> "figure".equals(r.getRuleType())).findFirst().orElse(null);
        assertNotNull(figure, "应有图题注规则");
        assertEquals(10.5, figure.getFontSize(), 0.001, "题注五号=10.5pt");

        // 落盘账号信息, 供 HTTP 侧排版测试使用
        Path out = Paths.get("D:/project/Graduated/tmp_v3/test-account.txt");
        Files.createDirectories(out.getParent());
        String content = "userId=" + resp.getUserId() + "\nusername=" + username + "\ntoken=" + resp.getToken() + "\n";
        Files.write(out, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("[PRESET-TEST] account written to " + out + " token=" + resp.getToken());
    }
}
