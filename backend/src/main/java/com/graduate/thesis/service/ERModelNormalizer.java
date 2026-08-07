package com.graduate.thesis.service;

import com.graduate.thesis.dto.ErDTO;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ER 模型规范化: 保留菱形关系, 解析基数为两端标签
 */
public final class ERModelNormalizer {

    private static final Pattern CARD_SPLIT = Pattern.compile("[:：]");

    private ERModelNormalizer() {
    }

    public static ErGraph normalize(ErDTO dto) {
        ErGraph g = new ErGraph();
        Map<String, ErGraph.Entity> byName = new LinkedHashMap<>();

        if (dto.getEntities() != null) {
            for (ErDTO.ErEntity e : dto.getEntities()) {
                if (e.getName() == null || e.getName().trim().isEmpty()) {
                    continue;
                }
                ErGraph.Entity ent = new ErGraph.Entity();
                ent.name = e.getName().trim();
                byName.put(ent.name, ent);
                g.entities.add(ent);
            }
        }

        if (dto.getRelations() != null) {
            for (ErDTO.ErRelation r : dto.getRelations()) {
                if (r.getFrom() == null || r.getTo() == null) {
                    continue;
                }
                ErGraph.Entity from = byName.get(r.getFrom().trim());
                ErGraph.Entity to = byName.get(r.getTo().trim());
                if (from == null || to == null) {
                    continue;
                }
                String card = r.getCardinality() == null ? "" : r.getCardinality().trim();
                String[] parts = CARD_SPLIT.split(card);
                String fromCard = parts.length >= 1 ? parts[0].trim() : "";
                String toCard = parts.length >= 2 ? parts[1].trim() : "";

                ErGraph.Relation rel = new ErGraph.Relation();
                rel.name = (r.getLabel() == null || r.getLabel().trim().isEmpty())
                        ? "关联" : r.getLabel().trim();
                rel.from = from;
                rel.to = to;
                rel.fromCard = fromCard;
                rel.toCard = toCard;
                if (r.getAttrs() != null) {
                    for (ErDTO.ErAttr a : r.getAttrs()) {
                        ErGraph.Attribute attr = new ErGraph.Attribute();
                        attr.name = a.getName() == null ? "" : a.getName().trim();
                        attr.key = Boolean.TRUE.equals(a.getKey());
                        attr.parent = rel;
                        rel.attrs.add(attr);
                        g.attributes.add(attr);
                    }
                }
                g.relations.add(rel);
            }
        }

        // 实体属性
        for (ErGraph.Entity ent : g.entities) {
            ErDTO.ErEntity src = null;
            if (dto.getEntities() != null) {
                for (ErDTO.ErEntity e : dto.getEntities()) {
                    if (e.getName() != null && e.getName().trim().equals(ent.name)) {
                        src = e;
                        break;
                    }
                }
            }
            if (src != null && src.getAttrs() != null) {
                for (ErDTO.ErAttr a : src.getAttrs()) {
                    ErGraph.Attribute attr = new ErGraph.Attribute();
                    attr.name = a.getName() == null ? "" : a.getName().trim();
                    attr.key = Boolean.TRUE.equals(a.getKey());
                    attr.parent = ent;
                    ent.attrs.add(attr);
                    g.attributes.add(attr);
                }
            }
        }
        return g;
    }
}
