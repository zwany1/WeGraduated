package com.graduate.thesis.service.captcha;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

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
 * 图案均为正立姿态明确的矢量图形(字母/人物/房子/鱼/树), 倒过来一眼可辨正位, 不使用箭头
 */
@Component
public class RotateGenerator implements CaptchaGenerator {

    private static final int SIZE = 200;
    private static final int TOLERANCE = 12;
    private static final Color MAIN = new Color(64, 158, 255);
    private static final Color ACCENT = new Color(231, 76, 60);
    private static final Color WARM = new Color(245, 158, 11);
    private static final Color GREEN = new Color(103, 194, 58);
    private static final Color BROWN = new Color(120, 80, 50);

    @Override
    public String type() {
        return "ROTATE";
    }

    @Override
    public CaptchaData generate() {
        Random r = ThreadLocalRandom.current();
        int targetAngle = r.nextInt(360);
        int tpl = r.nextInt(6); // 0字母F 1字母R 2人物 3房子 4鱼 5树

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
            case 1: drawLetterR(g); break;
            case 2: drawPerson(g); break;
            case 3: drawHouse(g); break;
            case 4: drawFish(g); break;
            default: drawTree(g); break;
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
        g.setColor(MAIN);
        g.setFont(new Font("Arial", Font.BOLD, 130));
        g.drawString("F", 62, 142);
    }

    /** 模板1: 字母 R */
    private void drawLetterR(Graphics2D g) {
        g.setColor(MAIN);
        g.setFont(new Font("Arial", Font.BOLD, 130));
        g.drawString("R", 58, 142);
    }

    /** 模板2: 人物剪影(头在上, 脚在下, 正立姿态明确) */
    private void drawPerson(Graphics2D g) {
        g.setColor(MAIN);
        g.fillOval(82, 30, 36, 36);            // 头
        g.fillRoundRect(88, 68, 24, 52, 8, 8); // 身体
        g.fillRoundRect(58, 76, 30, 12, 6, 6); // 左臂
        g.fillRoundRect(112, 76, 30, 12, 6, 6);// 右臂
        g.fillRoundRect(86, 120, 12, 42, 4, 4);// 左腿
        g.fillRoundRect(102, 120, 12, 42, 4, 4);// 右腿
        g.setColor(ACCENT);
        g.fillOval(94, 42, 12, 8);             // 脸部朝向点缀
    }

    /** 模板3: 房子(屋顶尖朝上, 倒过来一眼可辨) */
    private void drawHouse(Graphics2D g) {
        g.setColor(MAIN);
        g.fillRect(68, 85, 64, 55);            // 墙
        Polygon roof = new Polygon();          // 屋顶(三角, 尖朝上)
        roof.addPoint(60, 85);
        roof.addPoint(140, 85);
        roof.addPoint(100, 45);
        g.fill(roof);
        g.setColor(BROWN);
        g.fillRoundRect(92, 105, 16, 35, 4, 4);// 门
        g.setColor(GREEN);
        g.fillRect(76, 95, 14, 14);            // 左窗
        g.fillRect(110, 95, 14, 14);           // 右窗
    }

    /** 模板4: 鱼(头朝左, 尾朝右, 正立姿态明确) */
    private void drawFish(Graphics2D g) {
        g.setColor(MAIN);
        g.fillOval(55, 85, 90, 40);            // 鱼身(横向椭圆, 左端为头)
        Polygon tail = new Polygon();          // 尾(右侧三角)
        tail.addPoint(140, 75);
        tail.addPoint(140, 135);
        tail.addPoint(172, 105);
        g.fill(tail);
        g.setColor(WARM);
        Polygon fin = new Polygon();           // 背鳍(上方)
        fin.addPoint(90, 85);
        fin.addPoint(110, 85);
        fin.addPoint(100, 70);
        g.fill(fin);
        g.setColor(ACCENT);
        g.fillOval(70, 96, 10, 10);            // 眼(靠左, 示头部朝向)
    }

    /** 模板5: 树(树冠在上, 树干在下, 正立姿态明确) */
    private void drawTree(Graphics2D g) {
        g.setColor(BROWN);
        g.fillRect(92, 118, 16, 48);           // 树干(下)
        g.setColor(GREEN);
        g.fillOval(68, 36, 64, 64);            // 树冠(上)
        g.fillOval(82, 56, 44, 44);
        g.setColor(ACCENT);
        g.fillOval(116, 70, 12, 12);           // 果实(点缀朝向)
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
