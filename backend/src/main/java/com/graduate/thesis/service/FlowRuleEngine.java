package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.DiagramEdge;
import com.graduate.thesis.dto.DiagramNode;
import com.graduate.thesis.dto.DiagramVO;
import com.graduate.thesis.dto.FlowConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程图规则引擎: 按拓扑层级纵向排布, 判断节点分出多分支。
 *
 * <p>布局只用 rank(层级) 决定行、按行内序号决定列; 前端 FLOW 还会再走一遍 Dagre，
 * 所以这里的位置是兜底而不是最终值 —— 关键是节点类型与连线标签要正确。</p>
 */
@Service
public class FlowRuleEngine {

    private static final int NODE_H = 52;
    private static final int GAP_Y = 74;
    private static final int GAP_X = 56;
    private static final int MARGIN_TOP = 60;

    public DiagramVO build(FlowConfig config) {
        if (config == null) {
            throw new BusinessException(400, "请配置流程图");
        }
        List<FlowConfig.Node> nodes = validNodes(config.getNodes());
        List<FlowConfig.Edge> edges = validEdges(config.getEdges(), nodes);
        if (nodes.isEmpty()) {
            throw new BusinessException(400, "请至少配置一个节点");
        }

        DiagramVO vo = new DiagramVO();
        vo.setType("FLOW");
        vo.setName(config.getTitle() == null || config.getTitle().isBlank() ? "流程图" : config.getTitle().trim());

        // 1. 层级
        Map<String, Integer> rank = topoRank(nodes, edges);
        int maxRank = rank.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // 2. 每行节点宽度与行宽, 行内居中
        Map<Integer, List<FlowConfig.Node>> byRank = new LinkedHashMap<>();
        for (int r = 0; r <= maxRank; r++) {
            byRank.put(r, new ArrayList<>());
        }
        for (FlowConfig.Node n : nodes) {
            byRank.get(rank.getOrDefault(n.getId(), maxRank)).add(n);
        }
        Map<String, Integer> widthOf = new HashMap<>();
        int maxRowWidth = 0;
        for (List<FlowConfig.Node> row : byRank.values()) {
            int w = 0;
            for (FlowConfig.Node n : row) {
                int nw = nodeWidth(n);
                widthOf.put(n.getId(), nw);
                w += nw + GAP_X;
            }
            maxRowWidth = Math.max(maxRowWidth, Math.max(0, w - GAP_X));
        }

        // 3. 摆放
        double canvasW = Math.max(360, maxRowWidth + 120);
        for (Map.Entry<Integer, List<FlowConfig.Node>> e : byRank.entrySet()) {
            List<FlowConfig.Node> row = e.getValue();
            int rowW = 0;
            for (FlowConfig.Node n : row) {
                rowW += widthOf.get(n.getId()) + GAP_X;
            }
            rowW = Math.max(0, rowW - GAP_X);
            double x = (canvasW - rowW) / 2.0;
            double y = MARGIN_TOP + e.getKey() * (NODE_H + GAP_Y);
            for (FlowConfig.Node n : row) {
                DiagramNode dn = new DiagramNode();
                dn.setId(n.getId());
                dn.setLabel(n.getName());
                dn.setShape(n.getType());
                dn.setX(x);
                dn.setY(y);
                dn.setWidth(widthOf.get(n.getId()));
                dn.setHeight("condition".equalsIgnoreCase(n.getType()) ? NODE_H + 16 : NODE_H);
                vo.getNodes().add(dn);
                x += widthOf.get(n.getId()) + GAP_X;
            }
        }

        // 4. 连线
        for (FlowConfig.Edge e : edges) {
            DiagramEdge de = new DiagramEdge();
            de.setId("e_" + e.getFrom() + "_" + e.getTo());
            de.setSource(e.getFrom());
            de.setTarget(e.getTo());
            de.setLabel(e.getLabel() == null || e.getLabel().isBlank() ? null : e.getLabel().trim());
            vo.getEdges().add(de);
        }

        int rows = maxRank + 1;
        vo.setWidth((int) Math.round(canvasW));
        vo.setHeight(MARGIN_TOP + rows * (NODE_H + GAP_Y) + 40);
        return vo;
    }

    private int nodeWidth(FlowConfig.Node n) {
        int w = Math.max(140, n.getName().length() * 16 + 40);
        return "condition".equalsIgnoreCase(n.getType()) ? Math.max(w, 170) : w;
    }

    private List<FlowConfig.Node> validNodes(List<FlowConfig.Node> nodes) {
        List<FlowConfig.Node> out = new ArrayList<>();
        if (nodes == null) return out;
        int i = 0;
        for (FlowConfig.Node n : nodes) {
            if (n.getName() == null || n.getName().trim().isEmpty()) continue;
            if (n.getId() == null || n.getId().trim().isEmpty()) n.setId("n" + (i + 1));
            n.setName(n.getName().trim());
            if (n.getType() == null || n.getType().trim().isEmpty()) n.setType("action");
            out.add(n);
            i++;
        }
        return out;
    }

    private List<FlowConfig.Edge> validEdges(List<FlowConfig.Edge> edges, List<FlowConfig.Node> nodes) {
        List<FlowConfig.Edge> out = new ArrayList<>();
        if (edges == null) return out;
        Map<String, Boolean> ids = new HashMap<>();
        for (FlowConfig.Node n : nodes) ids.put(n.getId(), true);
        for (FlowConfig.Edge e : edges) {
            if (e.getFrom() == null || e.getTo() == null) continue;
            if (!ids.containsKey(e.getFrom()) || !ids.containsKey(e.getTo())) continue;
            if (e.getFrom().equals(e.getTo())) continue;
            out.add(e);
        }
        return out;
    }

    /** 拓扑排序: 按边分配层级; 孤立/成环节点接在最后 */
    private Map<String, Integer> topoRank(List<FlowConfig.Node> nodes, List<FlowConfig.Edge> edges) {
        Map<String, Integer> rank = new HashMap<>();
        Map<String, Integer> indeg = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (FlowConfig.Node n : nodes) {
            indeg.put(n.getId(), 0);
            adj.put(n.getId(), new ArrayList<>());
        }
        for (FlowConfig.Edge e : edges) {
            if (!adj.containsKey(e.getFrom()) || !adj.containsKey(e.getTo())) continue;
            adj.get(e.getFrom()).add(e.getTo());
            indeg.put(e.getTo(), indeg.get(e.getTo()) + 1);
        }
        Deque<String> q = new ArrayDeque<>();
        for (FlowConfig.Node n : nodes) {
            if (indeg.get(n.getId()) == 0) q.add(n.getId());
        }
        int r = 0;
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                String id = q.poll();
                rank.put(id, r);
                for (String t : adj.get(id)) {
                    indeg.put(t, indeg.get(t) - 1);
                    if (indeg.get(t) == 0) q.add(t);
                }
            }
            r++;
        }
        int extra = r;
        for (FlowConfig.Node n : nodes) {
            if (!rank.containsKey(n.getId())) rank.put(n.getId(), extra++);
        }
        return rank;
    }
}
