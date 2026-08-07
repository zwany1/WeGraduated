package com.graduate.thesis.dto;

import lombok.Data;

/**
 * ER 图节点: 实体(rect)/关系(rhombus)/属性(ellipse)
 */
@Data
public class ERNode {
    private String id;
    private String label;
    /** rect / rhombus / ellipse */
    private String shape;
    /** 主键下划线(仅属性) */
    private boolean key;
    private double x;
    private double y;
    private double width;
    private double height;
}
