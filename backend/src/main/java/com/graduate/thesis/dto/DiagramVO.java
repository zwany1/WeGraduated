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
    /** FLOW / SWIMLANE / ARCH / FREE */
    private String type;
    private String description;
    /** drawio 原生 XML(仅 FREE 类型使用) */
    private String content;
    private List<DiagramNode> nodes = new ArrayList<>();
    private List<DiagramEdge> edges = new ArrayList<>();
    /** 泳道容器(仅 SWIMLANE 使用) */
    private List<DiagramLane> lanes = new ArrayList<>();
    /**
     * 文本 DSL 原文。保存时随图一起落盘，用于再次打开时回填输入框。
     * 改造前保存的图没有这个字段（反序列化后为 null），由 render 反向生成。
     */
    private String dsl;
    /**
     * 解析后的结构化配置（如 SequenceConfig）。DSL 生成成功时随结果返回，
     * 前端用它回填「精确调整」表单，保证文本与表单一致（R4）。
     */
    private Object config;
    private int width;
    private int height;
}
