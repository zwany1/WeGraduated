package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * ER 图数据结构(前端渲染用): 节点 + 边
 */
@Data
public class ERGraphVO {
    private List<ERNode> nodes = new ArrayList<>();
    private List<EREdge> edges = new ArrayList<>();
    private int width;
    private int height;
}
