package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 泳道活动图配置(UML Activity Diagram + Swimlane 模型): 泳道 + 节点 + 连线
 *
 * 与架构图/泳道图/用例图/时序图/类图数据模型独立。
 */
@Data
public class ActivityConfig {

    /** 图标题 */
    private String title;

    /** 泳道(横向排列为列) */
    private List<LaneConfig> lanes = new ArrayList<>();

    /** 活动节点 */
    private List<NodeConfig> nodes = new ArrayList<>();

    /** 连线(带条件标签) */
    private List<EdgeConfig> edges = new ArrayList<>();

    @Data
    public static class LaneConfig {
        private String id;
        private String name;
    }

    @Data
    public static class NodeConfig {
        private String id;
        private String text;
        /** START / ACTION / DECISION / END */
        private String type;
        private String laneId;
    }

    @Data
    public static class EdgeConfig {
        private String source;
        private String target;
        /** 分支条件 */
        private String label;
    }
}
