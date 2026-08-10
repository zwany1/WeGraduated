package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用例图配置(UML Use Case Diagram 模型): 参与者 + 用例 + 关系
 *
 * 与架构图/泳道图数据模型独立。
 */
@Data
public class UseCaseConfig {

    /** 系统名称(系统边界框标题) */
    private String system;

    /** 参与者 Actor */
    private List<ActorConfig> actors = new ArrayList<>();

    /** 用例 UseCase */
    private List<UseCaseConfigItem> usecases = new ArrayList<>();

    /** 关系: association(关联) / include(包含) / extend(扩展) */
    private List<RelationConfig> relations = new ArrayList<>();

    @Data
    public static class ActorConfig {
        private String id;
        private String name;
    }

    @Data
    public static class UseCaseConfigItem {
        private String id;
        private String name;
        /** 所属模块(可选, 用于用例分组排列) */
        private String module;
    }

    @Data
    public static class RelationConfig {
        private String source;
        private String target;
        /** association / include / extend */
        private String type;
    }
}
