package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统设计图数据结构
 */
@Data
public class DiagramVO {
    private Long id;
    private String name;
    /** FLOW / SWIMLANE / ARCH */
    private String type;
    private String description;
    private List<DiagramNode> nodes = new ArrayList<>();
    private List<DiagramEdge> edges = new ArrayList<>();
    private int width;
    private int height;
}
