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
    /** 连线样式: association(实线) / include(虚线箭头) */
    private String style;
}
