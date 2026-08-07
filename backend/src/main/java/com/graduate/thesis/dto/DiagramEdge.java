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
}
