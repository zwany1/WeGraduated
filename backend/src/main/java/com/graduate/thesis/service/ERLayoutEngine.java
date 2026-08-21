package com.graduate.thesis.service;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

/**
 * Chen 记法 ER 图布局:
 *   - 实体: 网格排列
 *   - 关系: 两实体中点, 碰撞检测后沿垂直方向偏移避免重叠
 *   - 属性: 父节点周围 360° 均匀环绕
 */
public final class ERLayoutEngine {

    public static final int ENTITY_W = 130;
    public static final int ENTITY_H = 46;
    public static final int ENTITY_GAP = 280;
    public static final int ATTR_DIST = 150;
    public static final int REL_HALF_W = 55;
    public static final int REL_HALF_H = 34;
    public static final int MARGIN = 120;

    private ERLayoutEngine() {
    }

    public static void layout(ErGraph g, Font attrFont) {
        if (g.entities.isEmpty()) {
            return;
        }
        measureAttributes(g, attrFont);
        initEntitySize(g);
        hierarchyLayoutEntities(g);
        placeRelations(g);
        placeAttributes(g);
        resolveAttributeOverlaps(g);
        avoidCardinalityLabels(g);
        sortAttrsByAngle(g);
        resolveLineCrossings(g);
        computeCanvas(g);
    }


    /**
     * 属性避开基数标注: 每个关系在 from/to 连线上各有一个基数(位于连线中点附近),
     * 把连线上基数可能出现的位置作为冲突区, 将属性椭圆推开
     */
    private static void avoidCardinalityLabels(ErGraph g) {
        // 收集所有基数位置(关系两端连线的 40%~60% 区间中点)
        List<double[]> cardPositions = new ArrayList<>();
        for (ErGraph.Relation r : g.relations) {
            cardPositions.add(cardMid(r.from, r));
            cardPositions.add(cardMid(r.to, r));
        }
        if (cardPositions.isEmpty()) {
            return;
        }
        for (int iter = 0; iter < 8; iter++) {
            boolean moved = false;
            for (ErGraph.Attribute a : g.attributes) {
                for (double[] cp : cardPositions) {
                    double dx = a.cx - cp[0];
                    double dy = a.cy - cp[1];
                    double d = Math.sqrt(dx * dx + dy * dy);
                    double minD = Math.max(a.rx, a.ry) + 14;
                    if (d < minD) {
                        if (d < 1) {
                            a.cx = cp[0] + minD;
                        } else {
                            a.cx = cp[0] + dx / d * minD;
                            a.cy = cp[1] + dy / d * minD;
                        }
                        moved = true;
                    }
                }
            }
            if (!moved) {
                break;
            }
        }
    }

    /**
     * 关系 from/to 端连线上基数的位置(实体边缘->菱形边交点线段的中点)
     */
    private static double[] cardMid(ErGraph.Entity e, ErGraph.Relation rel) {
        double dx = rel.cx - e.cx;
        double dy = rel.cy - e.cy;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) {
            return new double[]{e.cx, e.cy};
        }
        double ux = dx / len, uy = dy / len;
        // 实体边缘
        double tH = Math.abs(ux) < 1e-9 ? Double.MAX_VALUE : (e.w / 2) / Math.abs(ux);
        double tV = Math.abs(uy) < 1e-9 ? Double.MAX_VALUE : (e.h / 2) / Math.abs(uy);
        double tRect = Math.min(tH, tV);
        double sx = e.cx + ux * tRect;
        double sy = e.cy + uy * tRect;
        // 菱形边交点
        double tDia = 1.0 / (Math.abs(ux) / rel.hw + Math.abs(uy) / rel.hh);
        double ex = rel.cx - ux * tDia;
        double ey = rel.cy - uy * tDia;
        return new double[]{sx + (ex - sx) * 0.5, sy + (ey - sy) * 0.5};
    }

    /**
     * 基于关系连接的实体聚类布局: 用 BFS 从连接度最高的实体开始遍历,
     * 得到"有关系的实体尽量相邻"的顺序, 再蛇形填入网格, 使有关系实体在网格中相邻
     */
    /**
     * 初始化实体尺寸(位置由力导向布局决定)
     */
    private static void initEntitySize(ErGraph g) {
        for (ErGraph.Entity e : g.entities) {
            e.cx = 0;
            e.cy = 0;
            e.w = ENTITY_W;
            e.h = ENTITY_H;
        }
    }

    /**
     * 实体层级布局(BFS): 从连接最多的中心实体开始 BFS 分层,
     * 层沿 X 方向排列(layerGap), 层内节点沿 Y 方向排列(nodeGap)。
     * 有关系的实体自然相邻, 无需力导向。
     */
    private static void hierarchyLayoutEntities(ErGraph g) {
        if (g.entities.isEmpty()) {
            return;
        }
        // 1. 建图: 实体邻接(通过 Relation 连接)
        Map<ErGraph.Entity, List<ErGraph.Entity>> graph = new HashMap<ErGraph.Entity, List<ErGraph.Entity>>();
        for (ErGraph.Entity e : g.entities) {
            graph.put(e, new ArrayList<ErGraph.Entity>());
        }
        for (ErGraph.Relation r : g.relations) {
            graph.get(r.from).add(r.to);
            graph.get(r.to).add(r.from);
        }
        // 2. 找中心实体(连接最多的)
        ErGraph.Entity root = findCenterEntity(g);
        // 3. BFS 计算层级
        Map<ErGraph.Entity, Integer> level = new HashMap<ErGraph.Entity, Integer>();
        Queue<ErGraph.Entity> queue = new LinkedList<ErGraph.Entity>();
        level.put(root, 0);
        queue.offer(root);
        while (!queue.isEmpty()) {
            ErGraph.Entity current = queue.poll();
            for (ErGraph.Entity next : graph.get(current)) {
                if (!level.containsKey(next)) {
                    level.put(next, level.get(current) + 1);
                    queue.offer(next);
                }
            }
        }
        // 孤立实体(无关系)补到最后一层
        int maxLevel = 0;
        for (int lv : level.values()) {
            maxLevel = Math.max(maxLevel, lv);
        }
        for (ErGraph.Entity e : g.entities) {
            if (!level.containsKey(e)) {
                level.put(e, maxLevel + 1);
            }
        }
        // 4. 同层排列
        Map<Integer, List<ErGraph.Entity>> layers = new TreeMap<Integer, List<ErGraph.Entity>>();
        for (Map.Entry<ErGraph.Entity, Integer> e : level.entrySet()) {
            List<ErGraph.Entity> list = layers.get(e.getValue());
            if (list == null) {
                list = new ArrayList<ErGraph.Entity>();
                layers.put(e.getValue(), list);
            }
            list.add(e.getKey());
        }
        int layerGap = 350;
        int nodeGap = 250;
        for (Map.Entry<Integer, List<ErGraph.Entity>> entry : layers.entrySet()) {
            int l = entry.getKey();
            List<ErGraph.Entity> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                ErGraph.Entity e = list.get(i);
                e.cx = l * layerGap;
                e.cy = (i - (list.size() - 1) / 2.0) * nodeGap;
            }
        }
    }

    /**
     * 找中心实体: 连接关系最多的实体
     */
    private static ErGraph.Entity findCenterEntity(ErGraph g) {
        ErGraph.Entity result = null;
        int max = -1;
        for (ErGraph.Entity e : g.entities) {
            int count = 0;
            for (ErGraph.Relation r : g.relations) {
                if (r.from == e || r.to == e) {
                    count++;
                }
            }
            if (count > max) {
                max = count;
                result = e;
            }
        }
        if (result == null && !g.entities.isEmpty()) {
            result = g.entities.get(0);
        }
        return result;
    }

    /**
     * 属性椭圆碰撞解决: 属性与属性、属性与实体、属性与菱形相互推离, 迭代至无重叠
     */
    private static void resolveAttributeOverlaps(ErGraph g) {
        List<ErGraph.Attribute> attrs = g.attributes;
        for (int iter = 0; iter < 12; iter++) {
            boolean moved = false;
            // 属性 vs 属性
            for (int i = 0; i < attrs.size(); i++) {
                ErGraph.Attribute a = attrs.get(i);
                for (int j = i + 1; j < attrs.size(); j++) {
                    ErGraph.Attribute b = attrs.get(j);
                    double dx = b.cx - a.cx;
                    double dy = b.cy - a.cy;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double minDist = a.rx + b.rx + 6;
                    if (dist >= minDist) {
                        continue;
                    }
                    if (a.parent == b.parent) {
                        // 同父属性: 各自沿父节点径向向外推, 保持放射状(不产生交叉)
                        if (pushRadialFromParent(a)) {
                            moved = true;
                        }
                        if (pushRadialFromParent(b)) {
                            moved = true;
                        }
                    } else {
                        // 跨父属性: 沿连线方向互相推开
                        double nx, ny;
                        if (dist < 1) {
                            nx = 1;
                            ny = 0;
                        } else {
                            nx = dx / dist;
                            ny = dy / dist;
                        }
                        double push = (minDist - dist) / 2;
                        a.cx -= nx * push;
                        a.cy -= ny * push;
                        b.cx += nx * push;
                        b.cy += ny * push;
                        moved = true;
                    }
                }
            }
            // 属性 vs 实体/菱形(跳过属性自己的父节点, 否则会把线推得断开)
            for (ErGraph.Attribute a : attrs) {
                for (ErGraph.Entity e : g.entities) {
                    if (a.parent == e) {
                        continue;
                    }
                    if (pushAwayFromRectCenter(a, e.cx, e.cy, e.w / 2 + 6, e.h / 2 + 6)) {
                        moved = true;
                    }
                }
                for (ErGraph.Relation r : g.relations) {
                    // 关系属性也要避开父菱形(推到菱形外), 其他属性避开所有菱形
                    double hw = r.hw + (a.parent == r ? 8 : 0);
                    double hh = r.hh + (a.parent == r ? 8 : 0);
                    if (pushAwayFromRectCenter(a, r.cx, r.cy, hw, hh)) {
                        moved = true;
                    }
                }
            }
            if (!moved) {
                break;
            }
        }
    }

    /**
     * 属性沿其父节点(实体/菱形)中心向外径向推 6px, 保持角度不变
     */
    private static boolean pushRadialFromParent(ErGraph.Attribute a) {
        double pcx, pcy;
        if (a.parent instanceof ErGraph.Entity) {
            ErGraph.Entity e = (ErGraph.Entity) a.parent;
            pcx = e.cx;
            pcy = e.cy;
        } else if (a.parent instanceof ErGraph.Relation) {
            ErGraph.Relation r = (ErGraph.Relation) a.parent;
            pcx = r.cx;
            pcy = r.cy;
        } else {
            return false;
        }
        double dx = a.cx - pcx;
        double dy = a.cy - pcy;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6) {
            a.cy = pcy - a.ry - 6;
            return true;
        }
        a.cx += dx / len * 6;
        a.cy += dy / len * 6;
        return true;
    }

    /**
     * 把属性沿"远离矩形中心"方向推出到矩形外(用于避开其他实体/菱形), 返回是否移动
     */
    private static boolean pushAwayFromRectCenter(ErGraph.Attribute a, double rx, double ry, double halfW, double halfH) {
        double dx = a.cx - rx;
        double dy = a.cy - ry;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6) {
            a.cx = rx + halfW + Math.max(a.rx, a.ry) + 4;
            return true;
        }
        double ux = dx / len, uy = dy / len;
        double tH = Math.abs(ux) < 1e-9 ? Double.MAX_VALUE : halfW / Math.abs(ux);
        double tV = Math.abs(uy) < 1e-9 ? Double.MAX_VALUE : halfH / Math.abs(uy);
        double tRect = Math.min(tH, tV);
        double minDist = tRect + Math.max(a.rx, a.ry) + 4;
        if (len < minDist) {
            a.cx = rx + ux * minDist;
            a.cy = ry + uy * minDist;
            return true;
        }
        return false;
    }

    private static void measureAttributes(ErGraph g, Font attrFont) {
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D probe = dummy.createGraphics();
        probe.setFont(attrFont);
        FontMetrics fm = probe.getFontMetrics();
        for (ErGraph.Attribute a : g.attributes) {
            int tw = fm.stringWidth(a.name);
            a.rx = Math.max(tw / 2 + 14, 32);
            a.ry = fm.getHeight() / 2 + 8;
        }
    }

    /**
     * 实体网格排列: 4 个以下用单行/2x2, 更多用 3 列
     */
    private static void placeEntities(ErGraph g) {
        int n = g.entities.size();
        int cols;
        if (n <= 3) {
            cols = n;
        } else if (n <= 4) {
            cols = 2;
        } else if (n <= 6) {
            cols = 3;
        } else {
            cols = 4;
        }
        int rows = (n + cols - 1) / cols;
        double spacingX = ENTITY_GAP;
        double spacingY = ENTITY_GAP * 1.15;

        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols && idx < n; c++, idx++) {
                ErGraph.Entity e = g.entities.get(idx);
                e.cx = MARGIN + c * spacingX + ENTITY_W / 2;
                e.cy = MARGIN + r * spacingY + ENTITY_H / 2;
                e.w = ENTITY_W;
                e.h = ENTITY_H;
            }
        }
    }

    /**
     * 关系菱形: 严格放在两实体连线中点, 碰撞检测后仅在实际重叠时沿垂直方向偏移
     */
    private static void placeRelations(ErGraph g) {
        List<ErGraph.Relation> placed = new ArrayList<>();
        for (ErGraph.Relation rel : g.relations) {
            ErGraph.Entity f = rel.from;
            ErGraph.Entity t = rel.to;
            rel.cx = (f.cx + t.cx) / 2;
            rel.cy = (f.cy + t.cy) / 2;
            rel.hw = REL_HALF_W;
            rel.hh = REL_HALF_H;

            double dx = t.cx - f.cx;
            double dy = t.cy - f.cy;
            double len = Math.sqrt(dx * dx + dy * dy);
            double nx = (len < 1) ? 0 : -dy / len;
            double ny = (len < 1) ? 1 : dx / len;

            int guard = 0;
            while (collides(rel, g, placed) && guard < 30) {
                rel.cx += nx * 24;
                rel.cy += ny * 24;
                guard++;
            }
            placed.add(rel);
        }
    }

    private static boolean collides(ErGraph.Relation r, ErGraph g, List<ErGraph.Relation> placed) {
        double r1x = r.cx - r.hw, r1y = r.cy - r.hh;
        double r2x = r.cx + r.hw, r2y = r.cy + r.hh;
        for (ErGraph.Relation p : placed) {
            if (overlap(r1x, r1y, r2x, r2y, p.cx - p.hw, p.cy - p.hh, p.cx + p.hw, p.cy + p.hh)) {
                return true;
            }
        }
        for (ErGraph.Entity e : g.entities) {
            if (overlap(r1x, r1y, r2x, r2y, e.cx - e.w / 2, e.cy - e.h / 2, e.cx + e.w / 2, e.cy + e.h / 2)) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlap(double ax1, double ay1, double ax2, double ay2,
                                   double bx1, double by1, double bx2, double by2) {
        return ax1 < bx2 && ax2 > bx1 && ay1 < by2 && ay2 > by1;
    }

    /**
     * 属性自动寻位: 每个属性在父节点周围按"朝外/朝下"基准角度,
     * 依次尝试多个角度与半径, 选择第一个不与任何属性/实体/菱形冲突的位置
     */
    private static void placeAttributes(ErGraph g) {
        double centerX = 0;
        double centerY = 0;
        for (ErGraph.Entity e : g.entities) {
            centerX += e.cx;
            centerY += e.cy;
        }
        centerX /= g.entities.size();
        centerY /= g.entities.size();

        List<ErGraph.Attribute> placed = new ArrayList<>();
        // 实体属性: 朝图中心反方向(外侧)
        for (ErGraph.Entity e : g.entities) {
            double outward = Math.atan2(e.cy - centerY, e.cx - centerX);
            placeAttrsSmart(g, e, e.attrs, e.cx, e.cy, outward, true, placed);
        }
        // 关系属性: 朝向菱形外侧(图中心反方向), 自动找空旷方向
        for (ErGraph.Relation rel : g.relations) {
            double outward = Math.atan2(rel.cy - centerY, rel.cx - centerX);
            placeAttrsSmart(g, rel, rel.attrs, rel.cx, rel.cy, outward, false, placed);
        }
    }

    private static void placeAttrsSmart(ErGraph g, Object parent, List<ErGraph.Attribute> attrs,
                                        double pcx, double pcy, double baseAngle,
                                        boolean entity, List<ErGraph.Attribute> placed) {
        if (attrs.isEmpty()) {
            return;
        }
        attrs.sort((a, b) -> {
            if (a.key == b.key) {
                return 0;
            }
            return a.key ? -1 : 1;
        });
        int n = attrs.size();
        // 各属性的目标角度: 围绕 baseAngle 均匀展开
        double span = entity ? Math.PI * 1.1 : Math.PI * 0.85;
        double start = baseAngle - span / 2;
        double step = (n == 1) ? 0 : span / (n - 1);

        for (int i = 0; i < n; i++) {
            ErGraph.Attribute a = attrs.get(i);
            a.parent = parent;
            double target = start + step * i;
            // 综合评分选最佳位置
            double[] best = bestAttrPosition(g, a, pcx, pcy, target, parent, placed);
            a.cx = best[0];
            a.cy = best[1];
            placed.add(a);
        }
    }

    /**
     * 属性最佳位置搜索: 在父节点周围多个角度×半径上,
     * 按(连线交叉数, 连线长度, 与实体/菱形距离)综合评分, 选最优且不冲突的位置
     */
    private static double[] bestAttrPosition(ErGraph g, ErGraph.Attribute a,
                                             double pcx, double pcy, double target,
                                             Object parent, List<ErGraph.Attribute> placed) {
        double bestScore = Double.MAX_VALUE;
        double bx = pcx + Math.cos(target) * ATTR_DIST;
        double by = pcy + Math.sin(target) * ATTR_DIST;
        double[] best = new double[]{bx, by};

        for (int ai = -10; ai <= 10; ai++) {
            double angle = target + ai * (Math.PI / 16);
            for (double radius = ATTR_DIST * 0.75; radius <= ATTR_DIST * 2.4; radius += 18) {
                double cx = pcx + Math.cos(angle) * radius;
                double cy = pcy + Math.sin(angle) * radius;
                // 硬冲突: 压到属性/实体/菱形
                if (conflicts(g, cx, cy, a.rx, a.ry, parent, placed)) {
                    continue;
                }
                // 评分: 越低越好
                double score = 0;
                // 连线长度惩罚(离父节点越远分越高, 但权重小, 优先保证不交叉)
                score += radius * 0.05;
                // 角度偏离惩罚
                score += Math.abs(angle - target) * 2.0;
                // 连线交叉惩罚
                score += countAttrLineCrossings(g, a, cx, cy, parent) * 8.0;
                // 与实体/菱形间距惩罚
                score += nearShapePenalty(g, cx, cy, a.rx, a.ry, parent);
                if (score < bestScore) {
                    bestScore = score;
                    best[0] = cx;
                    best[1] = cy;
                }
            }
        }
        return best;
    }

    /**
     * 统计从属性(cx,cy)到父节点的连线, 与图中其他已存在连线(实体-关系等)的交叉次数
     */
    private static int countAttrLineCrossings(ErGraph g, ErGraph.Attribute a,
                                              double cx, double cy, Object parent) {
        int cross = 0;
        double[] seg;
        if (parent instanceof ErGraph.Entity) {
            ErGraph.Entity me = (ErGraph.Entity) parent;
            seg = new double[]{cx, cy, me.cx, me.cy};
        } else {
            ErGraph.Relation rel = (ErGraph.Relation) parent;
            seg = new double[]{cx, cy, rel.cx, rel.cy};
        }
        // 与所有实体-关系的连线判断交叉(这些是固定的主干连线)
        for (ErGraph.Relation r : g.relations) {
            double[] p1 = rectEdgePoint(r.from.cx, r.from.cy, r.from.w / 2, r.from.h / 2, r.cx, r.cy);
            double[] d1 = diamondEdgePoint(r.cx, r.cy, r.hw, r.hh, r.from.cx, r.from.cy);
            if (segmentCrossPair(seg, p1, d1)) {
                cross++;
            }
            double[] p2 = rectEdgePoint(r.to.cx, r.to.cy, r.to.w / 2, r.to.h / 2, r.cx, r.cy);
            double[] d2 = diamondEdgePoint(r.cx, r.cy, r.hw, r.hh, r.to.cx, r.to.cy);
            if (segmentCrossPair(seg, p2, d2)) {
                cross++;
            }
        }
        return cross;
    }

    private static boolean segmentCrossPair(double[] s, double[] p, double[] q) {
        if (s == null || p == null || q == null) {
            return false;
        }
        double[] cross = segmentCross(new LineSeg(s[0], s[1], s[2], s[3], null),
                new LineSeg(p[0], p[1], q[0], q[1], null));
        if (cross == null) {
            return false;
        }
        double t = cross[2], u = cross[3];
        return t > 0.02 && t < 0.98 && u > 0.02 && u < 0.98;
    }

    /**
     * 距离惩罚: 属性离其他实体/菱形越近分越高
     */
    private static double nearShapePenalty(ErGraph g, double cx, double cy, double rx, double ry, Object parent) {
        double penalty = 0;
        for (ErGraph.Entity e : g.entities) {
            if (parent instanceof ErGraph.Entity && e == parent) {
                continue;
            }
            double d = distToRect(cx, cy, e.cx, e.cy, e.w / 2, e.h / 2);
            if (d < 70) {
                penalty += (70 - d) * 0.3;
            }
        }
        for (ErGraph.Relation r : g.relations) {
            double hw = r.hw + (parent == r ? 10 : 0);
            double hh = r.hh + (parent == r ? 10 : 0);
            double d = distToRect(cx, cy, r.cx, r.cy, hw, hh);
            if (d < 55) {
                penalty += (55 - d) * 0.3;
            }
        }
        return penalty;
    }

    private static double distToRect(double cx, double cy, double rx, double ry, double hw, double hh) {
        double dx = Math.max(Math.abs(cx - rx) - hw, 0);
        double dy = Math.max(Math.abs(cy - ry) - hh, 0);
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 检查椭圆 (cx,cy,rx,ry) 是否与已放置属性、其他实体/菱形冲突
     */
    private static boolean conflicts(ErGraph g, double cx, double cy, double rx, double ry,
                                     Object parent, List<ErGraph.Attribute> placed) {
        for (ErGraph.Attribute b : placed) {
            double d = Math.hypot(b.cx - cx, b.cy - cy);
            if (d < rx + b.rx + 8) {
                return true;
            }
        }
        if (parent instanceof ErGraph.Entity) {
            ErGraph.Entity me = (ErGraph.Entity) parent;
            // 不能压到其他实体
            for (ErGraph.Entity e : g.entities) {
                if (e == me) {
                    continue;
                }
                if (overlapCircleRect(cx, cy, rx, ry, e.cx, e.cy, e.w / 2, e.h / 2)) {
                    return true;
                }
            }
            // 不能压到菱形
            for (ErGraph.Relation r : g.relations) {
                if (overlapCircleRect(cx, cy, rx, ry, r.cx, r.cy, r.hw, r.hh)) {
                    return true;
                }
            }
        } else {
            // 关系属性: 不能压到任何实体/菱形(包括自己的父菱形, 但父菱形用外扩边界留线空间)
            for (ErGraph.Entity e : g.entities) {
                if (overlapCircleRect(cx, cy, rx, ry, e.cx, e.cy, e.w / 2, e.h / 2)) {
                    return true;
                }
            }
            for (ErGraph.Relation r : g.relations) {
                double hw = r.hw + (r == parent ? 8 : 0);
                double hh = r.hh + (r == parent ? 8 : 0);
                if (overlapCircleRect(cx, cy, rx, ry, r.cx, r.cy, hw, hh)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean overlapCircleRect(double cx, double cy, double rx, double ry,
                                             double rx2, double ry2, double hw, double hh) {
        double dx = Math.abs(cx - rx2);
        double dy = Math.abs(cy - ry2);
        if (dx > hw + rx || dy > hh + ry) {
            return false;
        }
        if (dx <= hw || dy <= hh) {
            return true;
        }
        double dxc = dx - hw;
        double dyc = dy - hh;
        return dxc * dxc + dyc * dyc <= Math.max(rx, ry) * Math.max(rx, ry);
    }

    /**
     * 碰撞解决后按角度重排: 确保从父节点到属性的连线按角度有序，不交叉
     */
    private static void sortAttrsByAngle(ErGraph g) {
        for (ErGraph.Entity e : g.entities) {
            e.attrs.sort((a, b) -> {
                double aa = Math.atan2(a.cy - e.cy, a.cx - e.cx);
                double ab = Math.atan2(b.cy - e.cy, b.cx - e.cx);
                return Double.compare(aa, ab);
            });
        }
        for (ErGraph.Relation rel : g.relations) {
            rel.attrs.sort((a, b) -> {
                double aa = Math.atan2(a.cy - rel.cy, a.cx - rel.cx);
                double ab = Math.atan2(b.cy - rel.cy, b.cx - rel.cx);
                return Double.compare(aa, ab);
            });
        }
    }

    /**
     * 全局线段相交检测: 对相交的连线, 偏移其中一个属性端点(沿垂直连线方向),
     * 迭代直至无相交或达上限
     */
    private static void resolveLineCrossings(ErGraph g) {
        for (int iter = 0; iter < 8; iter++) {
            List<LineSeg> segs = collectLineSegments(g);
            boolean moved = false;
            for (int i = 0; i < segs.size(); i++) {
                for (int j = i + 1; j < segs.size(); j++) {
                    LineSeg a = segs.get(i);
                    LineSeg b = segs.get(j);
                    double[] cross = segmentCross(a, b);
                    if (cross == null) {
                        continue;
                    }
                    double t = cross[2], s = cross[3];
                    if (t <= 0.01 || t >= 0.99 || s <= 0.01 || s >= 0.99) {
                        continue;
                    }
                    // 相交: 偏移一个属性端点(选择非共享形状的那个)
                    ErGraph.Attribute victim = pickVictimAttribute(a, b);
                    if (victim == null) {
                        continue;
                    }
                    pushPerpendicular(victim, a, b, t, s);
                    moved = true;
                }
            }
            if (!moved) {
                break;
            }
        }
    }

    /**
     * 选择要偏移的属性: 偏向被多线段共享的形状不变, 偏移另一端
     */
    private static ErGraph.Attribute pickVictimAttribute(LineSeg a, LineSeg b) {
        if (a.attr != null && b.attr == null) {
            return a.attr;
        }
        if (b.attr != null && a.attr == null) {
            return b.attr;
        }
        if (a.attr != null) {
            return a.attr;
        }
        return null;
    }

    /**
     * 沿垂直于两条相交线段中点连线的方向, 偏移属性端点 10 像素
     */
    private static void pushPerpendicular(ErGraph.Attribute a, LineSeg l1, LineSeg l2, double t, double s) {
        // 交点
        double cx = l1.x1 + (l1.x2 - l1.x1) * t;
        double cy = l1.y1 + (l1.y2 - l1.y1) * t;
        // 另一条线段在交点处切向
        double tx = l2.x2 - l2.x1;
        double ty = l2.y2 - l2.y1;
        double tlen = Math.sqrt(tx * tx + ty * ty);
        if (tlen < 1e-6) {
            return;
        }
        // 垂直方向
        double nx = -ty / tlen;
        double ny = tx / tlen;
        // 把属性沿垂直方向推出
        a.cx += nx * 10;
        a.cy += ny * 10;
    }

    /**
     * 线段两两相交检测; 返回 {crossX, crossY, t, s} 或 null
     * t, s 是两线段参数 (0..1)
     */
    private static double[] segmentCross(LineSeg a, LineSeg b) {
        double x1 = a.x1, y1 = a.y1, x2 = a.x2, y2 = a.y2;
        double x3 = b.x1, y3 = b.y1, x4 = b.x2, y4 = b.y2;
        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 1e-9) {
            return null;
        }
        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        double s = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom;
        if (t < 0 || t > 1 || s < 0 || s > 1) {
            return null;
        }
        double cx = x1 + (x2 - x1) * t;
        double cy = y1 + (y2 - y1) * t;
        return new double[]{cx, cy, t, s};
    }

    /**
     * 收集所有要绘制的连线段(实体-属性、实体-关系、关系-属性)
     */
    private static List<LineSeg> collectLineSegments(ErGraph g) {
        List<LineSeg> list = new ArrayList<>();
        for (ErGraph.Entity e : g.entities) {
            for (ErGraph.Attribute a : e.attrs) {
                list.add(makeEntityAttrSeg(e, a));
            }
            for (ErGraph.Relation r : g.relations) {
                if (r.from == e || r.to == e) {
                    list.add(makeEntityRelSeg(e, r));
                }
            }
        }
        for (ErGraph.Relation r : g.relations) {
            for (ErGraph.Attribute a : r.attrs) {
                list.add(makeRelAttrSeg(r, a));
            }
        }
        return list;
    }

    private static LineSeg makeEntityAttrSeg(ErGraph.Entity e, ErGraph.Attribute a) {
        double[] p = rectEdgePoint(e.cx, e.cy, e.w / 2, e.h / 2, a.cx, a.cy);
        double ex = a.cx, ey = a.cy;
        double[] oval = ellipseEdgePoint(a.cx, a.cy, a.rx, a.ry, e.cx, e.cy);
        return new LineSeg(p[0], p[1], oval[0], oval[1], a);
    }

    private static LineSeg makeEntityRelSeg(ErGraph.Entity e, ErGraph.Relation r) {
        double[] p = rectEdgePoint(e.cx, e.cy, e.w / 2, e.h / 2, r.cx, r.cy);
        double[] d = diamondEdgePoint(r.cx, r.cy, r.hw, r.hh, e.cx, e.cy);
        return new LineSeg(p[0], p[1], d[0], d[1], null);
    }

    private static LineSeg makeRelAttrSeg(ErGraph.Relation r, ErGraph.Attribute a) {
        double[] d = diamondEdgePoint(r.cx, r.cy, r.hw, r.hh, a.cx, a.cy);
        double[] oval = ellipseEdgePoint(a.cx, a.cy, a.rx, a.ry, r.cx, r.cy);
        return new LineSeg(d[0], d[1], oval[0], oval[1], a);
    }

    private static double[] rectEdgePoint(double cx, double cy, double hw, double hh, double tx, double ty) {
        double dx = tx - cx, dy = ty - cy;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6) return new double[]{cx, cy};
        double ux = dx / len, uy = dy / len;
        double tH = Math.abs(ux) < 1e-9 ? 1e9 : hw / Math.abs(ux);
        double tV = Math.abs(uy) < 1e-9 ? 1e9 : hh / Math.abs(uy);
        double t = Math.min(tH, tV);
        return new double[]{cx + ux * t, cy + uy * t};
    }

    /**
     * 菱形角的交点: 根据方向主导选左/右或上/下的顶点(与渲染的 diamondCorner 一致)
     */
    private static double[] diamondEdgePoint(double cx, double cy, double hw, double hh, double tx, double ty) {
        double dx = tx - cx, dy = ty - cy;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6) return new double[]{cx, cy};
        double ux = dx / len, uy = dy / len;
        if (Math.abs(ux) * hh >= Math.abs(uy) * hw) {
            // 左角或右角
            return new double[]{cx + (ux >= 0 ? hw : -hw), cy};
        }
        // 上角或下角
        return new double[]{cx, cy + (uy >= 0 ? hh : -hh)};
    }

    private static double[] ellipseEdgePoint(double cx, double cy, double rx, double ry, double tx, double ty) {
        double dx = tx - cx, dy = ty - cy;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6) return new double[]{cx + rx, cy};
        double ux = dx / len, uy = dy / len;
        double t = 1.0 / Math.sqrt((ux * ux) / (rx * rx) + (uy * uy) / (ry * ry));
        return new double[]{cx - ux * (t + 3), cy - uy * (t + 3)};
    }

    private static class LineSeg {
        final double x1, y1, x2, y2;
        final ErGraph.Attribute attr;
        LineSeg(double x1, double y1, double x2, double y2, ErGraph.Attribute attr) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.attr = attr;
        }
    }

    private static void computeCanvas(ErGraph g) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (ErGraph.Entity e : g.entities) {
            minX = Math.min(minX, e.cx - e.w / 2 - 20);
            maxX = Math.max(maxX, e.cx + e.w / 2 + 20);
            minY = Math.min(minY, e.cy - e.h / 2 - 20);
            maxY = Math.max(maxY, e.cy + e.h / 2 + 20);
        }
        for (ErGraph.Relation r : g.relations) {
            minX = Math.min(minX, r.cx - r.hw - 20);
            maxX = Math.max(maxX, r.cx + r.hw + 20);
            minY = Math.min(minY, r.cy - r.hh - 20);
            maxY = Math.max(maxY, r.cy + r.hh + 20);
        }
        for (ErGraph.Attribute a : g.attributes) {
            minX = Math.min(minX, a.cx - a.rx - 10);
            maxX = Math.max(maxX, a.cx + a.rx + 10);
            minY = Math.min(minY, a.cy - a.ry - 10);
            maxY = Math.max(maxY, a.cy + a.ry + 10);
        }
        g.width = (int) (maxX - minX + 180);
        g.height = (int) (maxY - minY + 180);
        double offX = minX - 90;
        double offY = minY - 90;
        for (ErGraph.Entity e : g.entities) {
            e.cx -= offX;
            e.cy -= offY;
        }
        for (ErGraph.Relation r : g.relations) {
            r.cx -= offX;
            r.cy -= offY;
        }
        for (ErGraph.Attribute a : g.attributes) {
            a.cx -= offX;
            a.cy -= offY;
        }
    }
}
