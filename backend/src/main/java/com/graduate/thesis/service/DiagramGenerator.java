package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.DiagramEdge;
import com.graduate.thesis.dto.DiagramNode;
import com.graduate.thesis.dto.DiagramVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则驱动的系统设计图自动生成器:
 * 输入自然语言描述 + 图类型 → 关键词解析 → 生成图模型 → 自动布局
 *
 * 支持: FLOW(流程图) / ARCH(系统架构图) / SWIMLANE(泳道图)
 */
@Service
public class DiagramGenerator {

    private final FlowParser flowParser;

    public DiagramGenerator(FlowParser flowParser) {
        this.flowParser = flowParser;
    }

    // 人物(actor)
    private static final String[] ACTORS = {"用户", "管理员", "员工", "客户", "商户", "客服", "商家", "学生", "教师"};
    // 客户端组件(client)
    private static final String[] CLIENTS = {"前端", "小程序", "客户端", "APP", "App", "网页", "网关", "界面"};
    // 服务组件(service)
    private static final String[] SERVICES = {"服务", "接口", "后台", "系统", "服务器", "应用"};
    // 存储(database)
    private static final String[] DATABASES = {"MySQL", "Redis", "MongoDB", "Oracle", "数据库", "缓存", "消息队列"};
    // 动作(action)
    private static final String[] ACTIONS = {"登录", "注册", "查询", "新增", "修改", "删除", "支付", "审核", "提交",
            "验证", "保存", "发送", "扣减", "创建", "充值", "下单", "结算", "调用", "查询", "校验", "生成"};
    // 关系连接词(识别"谁调用谁")
    private static final String[] CONNECTION_WORDS = {"通过", "访问", "调用", "查询", "连接", "依赖", "读写",
            "更新", "存储", "保存", "返回", "写入", "读取", "请求", "发送", "投递", "搜索", "订阅", "推送"};

    private int seq = 0;

    public DiagramVO generate(String type, String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new BusinessException(400, "请输入系统描述");
        }
        seq = 0;
        List<String> clauses = split(description);
        DiagramModel model;
        String t = type == null ? "FLOW" : type.toUpperCase();
        switch (t) {
            case "ARCH":
                model = parseArch(clauses);
                break;
            case "SWIMLANE":
                model = parseSwimlane(clauses);
                break;
            default:
                t = "FLOW";
                // 含 if( 分支语法的流程脚本走 DSL 解析器
                if (description != null && description.contains("if(")) {
                    return flowParser.parse(description);
                }
                model = parseFlow(clauses);
        }
        // 布局
        DiagramVO vo = layout(model, t);
        vo.setType(t);
        vo.setDescription(description);
        return vo;
    }

    // ============ 文本拆分 ============

    private List<String> split(String text) {
        List<String> out = new ArrayList<>();
        String[] parts = text.split("[，。；、,\n;；]");
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    // ============ 流程图解析 ============

    private DiagramModel parseFlow(List<String> clauses) {
        DiagramModel model = new DiagramModel();
        // 开始节点
        DiagramNode start = node("开始", "start");
        model.nodes.add(start);
        String prev = start.getId();
        for (String clause : clauses) {
            DiagramNode n = node(clause, "action");
            model.nodes.add(n);
            model.edges.add(edge(prev, n.getId(), ""));
            prev = n.getId();
        }
        DiagramNode end = node("结束", "end");
        model.nodes.add(end);
        model.edges.add(edge(prev, end.getId(), ""));
        return model;
    }

    // ============ 架构图解析 ============

    /** 固定组件词库: 关键词 -> 组件类型 */
    private static final String[][] ARCH_KW = {
            {"用户", "CLIENT"}, {"管理员", "CLIENT"}, {"客户", "CLIENT"}, {"商家", "CLIENT"},
            {"小程序", "WEB"}, {"前端", "WEB"}, {"网页", "WEB"}, {"H5", "WEB"}, {"APP", "WEB"}, {"App", "WEB"},
            {"网关", "GATEWAY"}, {"Nginx", "GATEWAY"},
            {"MySQL", "DATABASE"}, {"Oracle", "DATABASE"}, {"MongoDB", "DATABASE"},
            {"PostgreSQL", "DATABASE"}, {"Postgres", "DATABASE"},
            {"Redis", "CACHE"}, {"Memcached", "CACHE"},
            {"Kafka", "MQ"}, {"RabbitMQ", "MQ"}, {"消息队列", "MQ"},
            {"Elasticsearch", "SEARCH"}, {"ES", "SEARCH"},
            {"OSS", "STORAGE"}, {"对象存储", "STORAGE"}, {"文件存储", "STORAGE"},
            {"微信", "THIRD_PARTY"}, {"支付宝", "THIRD_PARTY"}, {"第三方", "THIRD_PARTY"}
    };

    /** 专有组件名中的噪声前缀词(动词/连接词/量词), 识别后剔除 */
    private static final String[] NAME_NOISE = {"查询", "访问", "调用", "通过", "包括", "一个", "用于", "进行",
            "读取", "写入", "请求", "返回", "存储", "更新", "保存", "使用"};

    private DiagramModel parseArch(List<String> clauses) {
        DiagramModel model = new DiagramModel();
        // 1. 组件识别(type + layer 分离: type 决定长相, layer 决定位置)
        Map<String, Component> comps = recognizeComponents(clauses);
        if (comps.isEmpty()) {
            return parseFlow(clauses);
        }
        // 2. 关系识别: 从连接词句子提取依赖边
        List<Dependency> deps = new ArrayList<>();
        parseDependencies(clauses, new ArrayList<>(comps.values()), deps);
        // 3. 转 DiagramNode/Edge
        Map<Component, DiagramNode> nodeMap = new LinkedHashMap<>();
        for (Component c : comps.values()) {
            DiagramNode n = node(c.name, shapeOf(c.type));
            n.setLane(layerName(c.layer));   // lane 存"区域名"(访问层/业务层...)
            nodeMap.put(c, n);
            model.nodes.add(n);
        }
        for (Dependency d : deps) {
            DiagramNode a = nodeMap.get(d.source);
            DiagramNode b = nodeMap.get(d.target);
            if (a != null && b != null) {
                model.edges.add(edge(a.getId(), b.getId(), d.protocol));
            }
        }
        return model;
    }

    /**
     * 组件识别: 优先专有组件(xxx服务/xxx数据库/xxx缓存/xxx后台/xxx接口), 再匹配固定词库
     */
    private Map<String, Component> recognizeComponents(List<String> clauses) {
        Map<String, Component> comps = new LinkedHashMap<>();
        String[] patternWords = {"服务", "数据库", "缓存", "后台", "接口", "网关"};
        for (String clause : clauses) {
            // 专有组件: "XX服务" "XX数据库" 等(纯中文前缀 1-4 字), 后处理去掉噪声前缀
            for (String pw : patternWords) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("[\\u4e00-\\u9fa5]{1,4}" + java.util.regex.Pattern.quote(pw))
                        .matcher(clause);
                while (m.find()) {
                    String name = cleanComponentName(m.group());
                    if (name == null) {
                        continue;
                    }
                    String type = inferType(name);
                    comps.putIfAbsent(name, new Component(name, type, layerOf(type)));
                }
            }
            // 固定词库(长度降序, 避免 ES 与 Elasticsearch 等子串重复)
            List<String[]> kwSorted = new ArrayList<>(java.util.Arrays.asList(ARCH_KW));
            kwSorted.sort((a, b) -> Integer.compare(b[0].length(), a[0].length()));
            for (String[] e : kwSorted) {
                if (clause.contains(e[0])) {
                    // 若已有组件包含该词(如 Elasticsearch 已含 ES), 跳过
                    boolean exists = false;
                    for (String existing : comps.keySet()) {
                        if (existing.contains(e[0]) || e[0].contains(existing)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        String type = e[1];
                        comps.putIfAbsent(e[0], new Component(e[0], type, layerOf(type)));
                    }
                }
            }
        }
        return comps;
    }

    /**
     * 清理专有组件名: 去掉噪声词前缀(访问/查询/调用...), 并归一到固定组件词
     * 例: "访问会员服务"->"会员服务", "查询MySQL数据库"->"MySQL"
     */
    private String cleanComponentName(String name) {
        String result = name;
        boolean changed;
        do {
            changed = false;
            for (String n : NAME_NOISE) {
                int idx = result.indexOf(n);
                if (idx >= 0) {
                    result = result.substring(idx + n.length());
                    changed = true;
                }
            }
        } while (changed);
        if (result.isEmpty()) {
            return null;
        }
        // 若包含固定组件词(MySQL/Redis等), 归一到固定词
        for (String[] e : ARCH_KW) {
            if (result.contains(e[0])) {
                return e[0];
            }
        }
        return result;
    }

    /** 由组件名推断类型(专有组件名含类型词) */
    private String inferType(String name) {
        if (name.contains("数据库") || name.contains("MySQL") || name.contains("Oracle") || name.contains("MongoDB") || name.contains("Postgres")) return "DATABASE";
        if (name.contains("缓存") || name.contains("Redis") || name.contains("Memcached")) return "CACHE";
        if (name.contains("消息") || name.contains("Kafka") || name.contains("RabbitMQ") || name.contains("队列")) return "MQ";
        if (name.contains("搜索") || name.contains("ES") || name.contains("Elasticsearch")) return "SEARCH";
        if (name.contains("存储") || name.contains("OSS") || name.contains("文件")) return "STORAGE";
        if (name.contains("网关")) return "GATEWAY";
        if (name.contains("小程序") || name.contains("前端") || name.contains("网页")) return "WEB";
        if (name.contains("后台") || name.contains("接口") || name.contains("服务")) return "SERVICE";
        return "SERVICE";
    }

    /**
     * 关系识别: 对含连接词的句子, 按文本出现顺序连接组件, 并推断通信协议
     * (例: "用户通过小程序访问会员服务" -> 用户-HTTP->小程序->会员服务)
     */
    private void parseDependencies(List<String> clauses, List<Component> comps, List<Dependency> deps) {
        for (String clause : clauses) {
            boolean hasConn = false;
            for (String cw : CONNECTION_WORDS) {
                if (clause.contains(cw)) {
                    hasConn = true;
                    break;
                }
            }
            if (!hasConn) {
                continue;
            }
            List<Component> mentioned = new ArrayList<>();
            for (Component c : comps) {
                if (clause.contains(c.name)) {
                    mentioned.add(c);
                }
            }
            mentioned.sort((a, b) -> Integer.compare(clause.indexOf(a.name), clause.indexOf(b.name)));
            if (mentioned.size() >= 2) {
                String protocol = inferProtocol(clause);
                for (int i = 0; i < mentioned.size() - 1; i++) {
                    deps.add(new Dependency(mentioned.get(i), mentioned.get(i + 1), protocol));
                }
            }
        }
    }

    /** 由句子动词推断通信协议 */
    private String inferProtocol(String clause) {
        if (clause.contains("发送") || clause.contains("投递") || clause.contains("消息") || clause.contains("订阅")) return "MQ";
        if (clause.contains("查询") || clause.contains("读取") || clause.contains("读写") || clause.contains("写入") || clause.contains("存储")) return "SQL";
        if (clause.contains("调用") || clause.contains("RPC")) return "RPC";
        if (clause.contains("搜索")) return "查询";
        if (clause.contains("通过") || clause.contains("访问") || clause.contains("请求")) return "HTTP";
        return "";
    }

    /**
     * 类型 -> 层级(决定放置位置; 第三方与业务同层, 不被排最底)
     */
    private int layerOf(String type) {
        switch (type) {
            case "CLIENT": return 0;
            case "WEB": return 1;
            case "GATEWAY": return 2;
            case "SERVICE": return 3;
            case "THIRD_PARTY": return 3;   // 第三方被业务服务调用, 同层
            case "DATABASE": return 4;
            case "CACHE": return 4;
            case "MQ": return 4;
            case "SEARCH": return 4;
            case "STORAGE": return 4;
            default: return 3;
        }
    }

    /** 层级 -> 区域名(容器名) */
    private String layerName(int layer) {
        switch (layer) {
            case 0: return "用户访问层";
            case 1: return "客户端层";
            case 2: return "网关层";
            case 3: return "业务服务层";
            case 4: return "数据存储层";
            default: return "业务服务层";
        }
    }

    /** 组件类型 -> 节点形状 */
    private String shapeOf(String type) {
        switch (type) {
            case "CLIENT": return "actor";
            case "WEB": return "web";
            case "GATEWAY": return "gateway";
            case "SERVICE": return "service";
            case "DATABASE": return "database";
            case "CACHE": return "cache";
            case "MQ": return "mq";
            case "SEARCH": return "search";
            case "STORAGE": return "storage";
            case "THIRD_PARTY": return "third";
            default: return "service";
        }
    }

    /** 架构组件(内部模型) */
    private static class Component {
        final String name;
        final String type;
        final int layer;
        Component(String name, String type, int layer) {
            this.name = name;
            this.type = type;
            this.layer = layer;
        }
    }

    /** 架构依赖(内部模型) */
    private static class Dependency {
        final Component source;
        final Component target;
        final String protocol;
        Dependency(Component source, Component target, String protocol) {
            this.source = source;
            this.target = target;
            this.protocol = protocol;
        }
    }

    // ============ 泳道图解析 ============

    private DiagramModel parseSwimlane(List<String> clauses) {
        DiagramModel model = new DiagramModel();
        Map<String, List<DiagramNode>> lanes = new LinkedHashMap<>();
        Map<String, DiagramNode> seen = new LinkedHashMap<>();
        String prev = null;
        // 先识别角色泳道
        for (String clause : clauses) {
            String actor = findFirst(clause, ACTORS);
            if (actor == null) {
                actor = "系统";
            }
            DiagramNode n = uniqueNode(seen, clause, "action");
            n.setLane(actor);
            lanes.computeIfAbsent(actor, k -> new ArrayList<>()).add(n);
            model.nodes.add(n);
            if (prev != null) {
                model.edges.add(edge(prev, n.getId(), ""));
            }
            prev = n.getId();
        }
        // 若无角色则退化为流程图
        if (lanes.size() <= 1) {
            return parseFlow(clauses);
        }
        return model;
    }

    // ============ 布局 ============

    private DiagramVO layout(DiagramModel model, String type) {
        DiagramVO vo = new DiagramVO();
        int nodeW = 150, nodeH = 50;
        int gapX = 80, gapY = 80;
        if (type.equals("ARCH")) {
            layoutArch(model, vo);
        } else if (type.equals("SWIMLANE")) {
            layoutSwimlane(model, vo);
        } else {
            // FLOW: 纵向单列
            int x = 40;
            int y = 40;
            for (int i = 0; i < model.nodes.size(); i++) {
                DiagramNode n = model.nodes.get(i);
                n.setX(x);
                n.setY(y);
                y += nodeH + gapY;
            }
            vo.setWidth(300);
            vo.setHeight(y + 40);
        }
        for (DiagramNode n : model.nodes) {
            vo.getNodes().add(n);
        }
        for (DiagramEdge e : model.edges) {
            vo.getEdges().add(e);
        }
        return vo;
    }

    private void layoutArch(DiagramModel model, DiagramVO vo) {
        // 按区域分层: 用户访问层 -> 客户端层 -> 网关层 -> 业务服务层 -> 数据存储层
        String[] order = {"用户访问层", "客户端层", "网关层", "业务服务层", "数据存储层"};
        Map<String, List<DiagramNode>> layers = new LinkedHashMap<>();
        for (DiagramNode n : model.nodes) {
            String t = n.getLane() != null ? n.getLane() : "业务服务层";
            layers.computeIfAbsent(t, k -> new ArrayList<>()).add(n);
        }
        int nodeH = 56, gapX = 70, gapY = 120;
        int rowW = 180;
        // 计算每层宽度
        int maxW = 0;
        for (String t : order) {
            List<DiagramNode> list = layers.get(t);
            if (list == null) continue;
            int w = list.size() * rowW + (list.size() - 1) * gapX;
            maxW = Math.max(maxW, w);
        }
        int x0 = 100;
        int y = 80;
        for (String t : order) {
            List<DiagramNode> list = layers.get(t);
            if (list == null || list.isEmpty()) continue;
            int w = list.size() * rowW + (list.size() - 1) * gapX;
            placeRowCentered(list, x0 + (maxW - w) / 2, y, nodeH, gapX);
            y += nodeH + gapY;
        }
        vo.setWidth(maxW + x0 * 2);
        vo.setHeight(y + 40);
    }

    private void placeRowCentered(List<DiagramNode> nodes, int x0, int y0, int nodeH, int gapX) {
        int x = x0;
        for (DiagramNode n : nodes) {
            n.setX(x);
            n.setY(y0);
            x += 180 + gapX;
        }
    }

    private void layoutSwimlane(DiagramModel model, DiagramVO vo) {
        Map<String, List<DiagramNode>> lanes = new LinkedHashMap<>();
        for (DiagramNode n : model.nodes) {
            lanes.computeIfAbsent(n.getLane(), k -> new ArrayList<>()).add(n);
        }
        int laneW = 240;
        int nodeH = 50, gapY = 70;
        int laneIndex = 0;
        int maxH = 0;
        for (Map.Entry<String, List<DiagramNode>> e : lanes.entrySet()) {
            int x = 60 + laneIndex * (laneW + 40);
            int y = 80;
            for (DiagramNode n : e.getValue()) {
                n.setX(x);
                n.setY(y);
                y += nodeH + gapY;
            }
            maxH = Math.max(maxH, y);
            laneIndex++;
        }
        vo.setWidth(60 + laneIndex * (laneW + 40) + 40);
        vo.setHeight(maxH + 40);
    }

    // ============ 工具 ============

    private DiagramNode node(String label, String shape) {
        DiagramNode n = new DiagramNode();
        n.setId("n" + (seq++));
        n.setLabel(label);
        n.setShape(shape);
        return n;
    }

    private DiagramNode uniqueNode(Map<String, DiagramNode> seen, String label, String shape) {
        if (seen.containsKey(label)) {
            return seen.get(label);
        }
        DiagramNode n = node(label, shape);
        seen.put(label, n);
        return n;
    }

    private DiagramEdge edge(String src, String tgt, String label) {
        DiagramEdge e = new DiagramEdge();
        e.setId("e" + (seq++));
        e.setSource(src);
        e.setTarget(tgt);
        e.setLabel(label);
        return e;
    }

    private String findFirst(String text, String[] words) {
        for (String w : words) {
            if (text.contains(w)) {
                return w;
            }
        }
        return null;
    }

    /** 中间模型 */
    private static class DiagramModel {
        List<DiagramNode> nodes = new ArrayList<>();
        List<DiagramEdge> edges = new ArrayList<>();
    }
}
