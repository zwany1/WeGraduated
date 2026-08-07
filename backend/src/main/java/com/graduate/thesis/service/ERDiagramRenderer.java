package com.graduate.thesis.service;

import com.graduate.thesis.dto.ErDTO;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Chen 记法 ER 图渲染: 矩形(实体)/椭圆(属性)/菱形(关系) + 连线
 */
public final class ERDiagramRenderer {

    private static final Color COLOR_SHAPE = new Color(0x1A1A2E);
    private static final Color COLOR_KEY = new Color(0x1A1A2E);
    private static final Color COLOR_MUTED = new Color(0x6B7280);

    private ERDiagramRenderer() {
    }

    public static byte[] render(ErDTO dto) {
        int base = dto.getFontSize() != null && dto.getFontSize() > 0 ? dto.getFontSize() : 12;
        String cnFont = pickCjkFont();
        Font attrFont = new Font(cnFont, Font.PLAIN, base);

        ErGraph g = ERModelNormalizer.normalize(dto);
        if (g.entities.isEmpty()) {
            return new byte[0];
        }
        ERLayoutEngine.layout(g, attrFont);

        BufferedImage img = new BufferedImage(g.width, g.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = img.createGraphics();
        gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gr.setColor(Color.WHITE);
        gr.fillRect(0, 0, g.width, g.height);

        // 1. 连线
        gr.setColor(COLOR_SHAPE);
        gr.setStroke(new BasicStroke(1.2f));
        for (ErGraph.Entity e : g.entities) {
            for (ErGraph.Attribute a : e.attrs) {
                drawLineShapeToOval(gr, e.cx, e.cy, e.w / 2, e.h / 2, a);
            }
            for (ErGraph.Relation rel : g.relations) {
                if (rel.from == e || rel.to == e) {
                    drawLineEntityToDiamond(gr, e, rel);
                }
            }
        }
        for (ErGraph.Relation rel : g.relations) {
            for (ErGraph.Attribute a : rel.attrs) {
                drawLineDiamondToOval(gr, rel, a);
            }
        }

        // 2. 属性椭圆
        gr.setFont(attrFont);
        FontMetrics fm = gr.getFontMetrics();
        for (ErGraph.Attribute a : g.attributes) {
            drawOval(gr, a, attrFont, fm);
        }

        // 3. 关系菱形
        for (ErGraph.Relation rel : g.relations) {
            drawDiamond(gr, rel, attrFont, fm);
        }

        // 4. 实体矩形
        for (ErGraph.Entity e : g.entities) {
            drawRectangle(gr, e, attrFont, fm);
        }

        // 5. 基数标注(最后画, 保证不被菱形/实体覆盖, 正确"嵌入"线中)
        gr.setFont(attrFont);
        FontMetrics cardFm = gr.getFontMetrics();
        for (ErGraph.Relation rel : g.relations) {
            drawCardinality(gr, rel, cardFm);
        }

        gr.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("ER 图生成失败: " + ex.getMessage(), ex);
        }
    }

    private static void drawLineShapeToOval(Graphics2D gr, double sx, double sy, double sw2, double sh2, ErGraph.Attribute a) {
        double[] inter = intersectRectToOval(sx, sy, sw2, sh2, a);
        gr.draw(new Line2D.Double(inter[0], inter[1], inter[2], inter[3]));
    }

    private static void drawLineDiamondToOval(Graphics2D gr, ErGraph.Relation rel, ErGraph.Attribute a) {
        double dx = a.cx - rel.cx;
        double dy = a.cy - rel.cy;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        double ux = dx / len, uy = dy / len;
        // 菱形边交点(非顶点, 避免连线从尖角穿入菱形内部)
        double tDia = 1.0 / (Math.abs(ux) / rel.hw + Math.abs(uy) / rel.hh);
        double dx1 = rel.cx + ux * tDia;
        double dy1 = rel.cy + uy * tDia;
        // 椭圆边缘精确交点(方向从菱形中心指向属性)
        double[] oval = ellipseEdge(a.cx, a.cy, a.rx, a.ry, dx / len, dy / len);
        gr.draw(new Line2D.Double(dx1, dy1, oval[0], oval[1]));
    }

    private static void drawLineEntityToDiamond(Graphics2D gr, ErGraph.Entity e, ErGraph.Relation rel) {
        double dx = rel.cx - e.cx;
        double dy = rel.cy - e.cy;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        double ux = dx / len, uy = dy / len;
        // 实体边缘
        double hw = e.w / 2, hh = e.h / 2;
        double tH = Math.abs(ux) < 1e-9 ? Double.MAX_VALUE : hw / Math.abs(ux);
        double tV = Math.abs(uy) < 1e-9 ? Double.MAX_VALUE : hh / Math.abs(uy);
        double tRect = Math.min(tH, tV);
        double sx = e.cx + ux * tRect;
        double sy = e.cy + uy * tRect;
        // 菱形边交点(与 drawOneCardinality 同一算法, 保证基数落在真实连线上)
        double tDia = 1.0 / (Math.abs(ux) / rel.hw + Math.abs(uy) / rel.hh);
        double ex = rel.cx - ux * tDia;
        double ey = rel.cy - uy * tDia;
        gr.draw(new Line2D.Double(sx, sy, ex, ey));
    }

    /**
     * 求菱形朝向方向 (dx,dy) 的角(顶点):
     * 水平分量主导时在左/右顶点, 垂直分量主导时在上/下顶点
     */
    private static double[] diamondCorner(ErGraph.Relation rel, double dx, double dy) {
        if (Math.abs(dx) * rel.hh >= Math.abs(dy) * rel.hw) {
            // 左顶点或右顶点(水平方向)
            return new double[]{rel.cx + (dx >= 0 ? rel.hw : -rel.hw), rel.cy};
        }
        // 上顶点或下顶点(垂直方向)
        return new double[]{rel.cx, rel.cy + (dy >= 0 ? rel.hh : -rel.hh)};
    }

    private static double[] intersectRectToOval(double sx, double sy, double sw2, double sh2, ErGraph.Attribute a) {
        double dx = a.cx - sx;
        double dy = a.cy - sy;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) {
            return new double[]{sx - sw2, sy, a.cx + a.rx, a.cy};
        }
        double ux = dx / len, uy = dy / len;
        // 矩形边缘交点: 射线先到达的边
        double tH = Math.abs(ux) < 1e-9 ? Double.MAX_VALUE : sw2 / Math.abs(ux);
        double tV = Math.abs(uy) < 1e-9 ? Double.MAX_VALUE : sh2 / Math.abs(uy);
        double tRect = Math.min(tH, tV);
        double x1 = sx + ux * tRect;
        double y1 = sy + uy * tRect;
        // 椭圆边缘精确交点(方向 -ux,-uy)
        double[] oval = ellipseEdge(a.cx, a.cy, a.rx, a.ry, -ux, -uy);
        return new double[]{x1, y1, oval[0], oval[1]};
    }

    /**
     * 椭圆中心 (cx,cy), 半径 (rx,ry), 沿方向 (dx,dy) 的椭圆边缘点, 再沿该方向延伸 3 像素
     * 椭圆参数方程: P = (cx + rx*cosθ, cy + ry*sinθ), 射线方向 (dx,dy)
     * 由 rx*cosθ = t*dx, ry*sinθ = t*dy 且 cos²θ+sin²θ=1 => t = 1/sqrt((dx/rx)²+(dy/ry)²)
     */
    private static double[] ellipseEdge(double cx, double cy, double rx, double ry, double dx, double dy) {
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-9) {
            return new double[]{cx + rx, cy};
        }
        double ux = dx / len, uy = dy / len;
        double inv = Math.sqrt((ux * ux) / (rx * rx) + (uy * uy) / (ry * ry));
        double t = 1.0 / inv;
        double over = 3.0 / len;
        t += over;
        return new double[]{cx + ux * t, cy + uy * t};
    }

    private static void drawCardinality(Graphics2D gr, ErGraph.Relation rel, FontMetrics fm) {
        // 基数标注放在"实体边缘 -> 菱形角"实际连线的中点附近, 与连线对应
        drawOneCardinality(gr, rel, rel.from, rel.fromCard, fm);
        drawOneCardinality(gr, rel, rel.to, rel.toCard, fm);
    }

    private static void drawOneCardinality(Graphics2D gr, ErGraph.Relation rel,
                                           ErGraph.Entity e, String card, FontMetrics fm) {
        if (card == null || card.isEmpty()) {
            return;
        }
        // 端点算法与 drawLineEntityToDiamond 完全一致, 保证基数落在真实连线上
        double dx = rel.cx - e.cx;
        double dy = rel.cy - e.cy;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) {
            return;
        }
        double ux = dx / len, uy = dy / len;
        // 实体边缘
        double hw = e.w / 2, hh = e.h / 2;
        double tH = Math.abs(ux) < 1e-9 ? Double.MAX_VALUE : hw / Math.abs(ux);
        double tV = Math.abs(uy) < 1e-9 ? Double.MAX_VALUE : hh / Math.abs(uy);
        double tRect = Math.min(tH, tV);
        double sx = e.cx + ux * tRect;
        double sy = e.cy + uy * tRect;
        // 菱形边交点(非顶点, 与连线绘制算法一致)
        double tDia = 1.0 / (Math.abs(ux) / rel.hw + Math.abs(uy) / rel.hh);
        double ex = rel.cx - ux * tDia;
        double ey = rel.cy - uy * tDia;
        // 连线中点
        double midX = sx + (ex - sx) * 0.5;
        double midY = sy + (ey - sy) * 0.5;
        int w = fm.stringWidth(card);
        int fh = fm.getHeight();
        // 沿线方向用白色粗线覆盖中点附近, 精确截断线段
        double segUx = (ex - sx) / len;
        double segUy = (ey - sy) / len;
        double half = (w / 2 + 8);
        double bx1 = midX - segUx * half;
        double by1 = midY - segUy * half;
        double bx2 = midX + segUx * half;
        double by2 = midY + segUy * half;
        gr.setColor(Color.WHITE);
        gr.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        gr.draw(new Line2D.Double(bx1, by1, bx2, by2));
        // 水平文字(不画白底矩形, 避免覆盖属性椭圆; 细白线已截断连线)
        gr.setColor(COLOR_MUTED);
        gr.setFont(fm.getFont());
        gr.drawString(card, (int) (midX - w / 2), (int) (midY + fm.getAscent() / 2 - 1));
    }

    private static void drawOval(Graphics2D gr, ErGraph.Attribute a, Font font, FontMetrics fm) {
        Shape oval = new Ellipse2D.Double(a.cx - a.rx, a.cy - a.ry, a.rx * 2, a.ry * 2);
        gr.setColor(Color.WHITE);
        gr.fill(oval);
        gr.setColor(COLOR_SHAPE);
        gr.setStroke(new BasicStroke(1.2f));
        gr.draw(oval);
        gr.setFont(font);
        gr.setColor(COLOR_KEY);
        int tw = fm.stringWidth(a.name);
        gr.drawString(a.name, (int)(a.cx - tw / 2), (int)(a.cy + fm.getAscent() / 2));
        if (a.key) {
            gr.drawLine((int)(a.cx - tw / 2), (int)(a.cy + fm.getAscent() / 2 + 2),
                        (int)(a.cx + tw / 2), (int)(a.cy + fm.getAscent() / 2 + 2));
        }
    }

    private static void drawDiamond(Graphics2D gr, ErGraph.Relation rel, Font font, FontMetrics fm) {
        int[] xs = {(int)(rel.cx - rel.hw), (int)rel.cx, (int)(rel.cx + rel.hw), (int)rel.cx};
        int[] ys = {(int)rel.cy, (int)(rel.cy - rel.hh), (int)rel.cy, (int)(rel.cy + rel.hh)};
        gr.setColor(Color.WHITE);
        gr.fillPolygon(xs, ys, 4);
        gr.setColor(COLOR_SHAPE);
        gr.setStroke(new BasicStroke(1.5f));
        gr.drawPolygon(xs, ys, 4);
        gr.setFont(font);
        int tw = fm.stringWidth(rel.name);
        gr.drawString(rel.name, (int)(rel.cx - tw / 2), (int)(rel.cy + fm.getAscent() / 2));
    }

    private static void drawRectangle(Graphics2D gr, ErGraph.Entity e, Font font, FontMetrics fm) {
        gr.setColor(Color.WHITE);
        gr.fillRect((int)(e.cx - e.w / 2), (int)(e.cy - e.h / 2), (int)e.w, (int)e.h);
        gr.setColor(COLOR_SHAPE);
        gr.setStroke(new BasicStroke(1.5f));
        gr.drawRect((int)(e.cx - e.w / 2), (int)(e.cy - e.h / 2), (int)e.w, (int)e.h);
        gr.setFont(font);
        int tw = fm.stringWidth(e.name);
        gr.drawString(e.name, (int)(e.cx - tw / 2), (int)(e.cy + fm.getAscent() / 2));
    }

    private static String pickCjkFont() {
        String[] names;
        try {
            names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        } catch (Throwable t) {
            names = new String[0];
        }
        String[] prefs = {"微软雅黑", "Microsoft YaHei", "宋体", "SimSun", "黑体", "SimHei"};
        for (String p : prefs) {
            for (String n : names) {
                if (n.equalsIgnoreCase(p)) return n;
            }
        }
        return Font.SANS_SERIF;
    }
}
