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

    private int seq = 0;

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
        seq = 0;

        DiagramVO vo = new DiagramVO();
        vo.setType("SEQUENCE");
        vo.setName(config.getTitle() == null || config.getTitle().trim().isEmpty()
                ? "时序图" : config.getTitle().trim());

        int laneWidth = 150;
        int x0 = 100;
        int topY = 40;
        int lifelineTop = 140;
        int lifelineBottom = 720;
        int msgY0 = 220;
        int msgGap = 60;

        // 参与者 id -> x
        Map<String, Double> xMap = new HashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            SequenceConfig.ParticipantConfig p = participants.get(i);
            double x = x0 + i * laneWidth;
            xMap.put(p.getId(), x);

            DiagramNode node = node(p.getId(), p.getName(), "participant");
            node.setX(x);
            node.setY(topY);
            node.setWidth(120);
            node.setHeight(44);
            vo.getNodes().add(node);

            // 生命线(作为 lane, 前端画竖线)
            DiagramLane lane = new DiagramLane();
            lane.setId("line" + (seq++));
            lane.setName(p.getName());
            lane.setX(x);
            lane.setY(lifelineTop);
            lane.setWidth(0);
            lane.setHeight(lifelineBottom - lifelineTop);
            vo.getLanes().add(lane);
        }

        // 消息: 每条记录 y 坐标, 返回激活区间
        List<double[]> msgYs = new ArrayList<>();
        int idx = 0;
        for (SequenceConfig.MessageConfig m : messages) {
            double x1 = xMap.get(m.getFrom());
            double x2 = xMap.get(m.getTo());
            double y = msgY0 + idx * msgGap;

            DiagramEdge edge = new DiagramEdge();
            edge.setId("m" + (seq++));
            edge.setSource(m.getFrom());
            edge.setTarget(m.getTo());
            edge.setLabel(m.getText());
            edge.setStyle("return".equalsIgnoreCase(m.getType()) ? "return" : "request");
            edge.setSourceX(x1);
            edge.setSourceY(y);
            edge.setTargetX(x2);
            edge.setTargetY(y);
            vo.getEdges().add(edge);
            msgYs.add(new double[]{x1, x2, y});
            idx++;
        }

        // 激活条: 参与者在消息中的覆盖区间(简化: 首条消息上沿到末条消息下沿)
        double msgTop = msgY0 - 15;
        double msgBottom = msgY0 + (messages.size() - 1) * msgGap + 15;
        for (SequenceConfig.ParticipantConfig p : participants) {
            double x = xMap.get(p.getId());
            DiagramNode act = node("act_" + p.getId(), "", "activation");
            act.setX(x - 12);
            act.setY(msgTop);
            act.setWidth(12);
            act.setHeight(msgBottom - msgTop);
            vo.getNodes().add(act);
        }

        vo.setWidth(x0 + participants.size() * laneWidth + 80);
        vo.setHeight(lifelineBottom + 40);
        return vo;
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
