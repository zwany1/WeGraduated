package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.DiagramEdge;
import com.graduate.thesis.dto.DiagramLane;
import com.graduate.thesis.dto.DiagramNode;
import com.graduate.thesis.dto.DiagramVO;
import com.graduate.thesis.dto.UseCaseConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用例图规则引擎(UML Use Case Diagram): Actor + UseCase + Relation
 *
 * 布局规则:
 *   - 系统边界框: 围住所有用例(中间区域)
 *   - 参与者: 左侧(关联的) / 右侧(纯动作), y 均匀分布
 *   - 用例: 中间区域, 按模块分组纵向排列
 *   - 关系: association 实线 / include,extend 虚线箭头(带 <<>> 标签)
 */
@Service
public class UseCaseRuleEngine {

    private int seq = 0;

    public DiagramVO build(UseCaseConfig config) {
        if (config == null) {
            throw new BusinessException(400, "请配置用例图");
        }
        List<UseCaseConfig.ActorConfig> actors = validActors(config.getActors());
        List<UseCaseConfig.UseCaseConfigItem> usecases = validUseCases(config.getUsecases());
        List<UseCaseConfig.RelationConfig> relations = validRelations(config.getRelations(), actors, usecases);
        if (usecases.isEmpty()) {
            throw new BusinessException(400, "请至少配置一个用例");
        }
        seq = 0;

        DiagramVO vo = new DiagramVO();
        vo.setType("USECASE");
        vo.setName(config.getSystem() == null || config.getSystem().trim().isEmpty()
                ? "系统用例图" : config.getSystem().trim());

        // ---------- 布局参数 ----------
        int actorW = 120, actorH = 120;
        int ucW = 170, ucH = 56;
        int ucGapX = 40, ucGapY = 30;
        int moduleGap = 30;
        int actorColX = 90;     // 左侧 Actor 列
        int actorRightX = 950;  // 右侧 Actor 列
        int ucX0 = 380;         // 用例区起始 x
        int marginTop = 100;

        // ---------- 1. 用例按模块分组 ----------
        Map<String, List<UseCaseConfig.UseCaseConfigItem>> modules = new LinkedHashMap<>();
        for (UseCaseConfig.UseCaseConfigItem u : usecases) {
            String mod = (u.getModule() == null || u.getModule().trim().isEmpty()) ? "" : u.getModule().trim();
            modules.computeIfAbsent(mod, k -> new ArrayList<>()).add(u);
        }

        // 计算用例区高度
        int ucAreaH = 0;
        for (List<UseCaseConfig.UseCaseConfigItem> list : modules.values()) {
            ucAreaH += list.size() * (ucH + ucGapY);
        }
        ucAreaH += Math.max(0, modules.size() - 1) * moduleGap;

        // 系统边界框尺寸
        int sysW = ucW + ucGapX * 2 + 60;
        int sysH = Math.max(ucAreaH + 90, marginTop + 60);
        int sysX = ucX0 - 30;
        int sysY = 40;

        // ---------- 2. 参与者分类: 左侧(被用例关联) / 右侧 ----------
        Set<String> linked = new LinkedHashSet<>();
        for (UseCaseConfig.RelationConfig r : relations) {
            if ("association".equals(r.getType())) {
                linked.add(r.getSource());
                linked.add(r.getTarget());
            }
        }
        List<UseCaseConfig.ActorConfig> leftActors = new ArrayList<>();
        List<UseCaseConfig.ActorConfig> rightActors = new ArrayList<>();
        for (UseCaseConfig.ActorConfig a : actors) {
            if (linked.contains(a.getId())) {
                leftActors.add(a);
            } else {
                rightActors.add(a);
            }
        }

        // Actor 布局
        int n = Math.max(leftActors.size(), 1);
        for (int i = 0; i < leftActors.size(); i++) {
            UseCaseConfig.ActorConfig a = leftActors.get(i);
            DiagramNode node = node(a.getId(), a.getName(), "actor");
            node.setX(actorColX);
            node.setY(marginTop + i * (sysH - 2 * marginTop) / n);
            vo.getNodes().add(node);
        }
        int m = Math.max(rightActors.size(), 1);
        for (int i = 0; i < rightActors.size(); i++) {
            UseCaseConfig.ActorConfig a = rightActors.get(i);
            DiagramNode node = node(a.getId(), a.getName(), "actor");
            node.setX(actorRightX);
            node.setY(marginTop + i * (sysH - 2 * marginTop) / m);
            vo.getNodes().add(node);
        }

        // ---------- 3. 用例布局(按模块) ----------
        int y = marginTop + 20;
        for (Map.Entry<String, List<UseCaseConfig.UseCaseConfigItem>> e : modules.entrySet()) {
            String mod = e.getKey();
            if (!mod.isEmpty()) {
                // 模块标签(作为 lane 节点, 前端渲染为用例区分组标题)
                DiagramLane lane = new DiagramLane();
                lane.setId("mod" + (seq++));
                lane.setName(mod);
                lane.setX(ucX0);
                lane.setY(y - ucGapY + 6);
                lane.setWidth(sysW - 40);
                lane.setHeight(10);
                vo.getLanes().add(lane);
            }
            int x = ucX0;
            for (UseCaseConfig.UseCaseConfigItem u : e.getValue()) {
                DiagramNode node = node(u.getId(), u.getName(), "usecase");
                node.setX(x);
                node.setY(y);
                vo.getNodes().add(node);
                y += ucH + ucGapY;
            }
        }

        // ---------- 4. 系统边界框(放最后, zIndex 最低) ----------
        DiagramNode sys = node("system", vo.getName(), "system");
        sys.setX(sysX);
        sys.setY(sysY);
        sys.setWidth(sysW);
        sys.setHeight(sysH);
        vo.getNodes().add(0, sys);

        // ---------- 5. 关系连线 ----------
        for (UseCaseConfig.RelationConfig r : relations) {
            DiagramEdge edge = new DiagramEdge();
            edge.setId("e" + (seq++));
            edge.setSource(r.getSource());
            edge.setTarget(r.getTarget());
            edge.setLabel(edgeLabel(r.getType()));
            edge.setStyle(edgeStyle(r.getType()));
            vo.getEdges().add(edge);
        }

        vo.setWidth(actorRightX + actorW + 60);
        vo.setHeight(sysH + 80);
        return vo;
    }

    private String edgeLabel(String type) {
        if (type == null) return "";
        switch (type.trim().toLowerCase()) {
            case "include": return "«include»";
            case "extend": return "«extend»";
            default: return "";
        }
    }

    private String edgeStyle(String type) {
        if (type == null) return "association";
        switch (type.trim().toLowerCase()) {
            case "include": return "include";
            case "extend": return "include";
            default: return "association";
        }
    }

    private List<UseCaseConfig.ActorConfig> validActors(List<UseCaseConfig.ActorConfig> actors) {
        List<UseCaseConfig.ActorConfig> out = new ArrayList<>();
        if (actors == null) return out;
        int idx = 0;
        for (UseCaseConfig.ActorConfig a : actors) {
            if (a.getName() == null || a.getName().trim().isEmpty()) continue;
            if (a.getId() == null || a.getId().trim().isEmpty()) a.setId("A" + (idx++));
            a.setName(a.getName().trim());
            out.add(a);
        }
        return out;
    }

    private List<UseCaseConfig.UseCaseConfigItem> validUseCases(List<UseCaseConfig.UseCaseConfigItem> usecases) {
        List<UseCaseConfig.UseCaseConfigItem> out = new ArrayList<>();
        if (usecases == null) return out;
        int idx = 0;
        for (UseCaseConfig.UseCaseConfigItem u : usecases) {
            if (u.getName() == null || u.getName().trim().isEmpty()) continue;
            if (u.getId() == null || u.getId().trim().isEmpty()) u.setId("U" + (idx++));
            u.setName(u.getName().trim());
            out.add(u);
        }
        return out;
    }

    private List<UseCaseConfig.RelationConfig> validRelations(
            List<UseCaseConfig.RelationConfig> relations,
            List<UseCaseConfig.ActorConfig> actors,
            List<UseCaseConfig.UseCaseConfigItem> usecases) {
        List<UseCaseConfig.RelationConfig> out = new ArrayList<>();
        if (relations == null) return out;
        Set<String> ids = new LinkedHashSet<>();
        for (UseCaseConfig.ActorConfig a : actors) ids.add(a.getId());
        for (UseCaseConfig.UseCaseConfigItem u : usecases) ids.add(u.getId());
        for (UseCaseConfig.RelationConfig r : relations) {
            if (r.getSource() == null || r.getTarget() == null) continue;
            if (ids.contains(r.getSource()) && ids.contains(r.getTarget())) {
                if (r.getType() == null || r.getType().trim().isEmpty()) r.setType("association");
                out.add(r);
            }
        }
        return out;
    }

    private DiagramNode node(String id, String label, String shape) {
        DiagramNode n = new DiagramNode();
        n.setId(id);
        n.setLabel(label);
        n.setShape(shape);
        return n;
    }
}
