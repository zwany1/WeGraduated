package com.graduate.thesis.dto;

import lombok.Data;

/**
 * 系统设计图节点: 流程图/泳道图/架构图通用
 */
@Data
public class DiagramNode {
    private String id;
    /** 形状: start / end / process / decision / service / storage / database / actor / usecase / system */
    private String shape;
    private String label;
    private double x;
    private double y;
    private double width;
    private double height;
    /** 泳道名(泳道图用) */
    private String lane;
}
