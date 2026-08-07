package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.DiagramEdge;
import com.graduate.thesis.dto.DiagramLane;
import com.graduate.thesis.dto.DiagramNode;
import com.graduate.thesis.dto.DiagramVO;
import com.graduate.thesis.dto.SwimlaneConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 泳道图规则引擎(BPMN 模型): Lane(列) + Node(类型) + Edge(流程顺序)
 *
 * 布局规则:
 *   - 泳道横向排列为列: x = index * laneWidth, 每条泳道从上到下
 *   - 节点按拓扑排序分配 rank(流程层级), y = marginTop + rank * rowHeight
 *   - 同一 rank 的节点(并行流程)位于同一水平线
 *   - 节点类型: start / task(矩形) / gateway(菱形) / end
 */
@Service
public class SwimlaneRuleEngine {

    private int seq = 0;

    public DiagramVO build(SwimlaneConfig config) {
        if (config == null) {
            throw new BusinessException(400, "请配置泳道图");
        }
        List<SwimlaneConfig.LaneConfig> lanes = validLanes(config.getLanes());
        List<SwimlaneConfig.NodeConfig> nodes = validNodes(config.getNodes(), lanes);
        if (lanes.isEmpty()) {
            throw new BusinessException(400, "请至少配置一个泳道");
        }
        if (nodes.isEmpty()) {
            throw new BusinessException(400, "请至少配置一个节点");
        }
        seq = 0;

        DiagramVO vo = new DiagramVO();
        vo.setType("SWIMLANE");
        vo.setName(config.getFlowName() == null || config.getFlowName().trim().isEmpty()
                ? "业务流程" : config.getFlowName().trim());

        // 1. 连线: 优先用户配置(过滤无效引用), 否则按节点顺序自动连
        List<SwimlaneConfig.EdgeConfig> edges = validEdges(config.getEdges(), nodes);
        if (edges.isEmpty()) {
            for (int i = 0; i < nodes.size() - 1; i++) {
                SwimlaneConfig.EdgeConfig e = new SwimlaneConfig.EdgeConfig();
                e.setSource(nodes.get(i).getId());
                e.setTarget(nodes.get(i + 1).getId());
                edges.add(e);
            }
        }

        // 2. 拓扑排序: 节点 -> rank(流程层级)
        Map<String, Integer> rank = topoRank(nodes, edges);

        // 3. 布局参数
        int laneWidth = 260;
        int nodeW = 150, nodeH = 52;
        int gapY = 60;
        int marginTop = 90;
        int marginBottom = 60;
        int rowHeight = nodeH + gapY;
        int maxRank = rank.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int totalHeight = marginTop + (maxRank + 1) * rowHeight + marginBottom;

        // 4. 泳道 -> 列
        Map<String, List<SwimlaneConfig.NodeConfig>> byLane = new LinkedHashMap<>();
        for (SwimlaneConfig.LaneConfig lane : lanes) {
            byLane.put(lane.getId(), new ArrayList<>());
        }
        for (SwimlaneConfig.NodeConfig n : nodes) {
            byLane.get(n.getLaneId()).add(n);
        }

        int laneIndex = 0;
        for (SwimlaneConfig.LaneConfig lane : lanes) {
            double x = laneIndex * laneWidth;
            DiagramLane dl = new DiagramLane();
            dl.setId("lane" + (seq++));
            dl.setName(lane.getName());
            dl.setX(x);
            dl.setY(0);
            dl.setWidth(laneWidth);
            dl.setHeight(totalHeight);
            vo.getLanes().add(dl);

            for (SwimlaneConfig.NodeConfig n : byLane.get(lane.getId())) {
                int r = rank.get(n.getId());
                DiagramNode dn = node(n.getId(), n.getName(), shapeOf(n.getType()));
                dn.setLane(dl.getId());
                dn.setX(x + (laneWidth - nodeW) / 2.0);
                dn.setY(marginTop + r * rowHeight);
                vo.getNodes().add(dn);
            }
            laneIndex++;
        }

        // 5. 连线
        for (SwimlaneConfig.EdgeConfig e : edges) {
            vo.getEdges().add(edge(e.getSource(), e.getTarget(), e.getLabel()));
        }

        vo.setWidth(laneIndex * laneWidth);
        vo.setHeight(totalHeight);
        return vo;
    }

    /**
     * 拓扑排序: 按 edges 分配 rank; 孤立/成环节点按输入顺序续接
     */
    private Map<String, Integer> topoRank(List<SwimlaneConfig.NodeConfig> nodes,
                                          List<SwimlaneConfig.EdgeConfig> edges) {
        Map<String, Integer> rank = new HashMap<>();
        Map<String, Integer> indeg = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (SwimlaneConfig.NodeConfig n : nodes) {
            indeg.put(n.getId(), 0);
            adj.put(n.getId(), new ArrayList<>());
        }
        for (SwimlaneConfig.EdgeConfig e : edges) {
            if (!adj.containsKey(e.getSource()) || !adj.containsKey(e.getTarget())) {
                continue;
            }
            adj.get(e.getSource()).add(e.getTarget());
            indeg.put(e.getTarget(), indeg.get(e.getTarget()) + 1);
        }
        Deque<String> q = new ArrayDeque<>();
        for (SwimlaneConfig.NodeConfig n : nodes) {
            if (indeg.get(n.getId()) == 0) {
                q.add(n.getId());
            }
        }
        int r = 0;
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                String id = q.poll();
                rank.put(id, r);
                for (String t : adj.get(id)) {
                    indeg.put(t, indeg.get(t) - 1);
                    if (indeg.get(t) == 0) {
                        q.add(t);
                    }
                }
            }
            r++;
        }
        // 未被拓扑覆盖(环/孤立)的节点按输入顺序续接
        int extra = r;
        for (SwimlaneConfig.NodeConfig n : nodes) {
            if (!rank.containsKey(n.getId())) {
                rank.put(n.getId(), extra++);
            }
        }
        return rank;
    }

    private List<SwimlaneConfig.LaneConfig> validLanes(List<SwimlaneConfig.LaneConfig> lanes) {
        List<SwimlaneConfig.LaneConfig> out = new ArrayList<>();
        if (lanes == null) {
            return out;
        }
        int idx = 0;
        for (SwimlaneConfig.LaneConfig l : lanes) {
            if (l.getName() == null || l.getName().trim().isEmpty()) {
                continue;
            }
            if (l.getId() == null || l.getId().trim().isEmpty()) {
                l.setId("L" + (idx++));
            }
            l.setName(l.getName().trim());
            out.add(l);
        }
        return out;
    }

    private List<SwimlaneConfig.NodeConfig> validNodes(
            List<SwimlaneConfig.NodeConfig> nodes, List<SwimlaneConfig.LaneConfig> lanes) {
        List<SwimlaneConfig.NodeConfig> out = new ArrayList<>();
        if (nodes == null) {
            return out;
        }
        for (SwimlaneConfig.NodeConfig n : nodes) {
            if (n.getName() == null || n.getName().trim().isEmpty()) {
                continue;
            }
            boolean laneExists = false;
            for (SwimlaneConfig.LaneConfig l : lanes) {
                if (l.getId().equals(n.getLaneId())) {
                    laneExists = true;
                    break;
                }
            }
            if (!laneExists) {
                continue;
            }
            n.setName(n.getName().trim());
            out.add(n);
        }
        return out;
    }

    private List<SwimlaneConfig.EdgeConfig> validEdges(
            List<SwimlaneConfig.EdgeConfig> edges, List<SwimlaneConfig.NodeConfig> nodes) {
        List<SwimlaneConfig.EdgeConfig> out = new ArrayList<>();
        if (edges == null) {
            return out;
        }
        for (SwimlaneConfig.EdgeConfig e : edges) {
            if (e.getSource() == null || e.getTarget() == null) {
                continue;
            }
            boolean s = false, t = false;
            for (SwimlaneConfig.NodeConfig n : nodes) {
                if (n.getId().equals(e.getSource())) s = true;
                if (n.getId().equals(e.getTarget())) t = true;
            }
            if (s && t) {
                out.add(e);
            }
        }
        return out;
    }

    private String shapeOf(String type) {
        if (type == null) {
            return "task";
        }
        switch (type.trim().toLowerCase()) {
            case "start": return "start";
            case "end": return "end";
            case "gateway": return "condition";
            default: return "task";
        }
    }

    private DiagramNode node(String id, String label, String shape) {
        DiagramNode n = new DiagramNode();
        n.setId(id);
        n.setLabel(label);
        n.setShape(shape);
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
}
