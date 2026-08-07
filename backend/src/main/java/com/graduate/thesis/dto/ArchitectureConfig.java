package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统架构图配置: 动态分层结构
 *
 * 用户可自定义层名与每层的组件列表
 */
@Data
public class ArchitectureConfig {

    private String systemName;
    private List<LayerConfig> layers = new ArrayList<>();

    @Data
    public static class LayerConfig {
        private String name;
        private List<ComponentConfig> components = new ArrayList<>();
    }

    @Data
    public static class ComponentConfig {
        private String name;
        /** 类型: client/web/gateway/service/database/cache */
        private String type;
    }
}
