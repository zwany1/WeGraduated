package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 泳道图配置(BPMN 模型): 流程 = 泳道(Lane) + 节点(Node) + 连线(Edge)
 *
 * 泳道横向排列(列), 流程纵向流动; 节点类型: start / task / gateway / end
 * 与架构图(Layer+Component)数据模型完全独立。
 */
@Data
public class SwimlaneConfig {

    /** 业务名称 */
    private String flowName;

    /** 泳道(参与者/系统), 横向排列为列: 用户 / Vue客户端 / SpringBoot服务 / MySQL数据库 */
    private List<LaneConfig> lanes = new ArrayList<>();

    /** 节点: 属于某个泳道, 带类型与流程顺序(edges 决定) */
    private List<NodeConfig> nodes = new ArrayList<>();

    /** 连线: source/target 为节点 id */
    private List<EdgeConfig> edges = new ArrayList<>();

    @Data
    public static class LaneConfig {
        private String id;
        private String name;
    }

    @Data
    public static class NodeConfig {
        private String id;
        private String laneId;
        private String name;
        /** 节点类型: start(开始/圆角) / task(任务/矩形) / gateway(判断/菱形) / end(结束/圆角) */
        private String type;
    }

    @Data
    public static class EdgeConfig {
        private String source;
        private String target;
        private String label;
    }
}
