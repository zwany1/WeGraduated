package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程图配置: 节点(带类型) + 连线(可带条件标签)。
 *
 * <p>节点类型: start(开始) / action(任务) / condition(判断) / end(结束)。
 * 与泳道活动图的区别是不分层(没有 lane), 纯纵向流程。</p>
 */
@Data
public class FlowConfig {

    private String title;

    private List<Node> nodes = new ArrayList<>();

    private List<Edge> edges = new ArrayList<>();

    @Data
    public static class Node {
        private String id;
        private String name;
        /** start / action / condition / end */
        private String type;
    }

    @Data
    public static class Edge {
        private String from;
        private String to;
        /** 分支条件, 如「是」「否」 */
        private String label;
    }
}
