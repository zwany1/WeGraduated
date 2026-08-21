package com.graduate.thesis.service.captcha;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
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
 * 文字还原拖拽(参考极验五子棋意象): 拖动打乱的圆形棋子到正确顺序组成单词
 */
@Component
public class ReorderGenerator implements CaptchaGenerator {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 180;
    private static final int RADIUS = 18;
    private static final String[] WORDS = {
        "FORM", "WORD", "TEXT", "CODE", "PAGE", "FILE",
        "DATA", "INFO", "LINK", "SORT", "TYPE", "RULE"
    };
    private static final String[] COLORS = {
        "#409eff", "#67c23a", "#e6a23c", "#f56c6c", "#722ed1", "#13c2c2"
    };

    @Override
    public String type() {
        return "REORDER";
    }

    @Override
    public CaptchaData generate() {
        Random r = ThreadLocalRandom.current();
        String word = WORDS[r.nextInt(WORDS.length)];
        int n = word.length();

        // 打乱字母顺序: order[i] = 第 i 个 token 对应 word 的第几个字母
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            order.add(i);
        }
        Collections.shuffle(order, r);
        // tokenChar[k] = 第 k 个棋子的字符(校验时按字符而非棋子 id 比对, 相同字母互换不算错)
        char[] tokenChar = new char[n];
        for (int k = 0; k < n; k++) {
            tokenChar[k] = word.charAt(order.get(k));
        }

        // 槽位: 画布下半部均匀分布
        List<int[]> slots = new ArrayList<>();
        int gap = WIDTH / (n + 1);
        for (int i = 0; i < n; i++) {
            slots.add(new int[]{gap * (i + 1), 130});
        }

        // 棋子散布: 上半部随机不重叠
        List<int[]> tokenPos = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int tx = 40 + r.nextInt(WIDTH - 80);
            int ty = 30 + r.nextInt(40);
            int tries = 0;
            while (overlaps(tokenPos, tx, ty) && tries < 50) {
                tx = 40 + r.nextInt(WIDTH - 80);
                ty = 30 + r.nextInt(40);
                tries++;
            }
            tokenPos.add(new int[]{tx, ty});
        }

        // 棋子颜色随机(与顺序无关, 不暗示答案)
        List<Integer> colorIdx = new ArrayList<>();
        for (int i = 0; i < COLORS.length; i++) {
            colorIdx.add(i);
        }
        Collections.shuffle(colorIdx, r);
        List<String> tokenColors = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tokenColors.add(COLORS[colorIdx.get(i % COLORS.length)]);
        }

        // 画背景: 棋盘格线 + 槽位虚线轮廓(带序号)
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(222, 225, 232));
        g.setStroke(new BasicStroke(1));
        for (int x = 0; x <= WIDTH; x += 20) {
            g.drawLine(x, 0, x, HEIGHT);
        }
        for (int y = 0; y <= HEIGHT; y += 20) {
            g.drawLine(0, y, WIDTH, y);
        }
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6, 4}, 0));
        g.setFont(new Font("Arial", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < n; i++) {
            int[] s = slots.get(i);
            g.setColor(new Color(180, 190, 200));
            g.drawOval(s[0] - RADIUS, s[1] - RADIUS, RADIUS * 2, RADIUS * 2);
            String num = String.valueOf(i + 1);
            g.drawString(num, s[0] - fm.stringWidth(num) / 2, s[1] + fm.getAscent() / 2);
        }
        g.dispose();

        List<Map<String, Object>> tokens = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Map<String, Object> tk = new HashMap<>();
            tk.put("id", i);
            tk.put("char", String.valueOf(word.charAt(order.get(i))));
            tk.put("color", tokenColors.get(i));
            tk.put("x", tokenPos.get(i)[0]);
            tk.put("y", tokenPos.get(i)[1]);
            tokens.add(tk);
        }
        List<Map<String, Object>> slotList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Map<String, Object> sl = new HashMap<>();
            sl.put("x", slots.get(i)[0]);
            sl.put("y", slots.get(i)[1]);
            slotList.add(sl);
        }

        Map<String, Object> render = new HashMap<>();
        render.put("backgroundImage", CaptchaGenerator.toBase64(img));
        render.put("tokens", tokens);
        render.put("slots", slotList);
        render.put("radius", RADIUS);
        render.put("tip", "请拖动棋子组成: " + word);
        render.put("width", WIDTH);
        render.put("height", HEIGHT);
        render.put("count", n);
        return new CaptchaData("REORDER", render, new ReorderSecret(word, tokenChar));
    }

    private boolean overlaps(List<int[]> existing, int x, int y) {
        for (int[] p : existing) {
            if (Math.hypot(p[0] - x, p[1] - y) < RADIUS * 2 + 6) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean verify(Object secret, JsonNode payload) {
        if (!(secret instanceof ReorderSecret)) {
            return false;
        }
        ReorderSecret s = (ReorderSecret) secret;
        JsonNode slots = payload.path("slots");
        if (!slots.isArray() || slots.size() != s.word.length()) {
            return false;
        }
        // 按"每个槽位应放的字符"比对: 相同字母的不同棋子互换位置仍算正确;
        // 同时要求每个棋子只能用一个槽位, 防止用同一棋子填多个相同字母槽位
        boolean[] used = new boolean[s.tokenChar.length];
        for (int i = 0; i < s.word.length(); i++) {
            int tid = slots.get(i).asInt();
            if (tid < 0 || tid >= s.tokenChar.length || used[tid]) {
                return false;
            }
            used[tid] = true;
            if (s.tokenChar[tid] != s.word.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /** 还原秘密: 目标词 + 各棋子字符(按棋子 id 索引) */
    static class ReorderSecret {
        final String word;
        final char[] tokenChar;
        ReorderSecret(String word, char[] tokenChar) {
            this.word = word;
            this.tokenChar = tokenChar;
        }
    }
}
