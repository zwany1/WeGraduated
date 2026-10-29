package com.graduate.thesis.dsl;

import com.graduate.thesis.dto.ArchitectureConfig;
import com.graduate.thesis.dto.FlowConfig;
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

    /** 已实现方言的图类型。票 01 时序图、票 03 流程图与架构图，其余逐票补齐 */
    private static final Set<String> IMPLEMENTED = Set.of("SEQUENCE", "FLOW", "ARCH");

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

    private static final String FLOW_HINT = String.join("\n",
            "流程图语法：",
            "  开始 -> 提交论文            每行一条连线",
            "  格式检查 -> 合格?           名字带 ? 即判断节点（菱形）",
            "  合格? -> 通过: 是           「: 」后为分支条件",
            "  @title 论文提交流程         可选，图标题",
            "  # 开头为注释；空行忽略");

    private static final String ARCH_HINT = String.join("\n",
            "架构图语法（缩进表示从属）：",
            "  表现层                      顶格写层名",
            "    Vue3 前端                 缩进写该层的组件",
            "  服务层                      再顶格写下一层",
            "    SpringBoot",
            "  @title 系统架构             可选，系统名",
            "  同一层下的组件缩进要一致；只支持「层 → 组件」两层");

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
            return DslParseResult.fail(0, "", hintOf(t), "请输入内容后再生成");
        }
        switch (t) {
            case "SEQUENCE":
                return parseSequence(text);
            case "FLOW":
                return parseFlow(text);
            case "ARCH":
                return parseArch(text);
            default:
                return DslParseResult.fail(0, "", "", "未实现");
        }
    }

    private String hintOf(String type) {
        switch (type) {
            case "FLOW":
                return FLOW_HINT;
            case "ARCH":
                return ARCH_HINT;
            default:
                return SEQ_HINT;
        }
    }

    /** 图配置 → 文本。用于回填输入框，以及把画布/表单改动同步回文本 */
    public String render(String type, Object config) {
        String t = type == null ? "" : type.trim().toUpperCase();
        if (!IMPLEMENTED.contains(t)) {
            throw new IllegalArgumentException("该图类型暂不支持生成文本: " + type);
        }
        switch (t) {
            case "SEQUENCE":
                if (!(config instanceof SequenceConfig)) {
                    throw new IllegalArgumentException("配置类型与图类型不匹配: " + t);
                }
                return renderSequence((SequenceConfig) config);
            case "FLOW":
                if (!(config instanceof FlowConfig)) {
                    throw new IllegalArgumentException("配置类型与图类型不匹配: " + t);
                }
                return renderFlow((FlowConfig) config);
            case "ARCH":
                if (!(config instanceof ArchitectureConfig)) {
                    throw new IllegalArgumentException("配置类型与图类型不匹配: " + t);
                }
                return renderArch((ArchitectureConfig) config);
            default:
                throw new IllegalArgumentException("该图类型暂不支持生成文本: " + type);
        }
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
        // 引擎按参与者 id 校验/过滤消息(validMessages 只认 id), 而 DSL 里用户写的是名字
        // —— 统一映射成 id, 与前端表单(下拉框提交 id)的产出完全一致
        java.util.Map<String, String> nameToId = new java.util.HashMap<>();
        for (SequenceConfig.ParticipantConfig p : participants) {
            nameToId.putIfAbsent(p.getName(), p.getId());
        }
        for (SequenceConfig.MessageConfig m : messages) {
            String fid = nameToId.get(m.getFrom());
            String tid = nameToId.get(m.getTo());
            if (fid != null) {
                m.setFrom(fid);
            }
            if (tid != null) {
                m.setTo(tid);
            }
        }
        cfg.setParticipants(participants);
        cfg.setMessages(messages);
        return DslParseResult.success(cfg);
    }

    private String renderSequence(SequenceConfig c) {
        // config 里的 from/to 是参与者 id(parse 与前端表单均如此), 输出文本时转回名字才可读
        java.util.Map<String, String> idToName = new java.util.HashMap<>();
        if (c.getParticipants() != null) {
            for (SequenceConfig.ParticipantConfig p : c.getParticipants()) {
                if (p.getId() != null && p.getName() != null) {
                    idToName.put(p.getId(), p.getName());
                }
            }
        }
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
                String from = idToName.getOrDefault(String.valueOf(m.getFrom()), String.valueOf(m.getFrom()));
                String to = idToName.getOrDefault(String.valueOf(m.getTo()), String.valueOf(m.getTo()));
                sb.append(from)
                        .append("return".equalsIgnoreCase(String.valueOf(m.getType())) ? " --> " : " -> ")
                        .append(to);
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

    // ==================== 流程图(FLOW) ====================

    private static final Pattern FLOW_EDGE = Pattern.compile(
            "^(.+?)\\s*(-->|->)\\s*(.+?)\\s*(?:[:：]\\s*(.*))?$");

    /** 判断一个节点名是否表示判断: 名字带问号, 或用方括号括起来 */
    private boolean isConditionName(String rawName, String name) {
        if (name.contains("?") || name.contains("？")) {
            return true;
        }
        String s = rawName.trim();
        return (s.startsWith("[") && s.endsWith("]")) || (s.startsWith("【") && s.endsWith("】"));
    }

    private String stripBrackets(String raw) {
        String s = raw.trim();
        if (s.length() > 1 && ((s.startsWith("[") && s.endsWith("]")) || (s.startsWith("【") && s.endsWith("】")))) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private DslParseResult parseFlow(String text) {
        FlowConfig cfg = new FlowConfig();
        List<FlowConfig.Node> nodes = new ArrayList<>();
        List<FlowConfig.Edge> edges = new ArrayList<>();
        java.util.Map<String, FlowConfig.Node> byName = new java.util.LinkedHashMap<>();

        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            String line = normalizeLine(lines[i]).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Matcher title = TITLE.matcher(line);
            if (title.matches()) {
                cfg.setTitle(title.group(1));
                continue;
            }

            Matcher m = FLOW_EDGE.matcher(line);
            if (!m.matches()) {
                // 不含连接符的整行 = 孤立节点(没有连线也要能画出来)
                if (line.contains(">") || line.contains("-") || line.contains("→")) {
                    return DslParseResult.fail(lineNo, line, FLOW_HINT,
                            "第 " + lineNo + " 行缺少连接符，应写成「节点A -> 节点B」" + suspiciousChars(lines[i]));
                }
                nodeOf(byName, nodes, stripBrackets(line), isConditionName(line, line));
                continue;
            }

            String rawFrom = m.group(1).trim();
            String rawTo = m.group(3).trim();
            String from = cleanName(stripBrackets(rawFrom));
            String to = cleanName(stripBrackets(rawTo));
            String label = m.group(4) == null ? "" : cleanName(m.group(4));
            if (from.isEmpty() || to.isEmpty()) {
                return DslParseResult.fail(lineNo, line, FLOW_HINT,
                        "第 " + lineNo + " 行连接符两侧都要有节点名");
            }
            FlowConfig.Node nf = nodeOf(byName, nodes, from, isConditionName(rawFrom, from));
            FlowConfig.Node nt = nodeOf(byName, nodes, to, isConditionName(rawTo, to));

            FlowConfig.Edge e = new FlowConfig.Edge();
            e.setFrom(nf.getId());
            e.setTo(nt.getId());
            e.setLabel(label.isEmpty() ? null : label);
            edges.add(e);
        }

        if (nodes.isEmpty()) {
            return DslParseResult.fail(0, "", FLOW_HINT, "请至少写一条连线，如「开始 -> 提交论文」");
        }
        cfg.setNodes(nodes);
        cfg.setEdges(edges);
        return DslParseResult.success(cfg);
    }

    /** 取或建节点; 已存在的节点若这次带了判断标记, 升级为判断类型 */
    private FlowConfig.Node nodeOf(java.util.Map<String, FlowConfig.Node> byName,
                                   List<FlowConfig.Node> nodes, String name, boolean condition) {
        FlowConfig.Node n = byName.get(name);
        if (n == null) {
            n = new FlowConfig.Node();
            n.setId("n" + (nodes.size() + 1));
            n.setName(name);
            n.setType(inferFlowType(name, condition));
            byName.put(name, n);
            nodes.add(n);
        } else if (condition && !"condition".equals(n.getType())) {
            n.setType("condition");
        }
        return n;
    }

    private String inferFlowType(String name, boolean condition) {
        if (condition) {
            return "condition";
        }
        String s = name.trim();
        if (s.equals("开始") || s.equalsIgnoreCase("start")) {
            return "start";
        }
        if (s.equals("结束") || s.equals("完成") || s.equalsIgnoreCase("end")) {
            return "end";
        }
        return "action";
    }

    private String renderFlow(FlowConfig c) {
        java.util.Map<String, String> nameOf = new java.util.HashMap<>();
        if (c.getNodes() != null) {
            for (FlowConfig.Node n : c.getNodes()) {
                if (n.getId() != null) {
                    nameOf.put(n.getId(), n.getName());
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        if (c.getTitle() != null && !c.getTitle().isBlank()) {
            sb.append("@title ").append(c.getTitle()).append('\n');
        }
        java.util.Set<String> used = new java.util.LinkedHashSet<>();
        if (c.getEdges() != null) {
            for (FlowConfig.Edge e : c.getEdges()) {
                String from = nameOf.getOrDefault(e.getFrom(), String.valueOf(e.getFrom()));
                String to = nameOf.getOrDefault(e.getTo(), String.valueOf(e.getTo()));
                used.add(String.valueOf(e.getFrom()));
                used.add(String.valueOf(e.getTo()));
                sb.append(from).append(" -> ").append(to);
                if (e.getLabel() != null && !e.getLabel().isBlank()) {
                    sb.append(": ").append(e.getLabel());
                }
                sb.append('\n');
            }
        }
        // 没有任何连线的节点也要输出, 否则文本往返会把它丢掉
        if (c.getNodes() != null) {
            for (FlowConfig.Node n : c.getNodes()) {
                if (n.getId() != null && !used.contains(n.getId()) && n.getName() != null && !n.getName().isBlank()) {
                    sb.append(n.getName()).append('\n');
                }
            }
        }
        return sb.toString();
    }

    // ==================== 架构图(ARCH) ====================

    /** 行首缩进宽度: tab 记 2 格, 全角空格/普通空格都算 1 格 */
    private int indentOf(String line) {
        int n = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\t') {
                n += 2;
            } else if (c == ' ' || c == '\u3000' || c == '\u00a0') {
                n += 1;
            } else {
                break;
            }
        }
        return n;
    }

    private DslParseResult parseArch(String text) {
        ArchitectureConfig cfg = new ArchitectureConfig();
        List<ArchitectureConfig.LayerConfig> layers = new ArrayList<>();
        ArchitectureConfig.LayerConfig current = null;
        Integer childIndent = null;

        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            String raw = lines[i];
            int indent = indentOf(raw);
            String line = normalizeLine(raw).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Matcher title = TITLE.matcher(line);
            if (title.matches()) {
                cfg.setSystemName(title.group(1));
                continue;
            }
            if (indent == 0) {
                ArchitectureConfig.LayerConfig layer = new ArchitectureConfig.LayerConfig();
                layer.setName(cleanName(line));
                layers.add(layer);
                current = layer;
                childIndent = null;
                continue;
            }
            if (current == null) {
                return DslParseResult.fail(lineNo, line, ARCH_HINT,
                        "第 " + lineNo + " 行的组件没有归属：请先写层名，再把组件缩进写在它下面");
            }
            if (childIndent == null) {
                childIndent = indent;
            } else if (indent != childIndent) {
                return DslParseResult.fail(lineNo, line, ARCH_HINT,
                        "第 " + lineNo + " 行缩进与同层其他组件不一致（本层组件都缩进 " + childIndent
                                + " 格，这行是 " + indent + " 格）");
            }
            ArchitectureConfig.ComponentConfig comp = new ArchitectureConfig.ComponentConfig();
            comp.setName(cleanName(line));
            current.getComponents().add(comp);
        }

        if (layers.isEmpty()) {
            return DslParseResult.fail(0, "", ARCH_HINT, "请至少写一个层，如「表现层」");
        }
        cfg.setLayers(layers);
        return DslParseResult.success(cfg);
    }

    private String renderArch(ArchitectureConfig c) {
        StringBuilder sb = new StringBuilder();
        if (c.getSystemName() != null && !c.getSystemName().isBlank()) {
            sb.append("@title ").append(c.getSystemName()).append('\n');
        }
        if (c.getLayers() != null) {
            for (ArchitectureConfig.LayerConfig layer : c.getLayers()) {
                if (layer.getName() == null || layer.getName().isBlank()) {
                    continue;
                }
                sb.append(layer.getName()).append('\n');
                if (layer.getComponents() != null) {
                    for (ArchitectureConfig.ComponentConfig comp : layer.getComponents()) {
                        if (comp.getName() == null || comp.getName().isBlank()) {
                            continue;
                        }
                        sb.append("  ").append(comp.getName()).append('\n');
                    }
                }
            }
        }
        return sb.toString();
    }
}
