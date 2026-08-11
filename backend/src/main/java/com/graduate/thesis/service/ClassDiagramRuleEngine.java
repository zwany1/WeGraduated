package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.ClassConfig;
import com.graduate.thesis.dto.DiagramEdge;
import com.graduate.thesis.dto.DiagramNode;
import com.graduate.thesis.dto.DiagramVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 类图规则引擎(UML Class Diagram): 类 + 属性 + 方法 + 关系
 *
 * 布局规则:
 *   - 类节点: 类名区(40) + 属性区 + 方法区(三区域, 高度按内容行数)
 *   - 坐标由前端 Dagre 自动布局(后端不返回具体位置)
 *   - 关系: 关联/继承/组合, 带基数标签(1 / n)
 */
@Service
public class ClassDiagramRuleEngine {

    private int seq = 0;
    private static final int NODE_W = 230;
    private static final int ROW_H = 24;
    private static final int HEADER_H = 40;
    private static final int PAD = 20;

    public DiagramVO build(ClassConfig config) {
        if (config == null) {
            throw new BusinessException(400, "请配置类图");
        }
        List<ClassConfig.ClassItem> classes = validClasses(config.getClasses());
        List<ClassConfig.ClassRelation> relations = validRelations(config.getRelations(), classes);
        if (classes.isEmpty()) {
            throw new BusinessException(400, "请至少配置一个类");
        }
        seq = 0;

        DiagramVO vo = new DiagramVO();
        vo.setType("CLASS");
        vo.setName(config.getTitle() == null || config.getTitle().trim().isEmpty()
                ? "类图" : config.getTitle().trim());

        // 1. 类节点(三区域高度), 坐标由前端 Dagre 布局
        for (ClassConfig.ClassItem c : classes) {
            int attrs = c.getAttributes() == null ? 0 : c.getAttributes().size();
            int methods = c.getMethods() == null ? 0 : c.getMethods().size();
            int h = HEADER_H + attrs * ROW_H + methods * ROW_H + PAD;
            DiagramNode node = node(c.getId(), c.getName(), "classNode");
            node.setX(0);
            node.setY(0);
            node.setWidth(NODE_W);
            node.setHeight(h);
            node.setAttrsText(attrText(c.getAttributes()));
            node.setMethodsText(methodText(c.getMethods()));
            vo.getNodes().add(node);
        }

        // 2. 关系连线
        for (ClassConfig.ClassRelation r : relations) {
            DiagramEdge edge = new DiagramEdge();
            edge.setId("e" + (seq++));
            edge.setSource(r.getSource());
            edge.setTarget(r.getTarget());
            edge.setStyle(edgeStyle(r.getType()));
            StringBuilder labels = new StringBuilder();
            if (r.getLeft() != null && !r.getLeft().isEmpty()) labels.append(r.getLeft());
            if (r.getRight() != null && !r.getRight().isEmpty()) labels.append(" ").append(r.getRight());
            edge.setLabel(labels.toString().trim());
            vo.getEdges().add(edge);
        }

        // 画布尺寸由前端布局后自适应, 给个宽松默认
        vo.setWidth(1200);
        vo.setHeight(900);
        return vo;
    }

    private String attrText(List<ClassConfig.Attribute> attrs) {
        if (attrs == null || attrs.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (ClassConfig.Attribute a : attrs) {
            String vis = a.getVisibility() == null || a.getVisibility().isEmpty() ? "+" : a.getVisibility().trim();
            String type = a.getType() == null || a.getType().isEmpty() ? "" : ": " + a.getType().trim();
            String name = a.getName() == null ? "" : a.getName().trim();
            if (sb.length() > 0) sb.append("\n");
            sb.append(vis).append(" ").append(name).append(type);
        }
        return sb.toString();
    }

    private String methodText(List<ClassConfig.Method> methods) {
        if (methods == null || methods.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (ClassConfig.Method m : methods) {
            String vis = m.getVisibility() == null || m.getVisibility().isEmpty() ? "+" : m.getVisibility().trim();
            String ret = m.getReturnType() == null || m.getReturnType().isEmpty() ? "void" : m.getReturnType().trim();
            String name = m.getName() == null ? "" : m.getName().trim();
            if (sb.length() > 0) sb.append("\n");
            sb.append(vis).append(" ").append(name).append("():").append(ret);
        }
        return sb.toString();
    }

    private String edgeStyle(String type) {
        if (type == null) return "association";
        switch (type.trim().toLowerCase()) {
            case "inheritance": return "inheritance";
            case "composition": return "composition";
            case "aggregation": return "aggregation";
            default: return "association";
        }
    }

    private List<ClassConfig.ClassItem> validClasses(List<ClassConfig.ClassItem> classes) {
        List<ClassConfig.ClassItem> out = new ArrayList<>();
        if (classes == null) return out;
        int i = 0;
        for (ClassConfig.ClassItem c : classes) {
            if (c.getName() == null || c.getName().trim().isEmpty()) continue;
            if (c.getId() == null || c.getId().trim().isEmpty()) c.setId("C" + (i++));
            c.setName(c.getName().trim());
            out.add(c);
        }
        return out;
    }

    private List<ClassConfig.ClassRelation> validRelations(
            List<ClassConfig.ClassRelation> relations, List<ClassConfig.ClassItem> classes) {
        List<ClassConfig.ClassRelation> out = new ArrayList<>();
        if (relations == null) return out;
        Map<String, Boolean> ids = new HashMap<>();
        for (ClassConfig.ClassItem c : classes) ids.put(c.getId(), true);
        for (ClassConfig.ClassRelation r : relations) {
            if (r.getSource() != null && r.getTarget() != null
                    && ids.containsKey(r.getSource()) && ids.containsKey(r.getTarget())) {
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
