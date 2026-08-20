package com.graduate.thesis.service.captcha;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 点选验证码: 多模板随机(字符/图形/颜色), 依次点击指定目标
 */
@Component
public class ClickGenerator implements CaptchaGenerator {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 160;
    private static final int COLS = 6;
    private static final int ROWS = 2;
    private static final int TARGET_COUNT = 3;
    private static final int DISTRACT_COUNT = 5;
    private static final int HIT_RADIUS = 20;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Override
    public String type() {
        return "CLICK";
    }

    @Override
    public CaptchaData generate() {
        Random r = ThreadLocalRandom.current();
        int tpl = r.nextInt(3); // 0字符 1图形 2颜色

        // 6x2 网格打乱取 8 格, 字符间距 >=50 远大于命中半径 20
        int cellW = WIDTH / COLS;
        int cellH = HEIGHT / ROWS;
        List<int[]> cells = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                cells.add(new int[]{col, row});
            }
        }
        Collections.shuffle(cells, r);
        int total = TARGET_COUNT + DISTRACT_COUNT;
        List<int[]> used = new ArrayList<>(cells.subList(0, total));
        List<int[]> allCenters = new ArrayList<>();
        for (int[] cell : used) {
            int cx = cell[0] * cellW + cellW / 2 + (r.nextInt(20) - 10);
            int cy = cell[1] * cellH + cellH / 2 + (r.nextInt(20) - 10);
            allCenters.add(new int[]{cx, cy});
        }
        List<int[]> targetCenters = new ArrayList<>(allCenters.subList(0, TARGET_COUNT));

        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 3; i++) {
            g.setColor(new Color(r.nextInt(160), r.nextInt(160), r.nextInt(160), 120));
            g.drawLine(r.nextInt(WIDTH), r.nextInt(HEIGHT), r.nextInt(WIDTH), r.nextInt(HEIGHT));
        }

        String tip;
        if (tpl == 0) {
            tip = drawCharTemplate(g, r, allCenters);
        } else if (tpl == 1) {
            tip = drawShapeTemplate(g, r, allCenters);
        } else {
            tip = drawColorTemplate(g, r, allCenters);
        }
        g.dispose();

        Map<String, Object> render = new HashMap<>();
        render.put("backgroundImage", CaptchaGenerator.toBase64(img));
        render.put("tip", tip);
        render.put("width", WIDTH);
        render.put("height", HEIGHT);
        render.put("targetCount", TARGET_COUNT);
        return new CaptchaData("CLICK", render, new ClickSecret(targetCenters));
    }

    /** 模板1: 字符点选(ASCII, Arial 渲染无字体风险) */
    private String drawCharTemplate(Graphics2D g, Random r, List<int[]> centers) {
        List<Character> pool = new ArrayList<>();
        for (char c : CHARS.toCharArray()) {
            pool.add(c);
        }
        Collections.shuffle(pool, r);
        List<Character> chars = new ArrayList<>(pool.subList(0, centers.size()));
        Font font = new Font("Arial", Font.BOLD, 28);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);
        for (int i = 0; i < centers.size(); i++) {
            int[] c = centers.get(i);
            String s = String.valueOf(chars.get(i));
            g.setColor(new Color(r.nextInt(80), r.nextInt(100), 180));
            g.drawString(s, c[0] - fm.stringWidth(s) / 2, c[1] + fm.getAscent() / 2);
        }
        return joinTip(new ArrayList<>(chars.subList(0, TARGET_COUNT)));
    }

    /** 模板2: 图形点选(圆/三角/方/星), 3 目标形状唯一, 干扰用第 4 形状 */
    private String drawShapeTemplate(Graphics2D g, Random r, List<int[]> centers) {
        String[] shapes = {"CIRCLE", "TRIANGLE", "SQUARE", "STAR"};
        String[] symbols = {"●", "▲", "■", "★"};
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            idx.add(i);
        }
        Collections.shuffle(idx, r);
        // 前 3 位置放目标(各用 idx[0..2] 形状, 唯一), 后 5 位置放干扰(全用 idx[3] 形状)
        for (int i = 0; i < centers.size(); i++) {
            int[] c = centers.get(i);
            int shapeIdx = (i < TARGET_COUNT) ? idx.get(i) : idx.get(3);
            g.setColor(new Color(60 + r.nextInt(120), 80 + r.nextInt(120), 160 + r.nextInt(80)));
            drawShape(g, shapes[shapeIdx], c[0], c[1], 16);
        }
        List<String> tipSymbols = new ArrayList<>();
        for (int i = 0; i < TARGET_COUNT; i++) {
            tipSymbols.add(symbols[idx.get(i)]);
        }
        return joinTip(tipSymbols);
    }

    /** 模板3: 颜色点选, 3 目标颜色唯一, 干扰用其他颜色 */
    private String drawColorTemplate(Graphics2D g, Random r, List<int[]> centers) {
        Color[] palette = {
            new Color(245, 108, 108), new Color(64, 158, 255), new Color(103, 194, 58),
            new Color(230, 162, 60), new Color(144, 147, 153), new Color(114, 46, 209)
        };
        String[] names = {"红", "蓝", "绿", "橙", "灰", "紫"};
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < palette.length; i++) {
            idx.add(i);
        }
        Collections.shuffle(idx, r);
        // 目标 3 颜色(idx 0,1,2), 干扰 5 用 idx 3,4,5 循环
        for (int i = 0; i < centers.size(); i++) {
            int[] c = centers.get(i);
            int colorIdx = (i < TARGET_COUNT) ? idx.get(i) : idx.get(3 + (i - TARGET_COUNT) % 3);
            g.setColor(palette[colorIdx]);
            g.fillOval(c[0] - 16, c[1] - 16, 32, 32);
            g.setColor(new Color(255, 255, 255, 180));
            g.setStroke(new BasicStroke(2));
            g.drawOval(c[0] - 16, c[1] - 16, 32, 32);
        }
        List<String> tipNames = new ArrayList<>();
        for (int i = 0; i < TARGET_COUNT; i++) {
            tipNames.add(names[idx.get(i)]);
        }
        return joinTip(tipNames);
    }

    private void drawShape(Graphics2D g, String shape, int cx, int cy, int size) {
        switch (shape) {
            case "CIRCLE":
                g.fillOval(cx - size, cy - size, size * 2, size * 2);
                break;
            case "TRIANGLE": {
                Polygon p = new Polygon();
                p.addPoint(cx, cy - size);
                p.addPoint(cx - size, cy + size);
                p.addPoint(cx + size, cy + size);
                g.fill(p);
                break;
            }
            case "SQUARE":
                g.fillRect(cx - size, cy - size, size * 2, size * 2);
                break;
            case "STAR":
                g.fill(createStar(cx, cy, size, size / 2));
                break;
            default:
                break;
        }
    }

    private Polygon createStar(int cx, int cy, int outer, int inner) {
        Polygon p = new Polygon();
        for (int i = 0; i < 10; i++) {
            int radius = (i % 2 == 0) ? outer : inner;
            double angle = Math.PI / 5 * i - Math.PI / 2;
            p.addPoint((int) (cx + radius * Math.cos(angle)), (int) (cy + radius * Math.sin(angle)));
        }
        return p;
    }

    private String joinTip(List<?> items) {
        StringBuilder tip = new StringBuilder("请依次点击: ");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                tip.append(' ');
            }
            tip.append(items.get(i));
        }
        return tip.toString();
    }

    @Override
    public boolean verify(Object secret, JsonNode payload) {
        if (!(secret instanceof ClickSecret)) {
            return false;
        }
        ClickSecret s = (ClickSecret) secret;
        JsonNode points = payload.path("points");
        if (!points.isArray() || points.size() != s.targetCenters.size()) {
            return false;
        }
        for (int i = 0; i < s.targetCenters.size(); i++) {
            JsonNode p = points.get(i);
            if (p == null || !p.isArray() || p.size() < 2) {
                return false;
            }
            int px = p.get(0).asInt();
            int py = p.get(1).asInt();
            int[] t = s.targetCenters.get(i);
            if (Math.hypot(px - t[0], py - t[1]) > HIT_RADIUS) {
                return false;
            }
        }
        return true;
    }

    /** 点选秘密: 目标字符中心坐标(按提示顺序) */
    static class ClickSecret {
        final List<int[]> targetCenters;
        ClickSecret(List<int[]> targetCenters) {
            this.targetCenters = targetCenters;
        }
    }
}
