package com.graduate.thesis.service;

import com.graduate.thesis.dto.ArchitectureConfig;
import com.graduate.thesis.dto.DiagramEdge;
import com.graduate.thesis.dto.DiagramNode;
import com.graduate.thesis.dto.DiagramVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 架构规则引擎: 根据用户结构化配置, 按规则自动生成系统架构图的节点与连线
 *
 * 规则:
 *   - 存在前端   -> 生成前端节点(WEB)
 *   - 存在后端   -> 生成 N 个服务节点(SERVICE)
 *   - 存在数据库 -> 生成数据库节点(DATABASE), 集群生成主/从两个
 *   - 存在缓存/消息队列/部署 -> 生成对应节点
 *   - 连线: 前端->服务(HTTP), 服务->数据库(JDBC), 服务->缓存, 服务->消息队列, 服务->部署
 */
@Service
public class ArchitectureRuleEngine {

    private int seq = 0;

    /** 根据组件名关键词自动推断形状 */
    private String shapeOf(String name) {
        if (name == null) {
            return "service";
        }
        String n = name.trim().toLowerCase();
        if (n.contains("mysql") || n.contains("oracle") || n.contains("mongodb") || n.contains("postgres")
                || n.contains("数据库") || n.contains("db")) {
            return "database";
        }
        if (n.contains("redis") || n.contains("memcached") || n.contains("缓存")) {
            return "cache";
        }
        if (n.contains("nginx") || n.contains("网关") || n.contains("gateway") || n.contains("kong")) {
            return "gateway";
        }
        if (n.contains("vue") || n.contains("react") || n.contains("小程序") || n.contains("app")
                || n.contains("h5") || n.contains("前端") || n.contains("网页") || n.contains("web")) {
            return "web";
        }
        // 其余按服务处理
        return "service";
    }

    public DiagramVO build(ArchitectureConfig config) {
        if (config == null) {
            config = new ArchitectureConfig();
        }
        seq = 0;
        DiagramVO vo = new DiagramVO();
        vo.setType("ARCH");
        List<DiagramNode> nodes = vo.getNodes();

        // 按层生成节点: 每层一个 lane, 层内组件横向排列
        if (config.getLayers() != null) {
            for (ArchitectureConfig.LayerConfig layer : config.getLayers()) {
                if (layer.getName() == null || layer.getName().trim().isEmpty()) {
                    continue;
                }
                String lane = layer.getName().trim();
                if (layer.getComponents() == null) {
                    continue;
                }
                for (ArchitectureConfig.ComponentConfig c : layer.getComponents()) {
                    if (c.getName() == null || c.getName().trim().isEmpty()) {
                        continue;
                    }
                    DiagramNode n = node(c.getName().trim(), shapeOf(c.getName()), lane);
                    nodes.add(n);
                }
            }
        }

        // 布局
        layout(nodes, vo);
        vo.setName(nvlStr(config.getSystemName()));
        return vo;
    }

    /** 全连: a 层每个 -> b 层每个 */
    private void connectAll(List<DiagramEdge> edges, List<String> a, List<String> b, String label) {
        if (a.isEmpty() || b.isEmpty()) return;
        for (String x : a) {
            for (String y : b) {
                edges.add(edge(x, y, label));
            }
        }
    }

    /** 按层布局: 层纵向排列(保持输入顺序), 层内水平居中 */
    private void layout(List<DiagramNode> nodes, DiagramVO vo) {
        Map<String, List<DiagramNode>> layers = new LinkedHashMap<>();
        for (DiagramNode n : nodes) {
            layers.computeIfAbsent(n.getLane(), k -> new ArrayList<>()).add(n);
        }
        int nodeH = 56, gapX = 70, gapY = 120;
        int rowW = 180;
        int maxW = 0;
        for (List<DiagramNode> list : layers.values()) {
            int w = list.size() * rowW + (list.size() - 1) * gapX;
            maxW = Math.max(maxW, w);
        }
        int x0 = 100;
        int y = 80;
        for (List<DiagramNode> list : layers.values()) {
            int w = list.size() * rowW + (list.size() - 1) * gapX;
            int x = x0 + (maxW - w) / 2;
            for (DiagramNode n : list) {
                n.setX(x);
                n.setY(y);
                x += rowW + gapX;
            }
            y += nodeH + gapY;
        }
        vo.setWidth(maxW + x0 * 2);
        vo.setHeight(y + 40);
    }

    private DiagramNode node(String label, String shape, String lane) {
        DiagramNode n = new DiagramNode();
        n.setId("n" + (seq++));
        n.setLabel(label);
        n.setShape(shape);
        n.setLane(lane);
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

    /** 多个同类型节点时加序号: SpringBoot, SpringBoot2... */
    private String label(String type, int count, int i) {
        if (count <= 1) {
            return type;
        }
        return type + (i + 1);
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private int nvl(Integer v) {
        return v == null ? 1 : v;
    }

    private String nvlStr(String s) {
        return s == null ? "" : s;
    }
}
