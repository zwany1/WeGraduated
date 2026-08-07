package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.EREdge;
import com.graduate.thesis.dto.ERGraphVO;
import com.graduate.thesis.dto.ERNode;
import com.graduate.thesis.dto.ErDTO;
import org.springframework.stereotype.Service;

import java.awt.Font;

/**
 * ER 图数据结构生成: 布局坐标 + 节点/边, 供前端渲染
 */
@Service
public class ERGraphService {

    public ERGraphVO build(ErDTO dto) {
        if (dto.getEntities() == null || dto.getEntities().isEmpty()) {
            throw new BusinessException(400, "请至少添加一个实体");
        }
        // 1. 规范化 + 布局(复用现有引擎, 计算实体/菱形/属性坐标)
        ErGraph g = ERModelNormalizer.normalize(dto);
        if (g.entities.isEmpty()) {
            throw new BusinessException(400, "请至少填写一个实体名称");
        }
        Font attrFont = new Font("Microsoft YaHei", Font.PLAIN, 12);
        ERLayoutEngine.layout(g, attrFont);

        ERGraphVO vo = new ERGraphVO();
        vo.setWidth(g.width);
        vo.setHeight(g.height);

        // 2. 实体 → rect 节点
        for (ErGraph.Entity e : g.entities) {
            ERNode n = new ERNode();
            n.setId("e_" + e.name);
            n.setLabel(e.name);
            n.setShape("rect");
            n.setX(e.cx - e.w / 2);
            n.setY(e.cy - e.h / 2);
            n.setWidth(e.w);
            n.setHeight(e.h);
            vo.getNodes().add(n);
        }

        // 3. 关系 → rhombus 节点
        for (ErGraph.Relation r : g.relations) {
            ERNode n = new ERNode();
            n.setId("r_" + r.name);
            n.setLabel(r.name);
            n.setShape("rhombus");
            n.setX(r.cx - r.hw);
            n.setY(r.cy - r.hh);
            n.setWidth(r.hw * 2);
            n.setHeight(r.hh * 2);
            vo.getNodes().add(n);
        }

        // 4. 属性 → ellipse 节点
        for (ErGraph.Attribute a : g.attributes) {
            ERNode n = new ERNode();
            String owner = ownerId(a.parent);
            n.setId("a_" + owner + "_" + a.name);
            n.setLabel(a.name);
            n.setShape("ellipse");
            n.setKey(a.key);
            n.setX(a.cx - a.rx);
            n.setY(a.cy - a.ry);
            n.setWidth(a.rx * 2);
            n.setHeight(a.ry * 2);
            vo.getNodes().add(n);
        }

        // 5. 边
        int seq = 0;
        // 实体-关系边(带基数嵌入文字): from 实体 --(fromCard)-- 菱形 --(toCard)-- to 实体
        for (ErGraph.Relation r : g.relations) {
            String rid = "r_" + r.name;
            String fromId = "e_" + r.from.name;
            String toId = "e_" + r.to.name;
            if (r.fromCard != null && !r.fromCard.isEmpty()) {
                vo.getEdges().add(edge(seq++, fromId, rid, r.fromCard, 0.18f));
            } else {
                vo.getEdges().add(edge(seq++, fromId, rid, null, 0.5f));
            }
            if (r.toCard != null && !r.toCard.isEmpty()) {
                vo.getEdges().add(edge(seq++, rid, toId, r.toCard, 0.82f));
            } else {
                vo.getEdges().add(edge(seq++, rid, toId, null, 0.5f));
            }
        }
        // 实体-属性边
        for (ErGraph.Entity e : g.entities) {
            String eid = "e_" + e.name;
            for (ErGraph.Attribute a : e.attrs) {
                vo.getEdges().add(edge(seq++, eid, "a_" + eid + "_" + a.name, null, 0.5f));
            }
        }
        // 关系-属性边
        for (ErGraph.Relation r : g.relations) {
            String rid = "r_" + r.name;
            for (ErGraph.Attribute a : r.attrs) {
                vo.getEdges().add(edge(seq++, rid, "a_" + rid + "_" + a.name, null, 0.5f));
            }
        }
        return vo;
    }

    private EREdge edge(int seq, String src, String tgt, String text, float pos) {
        EREdge e = new EREdge();
        e.setId("edge_" + seq);
        e.setSource(src);
        e.setTarget(tgt);
        e.setRelationText(text);
        e.setTextPosition(pos);
        return e;
    }

    private String ownerId(Object parent) {
        if (parent instanceof ErGraph.Entity) {
            return "e_" + ((ErGraph.Entity) parent).name;
        }
        return "r_" + ((ErGraph.Relation) parent).name;
    }
}
