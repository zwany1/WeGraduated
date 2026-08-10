package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 时序图配置(UML Sequence Diagram 模型): 参与者 + 消息 + 激活条
 *
 * 与架构图/泳道图/用例图数据模型独立。
 */
@Data
public class SequenceConfig {

    /** 图标题 */
    private String title;

    /** 参与者 Lifeline */
    private List<ParticipantConfig> participants = new ArrayList<>();

    /** 消息 Message(按顺序纵向排列) */
    private List<MessageConfig> messages = new ArrayList<>();

    @Data
    public static class ParticipantConfig {
        private String id;
        private String name;
    }

    @Data
    public static class MessageConfig {
        private String id;
        private String from;
        private String to;
        private String text;
        /** request(实线) / return(虚线) */
        private String type;
    }
}
