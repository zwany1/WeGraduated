package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.DiagramEdge;
import com.graduate.thesis.dto.DiagramLane;
import com.graduate.thesis.dto.DiagramNode;
import com.graduate.thesis.dto.DiagramVO;
import com.graduate.thesis.dto.SequenceConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 时序图规则引擎(UML Sequence Diagram): Participant + Message + Activation
 *
 * 布局规则:
 *   - 参与者顶部横向均分: x = index * laneWidth + offset
 *   - 每参与者一根生命线(竖线)
 *   - 消息按顺序纵向排列(request 实线 / return 虚线)
 *   - 激活条: 参与者生命线两侧的绿色竖条(按消息覆盖区间)
 */
@Service
public class SequenceRuleEngine {

    private final java.util.concurrent.atomic.AtomicInteger seq = new java.util.concurrent.atomic.AtomicInteger();

    public DiagramVO build(SequenceConfig config) {
        if (config == null) {
            throw new BusinessException(400, "请配置时序图");
        }
        List<SequenceConfig.ParticipantConfig> participants = validParticipants(config.getParticipants());
        List<SequenceConfig.MessageConfig> messages = validMessages(config.getMessages(), participants);
        if (participants.isEmpty()) {
            throw new BusinessException(400, "请至少配置一个参与者");
        }
        if (messages.isEmpty()) {
            throw new BusinessException(400, "请至少配置一条消息");
        }
        DiagramVO vo = new DiagramVO();
        vo.setType("SEQUENCE");
        vo.setName(config.getTitle() == null || config.getTitle().trim().isEmpty()
                ? "时序图" : config.getTitle().trim());

        // 布局: 盒子宽度按名字自适应, 生命线贴盒底, 消息紧凑排列, 画布高度按内容收缩
        int topY = 40;
        int boxH = 44;
        int laneGap = 56;
        int msgGap = 64;

        int n = participants.size();
        double[] centers = new double[n];
        double[] boxWs = new double[n];
        double cursor = 80;
        for (int i = 0; i < n; i++) {
            String name = participants.get(i).getName();
            double w = Math.min(220, Math.max(96, textWidth(name)));
            boxWs[i] = w;
            centers[i] = cursor + w / 2;
            cursor += w + laneGap;

            SequenceConfig.ParticipantConfig p = participants.get(i);
            DiagramNode node = node(p.getId(), p.getName(), "participant");
            node.setX(centers[i] - w / 2);
            node.setY(topY);
            node.setWidth(w);
            node.setHeight(boxH);
            vo.getNodes().add(node);

            // 生命线(作为 lane, x = 盒子中心, 前端画竖线)
            DiagramLane lane = new DiagramLane();
            lane.setId("line" + (seq.getAndIncrement()));
            lane.setName(p.getName());
            lane.setX(centers[i]);
            lane.setY(topY + boxH);
            lane.setWidth(0);
            lane.setHeight(0); // 底部在消息排完后回填
            vo.getLanes().add(lane);
        }

        // 消息: 纵向排列, 端点落在激活条边缘而不是生命线圆心
        int[] firstInv = new int[n];
        int[] lastInv = new int[n];
        java.util.Arrays.fill(firstInv, -1);
        java.util.Arrays.fill(lastInv, -1);
        double lastMsgY = 0;
        for (int idx = 0; idx < messages.size(); idx++) {
            SequenceConfig.MessageConfig m = messages.get(idx);
            int fi = indexOf(participants, m.getFrom());
            int ti = indexOf(participants, m.getTo());
            if (fi < 0 || ti < 0) continue;
            if (firstInv[fi] < 0) firstInv[fi] = idx;
            lastInv[fi] = idx;
            if (firstInv[ti] < 0) firstInv[ti] = idx;
            lastInv[ti] = idx;

            double y = msgY0() + idx * msgGap;
            lastMsgY = y;
            double dx = centers[ti] - centers[fi];
            double x1 = dx == 0 ? centers[fi] : centers[fi] + Math.signum(dx) * 5;
            double x2 = dx == 0 ? centers[ti] : centers[ti] - Math.signum(dx) * 5;

            DiagramEdge edge = new DiagramEdge();
            edge.setId("m" + (seq.getAndIncrement()));
            edge.setSource(m.getFrom());
            edge.setTarget(m.getTo());
            edge.setLabel(m.getText());
            edge.setStyle("return".equalsIgnoreCase(m.getType()) ? "return" : "request");
            edge.setSourceX(x1);
            edge.setSourceY(y);
            edge.setTargetX(x2);
            edge.setTargetY(y);
            vo.getEdges().add(edge);
        }
        if (messages.isEmpty()) {
            lastMsgY = msgY0();
        }

        // 激活条: 只给真正参与过消息的参与者画, 区间 = 首末消息各外扩 16px
        for (int i = 0; i < n; i++) {
            if (firstInv[i] < 0) continue;
            double actTop = msgY0() + firstInv[i] * msgGap - 16;
            double actBottom = msgY0() + lastInv[i] * msgGap + 16;
            DiagramNode act = node("act_" + participants.get(i).getId(), "", "activation");
            act.setX(centers[i] - 5);
            act.setY(actTop);
            act.setWidth(10);
            act.setHeight(actBottom - actTop);
            vo.getNodes().add(act);
        }

        double lifelineBottom = lastMsgY + 64;
        for (DiagramLane lane : vo.getLanes()) {
            lane.setHeight(lifelineBottom - lane.getY());
        }

        vo.setWidth((int) Math.round(cursor - laneGap + 80));
        vo.setHeight((int) Math.round(lifelineBottom + 40));
        return vo;
    }

    /** 消息起始 y: 生命线顶部往下留一段呼吸空间 */
    private double msgY0() {
        return 40 + 44 + 52;
    }

    /** 粗略估算名字渲染宽度: CJK 15px/字, 其余 9px/字, 加左右内边距 */
    private double textWidth(String name) {
        int w = 32;
        for (int i = 0; i < name.length(); i++) {
            w += name.charAt(i) > 0x2E7F ? 15 : 9;
        }
        return w;
    }

    private int indexOf(List<SequenceConfig.ParticipantConfig> participants, String id) {
        for (int i = 0; i < participants.size(); i++) {
            if (participants.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    private List<SequenceConfig.ParticipantConfig> validParticipants(List<SequenceConfig.ParticipantConfig> participants) {
        List<SequenceConfig.ParticipantConfig> out = new ArrayList<>();
        if (participants == null) return out;
        int i = 0;
        for (SequenceConfig.ParticipantConfig p : participants) {
            if (p.getName() == null || p.getName().trim().isEmpty()) continue;
            if (p.getId() == null || p.getId().trim().isEmpty()) p.setId("P" + (i++));
            p.setName(p.getName().trim());
            out.add(p);
        }
        return out;
    }

    private List<SequenceConfig.MessageConfig> validMessages(
            List<SequenceConfig.MessageConfig> messages, List<SequenceConfig.ParticipantConfig> participants) {
        List<SequenceConfig.MessageConfig> out = new ArrayList<>();
        if (messages == null) return out;
        Map<String, Boolean> ids = new HashMap<>();
        for (SequenceConfig.ParticipantConfig p : participants) ids.put(p.getId(), true);
        int i = 0;
        for (SequenceConfig.MessageConfig m : messages) {
            if (m.getText() == null || m.getText().trim().isEmpty()) continue;
            if (m.getFrom() != null && m.getTo() != null
                    && ids.containsKey(m.getFrom()) && ids.containsKey(m.getTo())) {
                if (m.getId() == null || m.getId().trim().isEmpty()) m.setId("M" + (i++));
                m.setText(m.getText().trim());
                if (m.getType() == null || m.getType().trim().isEmpty()) m.setType("request");
                out.add(m);
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
