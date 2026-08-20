package com.graduate.thesis.service.captcha;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 旋转验证码: 多图案模板随机, 拖动转盘将图案旋正
 * 图案均为正立姿态明确(字母/人物/房子), 倒过来一眼可辨正位
 */
@Component
public class RotateGenerator implements CaptchaGenerator {

    private static final int SIZE = 200;
    private static final int TOLERANCE = 12;

    @Override
    public String type() {
        return "ROTATE";
    }

    @Override
    public CaptchaData generate() {
        Random r = ThreadLocalRandom.current();
        int targetAngle = r.nextInt(360);
        int tpl = r.nextInt(4); // 0字母F 1字母P 2人物 3房子

        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, SIZE, SIZE);
        g.setColor(new Color(225, 230, 240));
        g.fillOval(10, 10, SIZE - 20, SIZE - 20);

        AffineTransform t = AffineTransform.getRotateInstance(Math.toRadians(targetAngle), SIZE / 2.0, SIZE / 2.0);
        g.setTransform(t);
        switch (tpl) {
            case 0: drawLetterF(g); break;
            case 1: drawLetterP(g); break;
            case 2: drawPerson(g); break;
            default: drawHouse(g); break;
        }
        g.dispose();

        Map<String, Object> render = new HashMap<>();
        render.put("rotateImage", CaptchaGenerator.toBase64(img));
        render.put("width", SIZE);
        render.put("height", SIZE);
        return new CaptchaData("ROTATE", render, new RotateSecret(targetAngle));
    }

    /** 模板0: 字母 F (正读方向明确, 倒立一眼可辨) */
    private void drawLetterF(Graphics2D g) {
        g.setColor(new Color(64, 158, 255));
        g.setFont(new Font("Arial", Font.BOLD, 120));
        g.drawString("F", 60, 135);
        g.setColor(new Color(231, 76, 60));
        g.fillOval(125, 72, 14, 14);
        g.setColor(new Color(103, 194, 58));
        g.setStroke(new BasicStroke(4));
        g.drawLine(140, 140, 160, 160);
    }

    /** 模板1: 字母 P */
    private void drawLetterP(Graphics2D g) {
        g.setColor(new Color(64, 158, 255));
        g.setFont(new Font("Arial", Font.BOLD, 120));
        g.drawString("P", 58, 135);
        g.setColor(new Color(231, 76, 60));
        g.fillOval(128, 70, 14, 14);
        g.setColor(new Color(245, 158, 11));
        g.setStroke(new BasicStroke(4));
        g.drawLine(60, 150, 80, 165);
    }

    /** 模板2: 人物剪影(头在上, 脚在下, 正立姿态明确) */
    private void drawPerson(Graphics2D g) {
        g.setColor(new Color(64, 158, 255));
        // 头
        g.fillOval(82, 32, 36, 36);
        // 身体
        g.fillRoundRect(88, 70, 24, 50, 8, 8);
        // 手臂
        g.fillRoundRect(58, 78, 30, 12, 6, 6);
        g.fillRoundRect(112, 78, 30, 12, 6, 6);
        // 腿
        g.fillRoundRect(86, 120, 12, 40, 4, 4);
        g.fillRoundRect(102, 120, 12, 40, 4, 4);
        // 脸部点缀(让朝向更可辨)
        g.setColor(new Color(231, 76, 60));
        g.fillOval(94, 44, 12, 8);
    }

    /** 模板3: 房子(屋顶尖朝上, 倒过来一眼可辨) */
    private void drawHouse(Graphics2D g) {
        g.setColor(new Color(64, 158, 255));
        // 墙
        g.fillRect(68, 85, 64, 55);
        // 屋顶(三角, 尖朝上)
        Polygon roof = new Polygon();
        roof.addPoint(60, 85);
        roof.addPoint(140, 85);
        roof.addPoint(100, 45);
        g.fill(roof);
        // 门
        g.setColor(new Color(120, 80, 50));
        g.fillRoundRect(92, 105, 16, 35, 4, 4);
        // 窗
        g.setColor(new Color(103, 194, 58));
        g.fillRect(76, 95, 14, 14);
        g.fillRect(110, 95, 14, 14);
    }

    @Override
    public boolean verify(Object secret, JsonNode payload) {
        if (!(secret instanceof RotateSecret)) {
            return false;
        }
        RotateSecret s = (RotateSecret) secret;
        int endAngle = payload.path("endAngle").asInt();
        // 图片本身已旋转 targetAngle, 用户再旋 endAngle, 回正需 两者之和 ≡ 0 (mod 360)
        int diff = Math.floorMod(endAngle + s.targetAngle, 360);
        if (diff > 180) {
            diff = 360 - diff;
        }
        return diff <= TOLERANCE;
    }

    /** 旋转秘密: 初始旋转角度 */
    static class RotateSecret {
        final int targetAngle;
        RotateSecret(int targetAngle) {
            this.targetAngle = targetAngle;
        }
    }
}
