package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.ActivityConfig;
import com.graduate.thesis.dto.DiagramEdge;
import com.graduate.thesis.dto.DiagramLane;
import com.graduate.thesis.dto.DiagramNode;
import com.graduate.thesis.dto.DiagramVO;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 泳道活动图规则引擎(UML Activity + Swimlane): 泳道(列) + 节点(类型) + 连线(条件)
 *
 * 布局规则:
 *   - 泳道横向排列为列: x = index * laneWidth
 *   - 节点按拓扑排序分配 rank(流程层级), y = marginTop + rank * rowHeight
 *   - 判断(DECISION)分支的节点位于同一 rank(并行)
 *   - 节点类型: START(圆) / ACTION(圆角矩形) / DECISION(菱形) / END(圆)
 */
@Service
public class ActivityDiagramRuleEngine {

    private int seq = 0;

    public DiagramVO build(ActivityConfig config) {
        if (config == null) {
            throw new BusinessException(400, "请配置活动图");
        }
        List<ActivityConfig.LaneConfig> lanes = validLanes(config.getLanes());
        List<ActivityConfig.NodeConfig> nodes = validNodes(config.getNodes(), lanes);
        List<ActivityConfig.EdgeConfig> edges = validEdges(config.getEdges(), nodes);
        if (lanes.isEmpty()) {
            throw new BusinessException(400, "请至少配置一个泳道");
        }
        if (nodes.isEmpty()) {
            throw new BusinessException(400, "请至少配置一个节点");
        }
        seq = 0;

        DiagramVO vo = new DiagramVO();
        vo.setType("ACTIVITY");
        vo.setName(config.getTitle() == null || config.getTitle().trim().isEmpty()
                ? "活动图" : config.getTitle().trim());

        int nodeH = 52;
        int gapY = 70;
        int marginTop = 90;
        int marginBottom = 60;
        int rowHeight = nodeH + gapY;

        // 1. 拓扑排序分配 rank
        Map<String, Integer> rank = topoRank(nodes, edges);
        int maxRank = rank.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int totalHeight = marginTop + (maxRank + 1) * rowHeight + marginBottom;

        // 2. 泳道 -> 列, 宽度自适应(节点内容宽度 + 泳道名宽度)
        Map<String, List<ActivityConfig.NodeConfig>> byLane = new LinkedHashMap<>();
        for (ActivityConfig.LaneConfig lane : lanes) {
            byLane.put(lane.getId(), new ArrayList<>());
        }
        for (ActivityConfig.NodeConfig n : nodes) {
            byLane.get(n.getLaneId()).add(n);
        }

        // 计算每个泳道宽度
        int laneGap = 30;
        Map<String, Integer> laneWMap = new LinkedHashMap<>();
        int totalWidth = 0;
        for (ActivityConfig.LaneConfig lane : lanes) {
            // 泳道名宽度(更宽)
            int nameW = Math.max(90, lane.getName().length() * 16 + 30);
            // 泳道内节点最大内容宽度(判断菱形加宽)
            int maxNodeW = 0;
            for (ActivityConfig.NodeConfig n : byLane.get(lane.getId())) {
                int textW = Math.max(140, n.getText().length() * 15 + 40);
                if ("DECISION".equalsIgnoreCase(n.getType())) {
                    textW = Math.max(textW, 170);
                }
                maxNodeW = Math.max(maxNodeW, textW);
            }
            // 泳道宽 = 名称区 + 节点区 + 两侧内边距, 最小宽度兜底
            int w = Math.max(200, nameW + maxNodeW + 50);
            laneWMap.put(lane.getId(), w);
            totalWidth += w + laneGap;
        }
        totalWidth -= laneGap; // 最后一条后面不加 gap

        // 判断节点分支水平偏移: 同一判断的多个下游节点左右错开, 避免连线重合
        Map<String, Integer> branchOffset = new LinkedHashMap<>();
        for (ActivityConfig.NodeConfig src : nodes) {
            if (!"DECISION".equalsIgnoreCase(src.getType())) continue;
            List<ActivityConfig.EdgeConfig> outs = new ArrayList<>();
            for (ActivityConfig.EdgeConfig e : edges) {
                if (src.getId().equals(e.getSource())) outs.add(e);
            }
            int n = outs.size();
            if (n < 2) continue;
            for (int i = 0; i < n; i++) {
                ActivityConfig.EdgeConfig e = outs.get(i);
                // 平均分布: 居中为0, 两侧对称
                int center = (n - 1) / 2.0 == 0 ? 0 : (int) ((i - (n - 1) / 2.0) * 120);
                branchOffset.merge(e.getTarget(), center, Integer::sum);
            }
        }

        int laneIndex = 0;
        for (ActivityConfig.LaneConfig lane : lanes) {
            int laneWidth = laneWMap.get(lane.getId());
            double x = 0;
            // 累加前面泳道宽度 + gap
            for (ActivityConfig.LaneConfig prev : lanes) {
                if (prev.getId().equals(lane.getId())) break;
                x += laneWMap.get(prev.getId()) + laneGap;
            }
            DiagramLane dl = new DiagramLane();
            dl.setId("lane" + (seq++));
            dl.setName(lane.getName());
            dl.setX(x);
            dl.setY(0);
            dl.setWidth(laneWidth);
            dl.setHeight(totalHeight);
            vo.getLanes().add(dl);

            int nodeW = laneWidth - 50 - nameWidth(lane.getName());
            for (ActivityConfig.NodeConfig n : byLane.get(lane.getId())) {
                int r = rank.get(n.getId());
                DiagramNode dn = node(n.getId(), n.getText(), shapeOf(n.getType()));
                dn.setLane(dl.getId());
                dn.setX(x + (laneWidth - nodeW) / 2.0 + branchOffset.getOrDefault(n.getId(), 0));
                dn.setY(marginTop + r * rowHeight);
                dn.setWidth(nodeW);
                dn.setHeight(nodeH);
                vo.getNodes().add(dn);
            }
            laneIndex++;
        }

        // 3. 连线(带条件标签)
        for (ActivityConfig.EdgeConfig e : edges) {
            vo.getEdges().add(edge(e.getSource(), e.getTarget(), e.getLabel()));
        }

        vo.setWidth(totalWidth);
        vo.setHeight(totalHeight);
        return vo;
    }

    private int nameWidth(String name) {
        return Math.max(90, name.length() * 16 + 30);
    }

    /**
     * 拓扑排序: 按 edges 分配 rank; 孤立/成环节点按输入顺序续接
     */
    private Map<String, Integer> topoRank(List<ActivityConfig.NodeConfig> nodes,
                                          List<ActivityConfig.EdgeConfig> edges) {
        Map<String, Integer> rank = new HashMap<>();
        Map<String, Integer> indeg = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (ActivityConfig.NodeConfig n : nodes) {
            indeg.put(n.getId(), 0);
            adj.put(n.getId(), new ArrayList<>());
        }
        for (ActivityConfig.EdgeConfig e : edges) {
            if (!adj.containsKey(e.getSource()) || !adj.containsKey(e.getTarget())) {
                continue;
            }
            adj.get(e.getSource()).add(e.getTarget());
            indeg.put(e.getTarget(), indeg.get(e.getTarget()) + 1);
        }
        Deque<String> q = new ArrayDeque<>();
        for (ActivityConfig.NodeConfig n : nodes) {
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
        int extra = r;
        for (ActivityConfig.NodeConfig n : nodes) {
            if (!rank.containsKey(n.getId())) {
                rank.put(n.getId(), extra++);
            }
        }
        return rank;
    }

    private List<ActivityConfig.LaneConfig> validLanes(List<ActivityConfig.LaneConfig> lanes) {
        List<ActivityConfig.LaneConfig> out = new ArrayList<>();
        if (lanes == null) return out;
        int i = 0;
        for (ActivityConfig.LaneConfig l : lanes) {
            if (l.getName() == null || l.getName().trim().isEmpty()) continue;
            if (l.getId() == null || l.getId().trim().isEmpty()) l.setId("L" + (i++));
            l.setName(l.getName().trim());
            out.add(l);
        }
        return out;
    }

    private List<ActivityConfig.NodeConfig> validNodes(
            List<ActivityConfig.NodeConfig> nodes, List<ActivityConfig.LaneConfig> lanes) {
        List<ActivityConfig.NodeConfig> out = new ArrayList<>();
        if (nodes == null) return out;
        Map<String, Boolean> laneIds = new HashMap<>();
        for (ActivityConfig.LaneConfig l : lanes) laneIds.put(l.getId(), true);
        int i = 0;
        for (ActivityConfig.NodeConfig n : nodes) {
            if (n.getText() == null || n.getText().trim().isEmpty()) continue;
            if (n.getLaneId() == null || !laneIds.containsKey(n.getLaneId())) continue;
            if (n.getId() == null || n.getId().trim().isEmpty()) n.setId("N" + (i++));
            n.setText(n.getText().trim());
            if (n.getType() == null || n.getType().trim().isEmpty()) n.setType("ACTION");
            out.add(n);
        }
        return out;
    }

    private List<ActivityConfig.EdgeConfig> validEdges(
            List<ActivityConfig.EdgeConfig> edges, List<ActivityConfig.NodeConfig> nodes) {
        List<ActivityConfig.EdgeConfig> out = new ArrayList<>();
        if (edges == null) return out;
        Map<String, Boolean> ids = new HashMap<>();
        for (ActivityConfig.NodeConfig n : nodes) ids.put(n.getId(), true);
        for (ActivityConfig.EdgeConfig e : edges) {
            if (e.getSource() != null && e.getTarget() != null
                    && ids.containsKey(e.getSource()) && ids.containsKey(e.getTarget())) {
                out.add(e);
            }
        }
        return out;
    }

    private String shapeOf(String type) {
        if (type == null) return "action";
        switch (type.trim().toUpperCase()) {
            case "START": return "start";
            case "END": return "end";
            case "DECISION": return "condition";
            default: return "action";
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
