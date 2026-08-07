package com.graduate.thesis.dto;

import lombok.Data;

/**
 * 泳道容器(泳道图用): 横向泳道, 内部容纳动作节点
 */
@Data
public class DiagramLane {
    private String id;
    private String name;
    private double x;
    private double y;
    private double width;
    private double height;
}
