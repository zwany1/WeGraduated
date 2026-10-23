package com.graduate.thesis.dsl;

import com.graduate.thesis.dto.SequenceConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统图文本 DSL: 文本 ↔ 图模型配置的双向转换。
 *
 * <p>两个方向都是必需的，不是可选品：只有 parse 而没有 render 时，
 * 用户在画布或表单上的改动，一切回文本视图就会被文本覆盖回旧版本（R4）。</p>
 *
 * <p>解析全部在本地完成，不调用任何外部服务（R3）。</p>
 */
@Service
public class DiagramDslService {

    /** 支持文本输入的 8 种图；三线表不在其中（它是表格而非图，且有独立的 SQL 解析入口） */
    private static final Set<String> SUPPORTED = Set.of(
            "FLOW", "ARCH", "SWIMLANE", "ACTIVITY", "USECASE", "SEQUENCE", "CLASS", "ER");

    /** 已实现方言的图类型。票 01 只打通时序图，其余逐票补齐 */
    private static final Set<String> IMPLEMENTED = Set.of("SEQUENCE");

    private static final Pattern MSG = Pattern.compile(
            "^\\s*(.+?)\\s*(-->|->)\\s*(.+?)\\s*(?:[:：]\\s*(.*))?\\s*$");
    private static final Pattern PARTICIPANTS = Pattern.compile("^\\s*@participants\\s*(.+?)\\s*$");
    private static final Pattern TITLE = Pattern.compile("^\\s*@title\\s+(.+?)\\s*$");

    private static final String SEQ_HINT = String.join("\n",
            "时序图语法：",
            "  用户 -> 系统: 登录请求      实线（请求）",
            "  系统 --> 用户: 登录成功     虚线（返回）",
            "  @participants 用户, 系统    可选，固定参与者顺序",
            "  @title 用户登录时序图       可选，图标题",
            "  # 开头为注释；空行忽略");

    /**
     * 文本 → 图配置。
     *
     * @return 成功时 config 非空；失败时 error 携带行号与语法提示
     */
    public DslParseResult parse(String type, String text) {
        String t = type == null ? "" : type.trim().toUpperCase();

        if (!SUPPORTED.contains(t)) {
            if ("TABLE3".equals(t)) {
                return DslParseResult.fail(0, "", "",
                        "三线表请使用三线表页面（支持粘贴建表 SQL 自动生成）");
            }
            return DslParseResult.fail(0, "", "", "不支持的图类型：" + type);
        }
        if (!IMPLEMENTED.contains(t)) {
            return DslParseResult.fail(0, "", "", "该图类型的文本输入尚未开放，请先用表单配置");
        }
        if (text == null || text.trim().isEmpty()) {
            return DslParseResult.fail(0, "", SEQ_HINT, "请输入内容后再生成");
        }
        return "SEQUENCE".equals(t) ? parseSequence(text) : DslParseResult.fail(0, "", "", "未实现");
    }

    /** 图配置 → 文本。用于回填输入框，以及把画布/表单改动同步回文本 */
    public String render(String type, Object config) {
        String t = type == null ? "" : type.trim().toUpperCase();
        if (!"SEQUENCE".equals(t)) {
            throw new IllegalArgumentException("该图类型暂不支持生成文本: " + type);
        }
        if (!(config instanceof SequenceConfig)) {
            throw new IllegalArgumentException("配置类型与图类型不匹配: " + t);
        }
        return renderSequence((SequenceConfig) config);
    }

    /**
     * 输入归一化: 只为结构识别。中文输入法的常见变体在视觉上与半角几乎无法区分
     * (全角空格 / 各类长破折号 / 零宽字符 / 汉字"一"当横线 / 单字符箭头),
     * 让用户逐字符排查是折磨 —— 统一归一后再匹配。
     */
    private static String normalizeLine(String line) {
        String s = line
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")      // 零宽字符/BOM
                .replaceAll("[\\p{Zs}\\t]", " ")                 // 所有 Unicode 空格(含全角/EN SPACE)
                .replaceAll("\\p{Pd}", "-")                      // 所有 Unicode 破折号/连字符(em/en/全角减号…)
                .replace("→", "->").replace("⇒", "->")
                .replace("➔", "->").replace("➜", "->")
                .replace("➤", "->").replace("➝", "->")
                .replace("➡", "->").replace("↦", "->").replace("⇢", "->");
        // 中文输入法打横线最常误触的汉字"一": 仅当其后紧跟 ">" 时当作横线归一,
        // 不做全局替换 —— 消息文本里的"一"不能动
        s = s.replaceAll("一(?=\\s*>)", "-");
        // 箭头内部被输入法拆开: "- >" / "-- >"
        s = s.replaceAll("-\\s+>", "->").replaceAll("--\\s+>", "-->");
        return s;
    }

    /**
     * 列出行内"非常规"字符的码点(非 ASCII、非 CJK 汉字、非中文标点),
     * 让解析失败的报错自带答案 —— 而不是让用户截图后靠猜。
     */
    private static String suspiciousChars(String line) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); ) {
            int cp = line.codePointAt(i);
            boolean ascii = cp >= 0x20 && cp <= 0x7E;
            boolean cjk = cp >= 0x4E00 && cp <= 0x9FFF;
            boolean cnPunct = cp >= 0x3000 && cp <= 0x303F;
            boolean fullwidth = cp >= 0xFF00 && cp <= 0xFFEF;
            if (!ascii && !cjk && !cnPunct && !fullwidth) {
                sb.append(String.format(" U+%04X(%c)", cp, cp));
            }
            i += Character.charCount(cp);
        }
        return sb.length() == 0 ? "" : "；该行含非常规字符:" + sb;
    }

    /** 参与者/实体名清理: 首尾空白 + 内部全角空格(否则名字带不可见尾巴, 图上显示异常) */
    private static String cleanName(String name) {
        return name == null ? "" : name.replace('\u3000', ' ').trim();
    }

    private DslParseResult parseSequence(String text) {
        SequenceConfig cfg = new SequenceConfig();
        List<SequenceConfig.ParticipantConfig> declared = new ArrayList<>();
        List<SequenceConfig.MessageConfig> messages = new ArrayList<>();
        LinkedHashSet<String> order = new LinkedHashSet<>();

        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i];
            int lineNo = i + 1;
            String line = normalizeLine(raw).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            Matcher title = TITLE.matcher(line);
            if (title.matches()) {
                cfg.setTitle(title.group(1));
                continue;
            }

            Matcher ps = PARTICIPANTS.matcher(line);
            if (ps.matches()) {
                for (String name : splitNames(ps.group(1))) {
                    declared.add(participant(name));
                    order.add(name);
                }
                continue;
            }

            Matcher m = MSG.matcher(line);
            if (!m.matches()) {
                return DslParseResult.fail(lineNo, line, SEQ_HINT,
                        "第 " + lineNo + " 行无法识别，请检查是否缺少 -> 或 --> 连接符"
                                + suspiciousChars(raw));
            }
            String from = cleanName(m.group(1));
            String arrow = m.group(2);
            String to = cleanName(m.group(3));
            String label = m.group(4) == null ? "" : cleanName(m.group(4));

            SequenceConfig.MessageConfig msg = new SequenceConfig.MessageConfig();
            msg.setId("m" + (messages.size() + 1));
            msg.setFrom(from);
            msg.setTo(to);
            msg.setText(label);
            msg.setType("-->".equals(arrow) ? "return" : "request");
            messages.add(msg);
            order.add(from);
            order.add(to);
        }

        // 显式声明的参与者优先保持其顺序，其余按消息中首次出现补齐
        List<SequenceConfig.ParticipantConfig> participants = new ArrayList<>(declared);
        for (String name : order) {
            if (participants.stream().noneMatch(p -> p.getName().equals(name))) {
                participants.add(participant(name));
            }
        }
        for (int i = 0; i < participants.size(); i++) {
            participants.get(i).setId("p" + (i + 1));
        }
        cfg.setParticipants(participants);
        cfg.setMessages(messages);
        return DslParseResult.success(cfg);
    }

    private String renderSequence(SequenceConfig c) {
        StringBuilder sb = new StringBuilder();
        if (c.getTitle() != null && !c.getTitle().isBlank()) {
            sb.append("@title ").append(c.getTitle()).append('\n');
        }
        if (c.getParticipants() != null && !c.getParticipants().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (SequenceConfig.ParticipantConfig p : c.getParticipants()) {
                if (p.getName() != null && !p.getName().isBlank()) {
                    names.add(p.getName());
                }
            }
            if (!names.isEmpty()) {
                sb.append("@participants ").append(String.join(", ", names)).append('\n');
            }
        }
        if (c.getMessages() != null) {
            for (SequenceConfig.MessageConfig m : c.getMessages()) {
                sb.append(m.getFrom())
                        .append("return".equalsIgnoreCase(String.valueOf(m.getType())) ? " --> " : " -> ")
                        .append(m.getTo());
                if (m.getText() != null && !m.getText().isBlank()) {
                    sb.append(": ").append(m.getText());
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private SequenceConfig.ParticipantConfig participant(String name) {
        SequenceConfig.ParticipantConfig p = new SequenceConfig.ParticipantConfig();
        p.setName(name);
        return p;
    }

    private List<String> splitNames(String raw) {
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[,，、]")) {
            String n = cleanName(part);
            if (!n.isEmpty()) {
                out.add(n);
            }
        }
        return out;
    }
}
