package com.graduate.thesis.dsl;

import com.graduate.thesis.dto.ActivityConfig;
import com.graduate.thesis.dto.ArchitectureConfig;
import com.graduate.thesis.dto.ClassConfig;
import com.graduate.thesis.dto.ErDTO;
import com.graduate.thesis.dto.FlowConfig;
import com.graduate.thesis.dto.SequenceConfig;
import com.graduate.thesis.dto.SwimlaneConfig;
import com.graduate.thesis.dto.UseCaseConfig;
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

    /** 已实现方言的图类型。票 01 时序、03 流程/架构、04 类图/用例、05 泳道/活动/ER —— 8 种齐了 */
    private static final Set<String> IMPLEMENTED = Set.of(
            "SEQUENCE", "FLOW", "ARCH", "CLASS", "USECASE", "SWIMLANE", "ACTIVITY", "ER");

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

    private static final String CLASS_HINT = String.join("\n",
            "类图语法：",
            "  class 论文 {                开一个类",
            "    - 标题: String            属性：可见性(- / + / #) + 名字 + 类型",
            "    + 提交()                  方法：可见性 + 名字 + ()",
            "  }                           关掉类",
            "  论文 -- 学生 [1 n]: 属于     关系：-- 关联 / <|-- 继承 / *-- 组合 / o-- 聚合",
            "                               [1 n] 可选基数，: 后面是关系名",
            "  @title 论文管理类图          可选，图标题");

    private static final String USECASE_HINT = String.join("\n",
            "用例图语法：",
            "  (学生): 提交论文, 查看进度    参与者及其用例",
            "  (教师)                      只有参与者、没有用例也可以",
            "  提交论文 -include-> 格式校验  包含关系",
            "  审核论文 -extend-> 提交论文   扩展关系",
            "  学生 -> 提交论文             普通关联",
            "  @title 论文管理系统          可选，系统名");

    private static final String SWIMLANE_HINT = String.join("\n",
            "泳道图语法：",
            "  学生:                       泳道名，冒号结尾",
            "    提交申请                  缩进写该泳道的节点",
            "  教师:",
            "    审批申请",
            "  提交申请 -> 审批申请         连线写在泳道块之后",
            "  审批申请 -> 查看进度: 通过   可带条件标签",
            "  名字带 ? 即判断节点；开始/结束 自动识别");

    private static final String ACTIVITY_HINT = String.join("\n",
            "活动图语法：",
            "  学生:                       可以只写一条泳道，也可以不写（默认「主流程」）",
            "    提交申请",
            "  提交申请 -> 是否完整?        名字带 ? 即判断节点（菱形）",
            "  是否完整? -> 补材料: 否      分支条件写在冒号后",
            "  是否完整? -> 审核: 是",
            "  补材料 -> 提交申请           多条入边即自动合并");

    private static final String ER_HINT = String.join("\n",
            "ER 图语法：",
            "  论文 (论文编号*, 标题)       实体，括号里是属性，带 * 为主键",
            "  学生 (学号*, 姓名)",
            "  学生 1--N 论文: 撰写         关系，两端写基数(1 / N / M)，冒号后是关系名",
            "  没有属性的实体只写名字即可");

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
            case "CLASS":
                return parseClass(text);
            case "USECASE":
                return parseUseCase(text);
            case "SWIMLANE":
                return parseSwimlane(text);
            case "ACTIVITY":
                return parseActivity(text);
            case "ER":
                return parseEr(text);
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
            case "CLASS":
                return CLASS_HINT;
            case "USECASE":
                return USECASE_HINT;
            case "SWIMLANE":
                return SWIMLANE_HINT;
            case "ACTIVITY":
                return ACTIVITY_HINT;
            case "ER":
                return ER_HINT;
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
            case "CLASS":
                if (!(config instanceof ClassConfig)) {
                    throw new IllegalArgumentException("配置类型与图类型不匹配: " + t);
                }
                return renderClass((ClassConfig) config);
            case "USECASE":
                if (!(config instanceof UseCaseConfig)) {
                    throw new IllegalArgumentException("配置类型与图类型不匹配: " + t);
                }
                return renderUseCase((UseCaseConfig) config);
            case "SWIMLANE":
                if (!(config instanceof SwimlaneConfig)) {
                    throw new IllegalArgumentException("配置类型与图类型不匹配: " + t);
                }
                return renderSwimlane((SwimlaneConfig) config);
            case "ACTIVITY":
                if (!(config instanceof ActivityConfig)) {
                    throw new IllegalArgumentException("配置类型与图类型不匹配: " + t);
                }
                return renderActivity((ActivityConfig) config);
            case "ER":
                if (!(config instanceof ErDTO)) {
                    throw new IllegalArgumentException("配置类型与图类型不匹配: " + t);
                }
                return renderEr((ErDTO) config);
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

    // ==================== 类图(CLASS) ====================

    private static final Pattern CLASS_DECL = Pattern.compile("^class\\s+(.+?)\\s*(\\{)?\\s*$");
    private static final Pattern CLASS_MEMBER = Pattern.compile("^([-+#])\\s*(.+?)\\s*$");
    /** 关系: A <箭头> B [基数] [: 关系名]。长箭头必须排前面, 否则 -- 会先把 <|-- 吃掉 */
    private static final Pattern CLASS_REL = Pattern.compile(
            "^(.+?)\\s*(<\\|--|--\\|>|\\*--|o--|--)\\s*(.+?)\\s*(?:\\[([^\\]]*)\\])?\\s*(?:[:：]\\s*(.*))?$");

    private DslParseResult parseClass(String text) {
        ClassConfig cfg = new ClassConfig();
        List<ClassConfig.ClassItem> classes = new ArrayList<>();
        java.util.Map<String, ClassConfig.ClassItem> byName = new java.util.LinkedHashMap<>();
        List<ClassConfig.ClassRelation> relations = new ArrayList<>();
        ClassConfig.ClassItem current = null;

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
            if ("}".equals(line)) {
                if (current == null) {
                    return DslParseResult.fail(lineNo, line, CLASS_HINT,
                            "第 " + lineNo + " 行多了一个 }，前面没有开着的类");
                }
                current = null;
                continue;
            }

            if (current != null) {
                Matcher mm = CLASS_MEMBER.matcher(line);
                if (!mm.matches()) {
                    return DslParseResult.fail(lineNo, line, CLASS_HINT,
                            "第 " + lineNo + " 行不是合法成员：属性/方法要以可见性符号开头"
                                    + "（- 私有 / + 公有 / # 保护）；如果这是关系行，请先补 } 结束上一个类");
                }
                DslParseResult bad = addMember(current, mm.group(1), cleanName(mm.group(2)), lineNo, line);
                if (bad != null) {
                    return bad;
                }
                continue;
            }

            Matcher decl = CLASS_DECL.matcher(line);
            if (decl.matches()) {
                String name = cleanName(decl.group(1));
                if (name.isEmpty()) {
                    return DslParseResult.fail(lineNo, line, CLASS_HINT, "第 " + lineNo + " 行缺少类名");
                }
                ClassConfig.ClassItem item = classOf(byName, classes, name);
                current = decl.group(2) != null ? item : null;
                continue;
            }

            Matcher rel = CLASS_REL.matcher(line);
            if (rel.matches()) {
                String from = cleanName(rel.group(1));
                String to = cleanName(rel.group(3));
                if (from.isEmpty() || to.isEmpty()) {
                    return DslParseResult.fail(lineNo, line, CLASS_HINT,
                            "第 " + lineNo + " 行关系两侧都要有类名");
                }
                classOf(byName, classes, from);
                classOf(byName, classes, to);
                ClassConfig.ClassRelation r = new ClassConfig.ClassRelation();
                r.setSource(byName.get(from).getId());
                r.setTarget(byName.get(to).getId());
                r.setType(relationTypeOf(rel.group(2)));
                String cards = rel.group(4) == null ? "" : rel.group(4).trim();
                if (!cards.isEmpty()) {
                    String[] parts = cards.split("\\s+");
                    r.setLeft(parts.length > 0 ? parts[0] : null);
                    r.setRight(parts.length > 1 ? parts[1] : null);
                }
                String name = rel.group(5) == null ? "" : cleanName(rel.group(5));
                r.setLabel(name.isEmpty() ? null : name);
                relations.add(r);
                continue;
            }

            return DslParseResult.fail(lineNo, line, CLASS_HINT,
                    "第 " + lineNo + " 行无法识别：类用「class 名称 {」，关系用「A -- B」「A <|-- B」" + suspiciousChars(lines[i]));
        }

        if (current != null) {
            // 少写了一个 } 也能用, 但要让用户知道
            // (不当作错误: 缩进块写到文件末尾很常见)
        }
        if (classes.isEmpty()) {
            return DslParseResult.fail(0, "", CLASS_HINT, "请至少写一个类，如「class 论文 { }」");
        }
        cfg.setClasses(classes);
        cfg.setRelations(relations);
        return DslParseResult.success(cfg);
    }

    /** 解析一行成员(属性或方法); 结构不合法时返回失败结果 */
    private DslParseResult addMember(ClassConfig.ClassItem item, String vis, String body, int lineNo, String line) {
        if (body.isEmpty()) {
            return DslParseResult.fail(lineNo, line, CLASS_HINT, "第 " + lineNo + " 行只有可见性符号，缺少成员名");
        }
        int lp = body.indexOf('(');
        if (lp >= 0) {
            int rp = body.indexOf(')', lp);
            if (rp < 0) {
                return DslParseResult.fail(lineNo, line, CLASS_HINT, "第 " + lineNo + " 行的方法缺少右括号 )");
            }
            String name = cleanName(body.substring(0, lp));
            String after = body.substring(rp + 1).trim();
            String ret = "";
            if (after.startsWith(":") || after.startsWith("：")) {
                ret = cleanName(after.substring(1));
            }
            ClassConfig.Method m = new ClassConfig.Method();
            m.setName(name);
            m.setReturnType(ret.isEmpty() ? null : ret);
            m.setVisibility(vis);
            item.getMethods().add(m);
            return null;
        }
        String name = body;
        String type = "";
        int colon = indexOfColon(body);
        if (colon >= 0) {
            name = cleanName(body.substring(0, colon));
            type = cleanName(body.substring(colon + 1));
        }
        ClassConfig.Attribute a = new ClassConfig.Attribute();
        a.setName(name);
        a.setType(type.isEmpty() ? null : type);
        a.setVisibility(vis);
        item.getAttributes().add(a);
        return null;
    }

    private int indexOfColon(String s) {
        int c1 = s.indexOf(':');
        int c2 = s.indexOf('：');
        if (c1 < 0) return c2;
        if (c2 < 0) return c1;
        return Math.min(c1, c2);
    }

    private ClassConfig.ClassItem classOf(java.util.Map<String, ClassConfig.ClassItem> byName,
                                          List<ClassConfig.ClassItem> classes, String name) {
        ClassConfig.ClassItem item = byName.get(name);
        if (item == null) {
            item = new ClassConfig.ClassItem();
            item.setId("C" + (classes.size() + 1));
            item.setName(name);
            byName.put(name, item);
            classes.add(item);
        }
        return item;
    }

    private String relationTypeOf(String arrow) {
        switch (arrow) {
            case "<|--":
            case "--|>":
                return "inheritance";
            case "*--":
                return "composition";
            case "o--":
                return "aggregation";
            default:
                return "association";
        }
    }

    private String arrowOf(String type) {
        if (type == null) return "--";
        switch (type.trim().toLowerCase()) {
            case "inheritance":
                return "<|--";
            case "composition":
                return "*--";
            case "aggregation":
                return "o--";
            default:
                return "--";
        }
    }

    private String renderClass(ClassConfig c) {
        java.util.Map<String, String> nameOf = new java.util.HashMap<>();
        if (c.getClasses() != null) {
            for (ClassConfig.ClassItem ci : c.getClasses()) {
                if (ci.getId() != null) {
                    nameOf.put(ci.getId(), ci.getName());
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        if (c.getTitle() != null && !c.getTitle().isBlank()) {
            sb.append("@title ").append(c.getTitle()).append('\n');
        }
        if (c.getClasses() != null) {
            for (ClassConfig.ClassItem ci : c.getClasses()) {
                if (ci.getName() == null || ci.getName().isBlank()) {
                    continue;
                }
                boolean hasAttrs = ci.getAttributes() != null && !ci.getAttributes().isEmpty();
                boolean hasMethods = ci.getMethods() != null && !ci.getMethods().isEmpty();
                if (!hasAttrs && !hasMethods) {
                    sb.append("class ").append(ci.getName()).append('\n');
                    continue;
                }
                sb.append("class ").append(ci.getName()).append(" {\n");
                if (hasAttrs) {
                    for (ClassConfig.Attribute a : ci.getAttributes()) {
                        if (a.getName() == null || a.getName().isBlank()) continue;
                        sb.append("  ").append(visOf(a.getVisibility())).append(' ').append(a.getName());
                        if (a.getType() != null && !a.getType().isBlank()) {
                            sb.append(": ").append(a.getType());
                        }
                        sb.append('\n');
                    }
                }
                if (hasMethods) {
                    for (ClassConfig.Method m : ci.getMethods()) {
                        if (m.getName() == null || m.getName().isBlank()) continue;
                        sb.append("  ").append(visOf(m.getVisibility())).append(' ').append(m.getName()).append("()");
                        if (m.getReturnType() != null && !m.getReturnType().isBlank()) {
                            sb.append(": ").append(m.getReturnType());
                        }
                        sb.append('\n');
                    }
                }
                sb.append("}\n");
            }
        }
        if (c.getRelations() != null) {
            for (ClassConfig.ClassRelation r : c.getRelations()) {
                String from = nameOf.getOrDefault(r.getSource(), String.valueOf(r.getSource()));
                String to = nameOf.getOrDefault(r.getTarget(), String.valueOf(r.getTarget()));
                sb.append(from).append(' ').append(arrowOf(r.getType())).append(' ').append(to);
                StringBuilder cards = new StringBuilder();
                if (r.getLeft() != null && !r.getLeft().isBlank()) cards.append(r.getLeft().trim());
                if (r.getRight() != null && !r.getRight().isBlank()) {
                    cards.append(cards.length() > 0 ? " " : "").append(r.getRight().trim());
                }
                if (cards.length() > 0) {
                    sb.append(" [").append(cards).append(']');
                }
                if (r.getLabel() != null && !r.getLabel().isBlank()) {
                    sb.append(": ").append(r.getLabel().trim());
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String visOf(String vis) {
        if (vis == null || vis.isBlank()) return "+";
        String v = vis.trim();
        return ("+" + "-" + "#").contains(v) ? v : "+";
    }

    // ==================== 用例图(USECASE) ====================

    private static final Pattern UC_ACTOR = Pattern.compile("^\\((.+?)\\)\\s*(?:[:：]\\s*(.*))?$");
    private static final Pattern UC_REL = Pattern.compile(
            "^(.+?)\\s*(?:-(include|extend)->|->)\\s*(.+?)\\s*$");

    private DslParseResult parseUseCase(String text) {
        UseCaseConfig cfg = new UseCaseConfig();
        List<UseCaseConfig.ActorConfig> actors = new ArrayList<>();
        List<UseCaseConfig.UseCaseConfigItem> usecases = new ArrayList<>();
        List<UseCaseConfig.RelationConfig> relations = new ArrayList<>();
        java.util.Map<String, String> actorIds = new java.util.LinkedHashMap<>();
        java.util.Map<String, String> ucIds = new java.util.LinkedHashMap<>();

        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            String line = normalizeLine(lines[i]).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Matcher title = TITLE.matcher(line);
            if (title.matches()) {
                cfg.setSystem(title.group(1));
                continue;
            }

            Matcher actor = UC_ACTOR.matcher(line);
            if (actor.matches()) {
                String name = cleanName(actor.group(1));
                if (name.isEmpty()) {
                    return DslParseResult.fail(lineNo, line, USECASE_HINT, "第 " + lineNo + " 行缺少参与者名");
                }
                String aid = actorId(actorIds, actors, name);
                String rest = actor.group(2) == null ? "" : actor.group(2).trim();
                for (String ucName : splitNames(rest)) {
                    String uid = ucId(ucIds, usecases, ucName);
                    UseCaseConfig.RelationConfig r = new UseCaseConfig.RelationConfig();
                    r.setSource(aid);
                    r.setTarget(uid);
                    r.setType("association");
                    relations.add(r);
                }
                continue;
            }

            Matcher rel = UC_REL.matcher(line);
            if (rel.matches()) {
                String from = cleanName(stripBrackets(rel.group(1)));
                String to = cleanName(stripBrackets(rel.group(3)));
                if (from.isEmpty() || to.isEmpty()) {
                    return DslParseResult.fail(lineNo, line, USECASE_HINT,
                            "第 " + lineNo + " 行关系两侧都要有名字");
                }
                String type = rel.group(2) == null ? "association" : rel.group(2);
                UseCaseConfig.RelationConfig r = new UseCaseConfig.RelationConfig();
                r.setSource(idOfAny(actorIds, ucIds, actors, usecases, from));
                r.setTarget(idOfAny(actorIds, ucIds, actors, usecases, to));
                r.setType(type);
                relations.add(r);
                continue;
            }

            return DslParseResult.fail(lineNo, line, USECASE_HINT,
                    "第 " + lineNo + " 行无法识别：参与者写「(学生): 用例1, 用例2」，关系写「A -include-> B」"
                            + suspiciousChars(lines[i]));
        }

        if (actors.isEmpty() && usecases.isEmpty()) {
            return DslParseResult.fail(0, "", USECASE_HINT, "请至少写一个参与者，如「(学生): 提交论文」");
        }
        cfg.setActors(actors);
        cfg.setUsecases(usecases);
        cfg.setRelations(relations);
        return DslParseResult.success(cfg);
    }

    private String actorId(java.util.Map<String, String> index, List<UseCaseConfig.ActorConfig> actors, String name) {
        String id = index.get(name);
        if (id == null) {
            UseCaseConfig.ActorConfig a = new UseCaseConfig.ActorConfig();
            a.setId("A" + (actors.size() + 1));
            a.setName(name);
            actors.add(a);
            index.put(name, a.getId());
            id = a.getId();
        }
        return id;
    }

    private String ucId(java.util.Map<String, String> index, List<UseCaseConfig.UseCaseConfigItem> usecases, String name) {
        String id = index.get(name);
        if (id == null) {
            UseCaseConfig.UseCaseConfigItem u = new UseCaseConfig.UseCaseConfigItem();
            u.setId("U" + (usecases.size() + 1));
            u.setName(name);
            usecases.add(u);
            index.put(name, u.getId());
            id = u.getId();
        }
        return id;
    }

    /** 关系端点可能是参与者, 也可能是用例: 已声明的优先按参与者, 其余当作用例 */
    private String idOfAny(java.util.Map<String, String> actorIds, java.util.Map<String, String> ucIds,
                           List<UseCaseConfig.ActorConfig> actors, List<UseCaseConfig.UseCaseConfigItem> usecases,
                           String name) {
        if (actorIds.containsKey(name)) {
            return actorIds.get(name);
        }
        if (ucIds.containsKey(name)) {
            return ucIds.get(name);
        }
        return ucId(ucIds, usecases, name);
    }

    private String renderUseCase(UseCaseConfig c) {
        java.util.Map<String, String> actorName = new java.util.HashMap<>();
        java.util.Map<String, String> ucName = new java.util.HashMap<>();
        if (c.getActors() != null) {
            for (UseCaseConfig.ActorConfig a : c.getActors()) {
                if (a.getId() != null) actorName.put(a.getId(), a.getName());
            }
        }
        if (c.getUsecases() != null) {
            for (UseCaseConfig.UseCaseConfigItem u : c.getUsecases()) {
                if (u.getId() != null) ucName.put(u.getId(), u.getName());
            }
        }
        StringBuilder sb = new StringBuilder();
        if (c.getSystem() != null && !c.getSystem().isBlank()) {
            sb.append("@title ").append(c.getSystem()).append('\n');
        }

        List<UseCaseConfig.RelationConfig> relations = c.getRelations() == null ? List.of() : c.getRelations();
        java.util.Set<Integer> consumed = new java.util.LinkedHashSet<>();

        // 参与者行: 该参与者的关联用例合并成一行
        if (c.getActors() != null) {
            for (UseCaseConfig.ActorConfig a : c.getActors()) {
                if (a.getName() == null || a.getName().isBlank()) continue;
                List<String> linked = new ArrayList<>();
                for (int i = 0; i < relations.size(); i++) {
                    UseCaseConfig.RelationConfig r = relations.get(i);
                    if (a.getId() != null && a.getId().equals(r.getSource())
                            && "association".equalsIgnoreCase(String.valueOf(r.getType()))) {
                        String name = ucName.get(r.getTarget());
                        if (name != null && !name.isBlank()) {
                            linked.add(name);
                            consumed.add(i);
                        }
                    }
                }
                sb.append('(').append(a.getName()).append(')');
                if (!linked.isEmpty()) {
                    sb.append(": ").append(String.join(", ", linked));
                }
                sb.append('\n');
            }
        }

        // 其余关系: include / extend 用专用记号, 其它按关联
        for (int i = 0; i < relations.size(); i++) {
            if (consumed.contains(i)) continue;
            UseCaseConfig.RelationConfig r = relations.get(i);
            String from = actorName.containsKey(r.getSource()) ? actorName.get(r.getSource()) : ucName.get(r.getSource());
            String to = actorName.containsKey(r.getTarget()) ? actorName.get(r.getTarget()) : ucName.get(r.getTarget());
            if (from == null || to == null) continue;
            String type = String.valueOf(r.getType()).toLowerCase();
            String arrow = "include".equals(type) ? " -include-> " : ("extend".equals(type) ? " -extend-> " : " -> ");
            sb.append(from).append(arrow).append(to).append('\n');
        }
        return sb.toString();
    }

    // ==================== 泳道图 / 活动图(共用「泳道块 + 连线」骨架) ====================

    /** 泳道块的中间结果: 两种图只差在各自的 Config 与节点类型词表 */
    private static final class LaneParse {
        String title;
        final List<String> laneNames = new ArrayList<>();
        final List<String> nodeNames = new ArrayList<>();
        final List<Integer> nodeLaneIdx = new ArrayList<>();
        /** 每条边: {from, to, label} */
        final List<String[]> edges = new ArrayList<>();
        DslParseResult fail;
    }

    private LaneParse readLanes(String text, String hint, boolean laneRequired) {
        LaneParse r = new LaneParse();
        int currentLane = -1;
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
                r.title = title.group(1);
                continue;
            }
            // 泳道: 顶格且以冒号结尾
            if (indent == 0 && (line.endsWith(":") || line.endsWith("："))) {
                String name = cleanName(line.substring(0, line.length() - 1));
                if (name.isEmpty()) {
                    r.fail = DslParseResult.fail(lineNo, line, hint, "第 " + lineNo + " 行泳道名是空的");
                    return r;
                }
                if (r.laneNames.contains(name)) {
                    r.fail = DslParseResult.fail(lineNo, line, hint, "第 " + lineNo + " 行泳道「" + name + "」重复了");
                    return r;
                }
                r.laneNames.add(name);
                currentLane = r.laneNames.size() - 1;
                continue;
            }
            // 缩进: 泳道内的节点
            if (indent > 0) {
                if (currentLane < 0) {
                    if (laneRequired) {
                        r.fail = DslParseResult.fail(lineNo, line, hint,
                                "第 " + lineNo + " 行的节点没有归属：请先写「泳道名:」再缩进写节点");
                        return r;
                    }
                    // 活动图允许不分泳道, 隐式补一条
                    r.laneNames.add("主流程");
                    currentLane = 0;
                }
                r.nodeNames.add(line);
                r.nodeLaneIdx.add(currentLane);
                continue;
            }
            // 顶格连线
            Matcher m = FLOW_EDGE.matcher(line);
            if (m.matches()) {
                String from = cleanName(stripBrackets(m.group(1)));
                String to = cleanName(stripBrackets(m.group(3)));
                String label = m.group(4) == null ? "" : cleanName(m.group(4));
                r.edges.add(new String[]{from, to, label});
                continue;
            }
            r.fail = DslParseResult.fail(lineNo, line, hint,
                    "第 " + lineNo + " 行无法识别：泳道要写成「名字:」，节点要缩进，连线写「A -> B」"
                            + suspiciousChars(raw));
            return r;
        }
        if (laneRequired && r.laneNames.isEmpty()) {
            r.fail = DslParseResult.fail(0, "", hint, "请至少写一条泳道，如「学生:」并缩进写节点");
            return r;
        }
        if (r.laneNames.isEmpty() && r.nodeNames.isEmpty() && r.edges.isEmpty()) {
            r.fail = DslParseResult.fail(0, "", hint, "请先写点内容，如「开始 -> 提交申请」");
        }
        return r;
    }

    private String swimlaneNodeType(String name) {
        String t = inferFlowType(name, name.contains("?") || name.contains("？"));
        switch (t) {
            case "condition":
                return "gateway";
            case "start":
                return "start";
            case "end":
                return "end";
            default:
                return "task";
        }
    }

    private String activityNodeType(String name) {
        String t = inferFlowType(name, name.contains("?") || name.contains("？"));
        switch (t) {
            case "condition":
                return "DECISION";
            case "start":
                return "START";
            case "end":
                return "END";
            default:
                return "ACTION";
        }
    }

    private DslParseResult parseSwimlane(String text) {
        LaneParse p = readLanes(text, SWIMLANE_HINT, true);
        if (p.fail != null) return p.fail;

        SwimlaneConfig cfg = new SwimlaneConfig();
        cfg.setFlowName(p.title);
        java.util.Map<String, String> laneIds = new java.util.LinkedHashMap<>();
        for (int i = 0; i < p.laneNames.size(); i++) {
            SwimlaneConfig.LaneConfig lane = new SwimlaneConfig.LaneConfig();
            lane.setId("L" + (i + 1));
            lane.setName(p.laneNames.get(i));
            laneIds.put(lane.getName(), lane.getId());
            cfg.getLanes().add(lane);
        }
        java.util.Map<String, String> nodeIds = new java.util.LinkedHashMap<>();
        for (int i = 0; i < p.nodeNames.size(); i++) {
            String name = p.nodeNames.get(i);
            if (nodeIds.containsKey(name)) {
                return DslParseResult.fail(0, name, SWIMLANE_HINT, "节点「" + name + "」重复出现，同一张图里节点名要唯一");
            }
            SwimlaneConfig.NodeConfig node = new SwimlaneConfig.NodeConfig();
            node.setId("N" + (i + 1));
            node.setName(name);
            node.setType(swimlaneNodeType(name));
            node.setLaneId(laneIds.get(p.laneNames.get(p.nodeLaneIdx.get(i))));
            nodeIds.put(name, node.getId());
            cfg.getNodes().add(node);
        }
        for (String[] e : p.edges) {
            String fid = nodeIds.get(e[0]);
            String tid = nodeIds.get(e[1]);
            if (fid == null || tid == null) {
                return DslParseResult.fail(0, e[0] + " -> " + e[1], SWIMLANE_HINT,
                        "连线「" + e[0] + " -> " + e[1] + "」里有节点没有出现在任何泳道下");
            }
            SwimlaneConfig.EdgeConfig edge = new SwimlaneConfig.EdgeConfig();
            edge.setSource(fid);
            edge.setTarget(tid);
            edge.setLabel(e[2].isEmpty() ? null : e[2]);
            cfg.getEdges().add(edge);
        }
        return DslParseResult.success(cfg);
    }

    private DslParseResult parseActivity(String text) {
        LaneParse p = readLanes(text, ACTIVITY_HINT, false);
        if (p.fail != null) return p.fail;

        ActivityConfig cfg = new ActivityConfig();
        cfg.setTitle(p.title);
        // 没写泳道时给一条默认泳道(活动图本来就不要求分泳道)
        if (p.laneNames.isEmpty()) {
            ActivityConfig.LaneConfig lane = new ActivityConfig.LaneConfig();
            lane.setId("L1");
            lane.setName("主流程");
            cfg.getLanes().add(lane);
        } else {
            for (int i = 0; i < p.laneNames.size(); i++) {
                ActivityConfig.LaneConfig lane = new ActivityConfig.LaneConfig();
                lane.setId("L" + (i + 1));
                lane.setName(p.laneNames.get(i));
                cfg.getLanes().add(lane);
            }
        }
        String defaultLaneId = cfg.getLanes().get(0).getId();
        java.util.Map<String, String> nodeIds = new java.util.LinkedHashMap<>();
        for (int i = 0; i < p.nodeNames.size(); i++) {
            String name = p.nodeNames.get(i);
            if (nodeIds.containsKey(name)) {
                return DslParseResult.fail(0, name, ACTIVITY_HINT, "节点「" + name + "」重复出现，同一张图里节点名要唯一");
            }
            ActivityConfig.NodeConfig node = new ActivityConfig.NodeConfig();
            node.setId("N" + (i + 1));
            node.setText(name);
            node.setType(activityNodeType(name));
            node.setLaneId(p.laneNames.isEmpty()
                    ? defaultLaneId
                    : cfg.getLanes().get(p.nodeLaneIdx.get(i)).getId());
            nodeIds.put(name, node.getId());
            cfg.getNodes().add(node);
        }
        // 活动图可以像流程图那样只写连线(节点由连线引入), 缺的节点在这里补出来
        for (String[] e : p.edges) {
            for (String name : new String[]{e[0], e[1]}) {
                if (name.isEmpty() || nodeIds.containsKey(name)) continue;
                ActivityConfig.NodeConfig node = new ActivityConfig.NodeConfig();
                node.setId("N" + (cfg.getNodes().size() + 1));
                node.setText(name);
                node.setType(activityNodeType(name));
                node.setLaneId(defaultLaneId);
                nodeIds.put(name, node.getId());
                cfg.getNodes().add(node);
            }
        }
        for (String[] e : p.edges) {
            String fid = nodeIds.get(e[0]);
            String tid = nodeIds.get(e[1]);
            if (fid == null || tid == null) {
                return DslParseResult.fail(0, e[0] + " -> " + e[1], ACTIVITY_HINT,
                        "连线「" + e[0] + " -> " + e[1] + "」里有节点没有出现在任何泳道下");
            }
            ActivityConfig.EdgeConfig edge = new ActivityConfig.EdgeConfig();
            edge.setSource(fid);
            edge.setTarget(tid);
            edge.setLabel(e[2].isEmpty() ? null : e[2]);
            cfg.getEdges().add(edge);
        }
        return DslParseResult.success(cfg);
    }

    /** 泳道块 + 连线的公共渲染: 两种图的文本形态完全一样, 只是数据源不同 */
    private String renderLaneBlocks(String title, List<String[]> laneNameAndId,
                                    List<String[]> nodeNameAndLaneId, List<String[]> edges) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) {
            sb.append("@title ").append(title).append('\n');
        }
        for (String[] lane : laneNameAndId) {
            sb.append(lane[1]).append(":\n");
            for (String[] node : nodeNameAndLaneId) {
                if (node[1].equals(lane[0])) {
                    sb.append("  ").append(node[0]).append('\n');
                }
            }
        }
        for (String[] e : edges) {
            sb.append(e[0]).append(" -> ").append(e[1]);
            if (e[2] != null && !e[2].isBlank()) {
                sb.append(": ").append(e[2]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String renderSwimlane(SwimlaneConfig c) {
        java.util.Map<String, String> nodeName = new java.util.HashMap<>();
        if (c.getNodes() != null) {
            for (SwimlaneConfig.NodeConfig n : c.getNodes()) {
                if (n.getId() != null) nodeName.put(n.getId(), n.getName());
            }
        }
        List<String[]> lanes = new ArrayList<>();
        if (c.getLanes() != null) {
            for (SwimlaneConfig.LaneConfig l : c.getLanes()) {
                if (l.getId() != null && l.getName() != null) lanes.add(new String[]{l.getId(), l.getName()});
            }
        }
        List<String[]> nodes = new ArrayList<>();
        if (c.getNodes() != null) {
            for (SwimlaneConfig.NodeConfig n : c.getNodes()) {
                if (n.getName() != null && n.getLaneId() != null) nodes.add(new String[]{n.getName(), n.getLaneId()});
            }
        }
        List<String[]> edges = new ArrayList<>();
        if (c.getEdges() != null) {
            for (SwimlaneConfig.EdgeConfig e : c.getEdges()) {
                String from = nodeName.get(e.getSource());
                String to = nodeName.get(e.getTarget());
                if (from != null && to != null) edges.add(new String[]{from, to, e.getLabel()});
            }
        }
        return renderLaneBlocks(c.getFlowName(), lanes, nodes, edges);
    }

    private String renderActivity(ActivityConfig c) {
        java.util.Map<String, String> nodeName = new java.util.HashMap<>();
        if (c.getNodes() != null) {
            for (ActivityConfig.NodeConfig n : c.getNodes()) {
                if (n.getId() != null) nodeName.put(n.getId(), n.getText());
            }
        }
        List<String[]> lanes = new ArrayList<>();
        if (c.getLanes() != null) {
            for (ActivityConfig.LaneConfig l : c.getLanes()) {
                if (l.getId() != null && l.getName() != null) lanes.add(new String[]{l.getId(), l.getName()});
            }
        }
        List<String[]> nodes = new ArrayList<>();
        if (c.getNodes() != null) {
            for (ActivityConfig.NodeConfig n : c.getNodes()) {
                if (n.getText() != null && n.getLaneId() != null) nodes.add(new String[]{n.getText(), n.getLaneId()});
            }
        }
        List<String[]> edges = new ArrayList<>();
        if (c.getEdges() != null) {
            for (ActivityConfig.EdgeConfig e : c.getEdges()) {
                String from = nodeName.get(e.getSource());
                String to = nodeName.get(e.getTarget());
                if (from != null && to != null) edges.add(new String[]{from, to, e.getLabel()});
            }
        }
        return renderLaneBlocks(c.getTitle(), lanes, nodes, edges);
    }

    // ==================== ER 图 ====================

    private static final Pattern ER_ENTITY = Pattern.compile("^(.+?)\\s*(?:\\(([^)]*)\\))?\\s*$");
    private static final Pattern ER_RELATION = Pattern.compile(
            "^(.+?)\\s+([0-9NnMm*]+)\\s*-{1,2}\\s*([0-9NnMm*]+)\\s+(.+?)(?:\\s*[:：]\\s*(.*))?$");

    private DslParseResult parseEr(String text) {
        ErDTO cfg = new ErDTO();
        java.util.Map<String, ErDTO.ErEntity> byName = new java.util.LinkedHashMap<>();
        java.util.List<ErDTO.ErRelation> relations = new ArrayList<>();

        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            String line = normalizeLine(lines[i]).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("@title")) {
                return DslParseResult.fail(lineNo, line, ER_HINT,
                        "ER 图暂时没有标题字段，请去掉 @title（保存时用图名区分）");
            }
            Matcher rel = ER_RELATION.matcher(line);
            if (line.contains("--") && rel.matches()) {
                String from = cleanName(rel.group(1));
                String to = cleanName(rel.group(4));
                if (from.isEmpty() || to.isEmpty()) {
                    return DslParseResult.fail(lineNo, line, ER_HINT, "第 " + lineNo + " 行关系两侧都要有实体名");
                }
                entityOf(byName, cfg, from);
                entityOf(byName, cfg, to);
                ErDTO.ErRelation r = new ErDTO.ErRelation();
                r.setFrom(from);
                r.setTo(to);
                r.setCardinality(cardOf(rel.group(2)) + ":" + cardOf(rel.group(3)));
                String label = rel.group(5) == null ? "" : cleanName(rel.group(5));
                r.setLabel(label.isEmpty() ? null : label);
                relations.add(r);
                continue;
            }
            Matcher ent = ER_ENTITY.matcher(line);
            if (ent.matches()) {
                String name = cleanName(ent.group(1));
                if (name.isEmpty()) {
                    return DslParseResult.fail(lineNo, line, ER_HINT, "第 " + lineNo + " 行缺少实体名");
                }
                ErDTO.ErEntity entity = entityOf(byName, cfg, name);
                String attrRaw = ent.group(2);
                if (attrRaw != null && !attrRaw.isBlank()) {
                    for (String part : attrRaw.split("[,，]")) {
                        String a = cleanName(part);
                        if (a.isEmpty()) continue;
                        boolean key = a.endsWith("*");
                        if (key) a = cleanName(a.substring(0, a.length() - 1));
                        ErDTO.ErAttr attr = new ErDTO.ErAttr();
                        attr.setName(a);
                        attr.setKey(key);
                        entity.getAttrs().add(attr);
                    }
                }
                continue;
            }
            return DslParseResult.fail(lineNo, line, ER_HINT,
                    "第 " + lineNo + " 行无法识别：实体写「实体名 (属性1, 属性2*)」，关系写「A 1--N B」"
                            + suspiciousChars(lines[i]));
        }

        if (cfg.getEntities().isEmpty()) {
            return DslParseResult.fail(0, "", ER_HINT, "请至少写一个实体，如「学生 (学号*, 姓名)」");
        }
        cfg.setRelations(relations);
        return DslParseResult.success(cfg);
    }

    private String cardOf(String token) {
        String t = token.trim();
        if (t.equals("*")) return "N";
        return t.toUpperCase();
    }

    private ErDTO.ErEntity entityOf(java.util.Map<String, ErDTO.ErEntity> byName, ErDTO cfg, String name) {
        ErDTO.ErEntity e = byName.get(name);
        if (e == null) {
            e = new ErDTO.ErEntity();
            e.setName(name);
            byName.put(name, e);
            cfg.getEntities().add(e);
        }
        return e;
    }

    private String renderEr(ErDTO c) {
        StringBuilder sb = new StringBuilder();
        if (c.getEntities() != null) {
            for (ErDTO.ErEntity e : c.getEntities()) {
                if (e.getName() == null || e.getName().isBlank()) continue;
                sb.append(e.getName());
                if (e.getAttrs() != null && !e.getAttrs().isEmpty()) {
                    List<String> parts = new ArrayList<>();
                    for (ErDTO.ErAttr a : e.getAttrs()) {
                        if (a.getName() == null || a.getName().isBlank()) continue;
                        parts.add(Boolean.TRUE.equals(a.getKey()) ? a.getName() + "*" : a.getName());
                    }
                    if (!parts.isEmpty()) {
                        sb.append(" (").append(String.join(", ", parts)).append(')');
                    }
                }
                sb.append('\n');
            }
        }
        if (c.getRelations() != null) {
            for (ErDTO.ErRelation r : c.getRelations()) {
                if (r.getFrom() == null || r.getTo() == null) continue;
                String card = r.getCardinality() == null || r.getCardinality().isBlank() ? "1:N" : r.getCardinality().trim();
                String[] two = card.split(":");
                String left = two.length > 0 ? two[0] : "1";
                String right = two.length > 1 ? two[1] : "N";
                sb.append(r.getFrom()).append(' ').append(left).append("--").append(right).append(' ').append(r.getTo());
                if (r.getLabel() != null && !r.getLabel().isBlank()) {
                    sb.append(": ").append(r.getLabel().trim());
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
