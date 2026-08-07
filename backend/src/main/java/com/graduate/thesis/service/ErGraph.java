package com.graduate.thesis.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Chen 记法 ER 图模型:
 *   - Entity: 矩形, 居中显示实体名
 *   - Relation: 菱形, 居中显示关系名, 挂在两个实体之间
 *   - Attribute: 椭圆, 挂在实体或关系旁边, 主键文字加下划线
 */
public class ErGraph {

    public List<Entity> entities = new ArrayList<>();
    public List<Relation> relations = new ArrayList<>();
    public List<Attribute> attributes = new ArrayList<>();

    public int width;
    public int height;

    public static class Entity {
        public String name;
        public double cx, cy;
        public double w, h;
        public List<Attribute> attrs = new ArrayList<>();
    }

    public static class Relation {
        public String name;
        public double cx, cy;
        public double hw, hh;  // 菱形半宽/半高
        public Entity from;
        public Entity to;
        public String fromCard;  // 靠近 from 端的基数
        public String toCard;    // 靠近 to 端的基数
        public List<Attribute> attrs = new ArrayList<>();
    }

    public static class Attribute {
        public String name;
        public boolean key;
        public double cx, cy;
        public double rx, ry;  // 椭圆半径
        public Object parent;  // Entity 或 Relation
    }
}
