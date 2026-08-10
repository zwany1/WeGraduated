package com.graduate.thesis.dto;

import lombok.Data;

/**
 * 系统设计图边
 */
@Data
public class DiagramEdge {
    private String id;
    private String source;
    private String target;
    private String label;
    /** 连线样式: association(实线) / include(虚线箭头) / request(实线) / return(虚线) */
    private String style;
    /** 时序图: 消息起止坐标(直接用坐标而非节点锚点) */
    private Double sourceX;
    private Double sourceY;
    private Double targetX;
    private Double targetY;
}
