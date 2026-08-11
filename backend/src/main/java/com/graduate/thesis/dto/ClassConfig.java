package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类图配置(UML Class Diagram 模型): 类 + 属性 + 方法 + 关系
 *
 * 与架构图/泳道图/用例图/时序图数据模型独立。
 */
@Data
public class ClassConfig {

    /** 图标题 */
    private String title;

    /** 类 */
    private List<ClassItem> classes = new ArrayList<>();

    /** 类间关系 */
    private List<ClassRelation> relations = new ArrayList<>();

    @Data
    public static class ClassItem {
        private String id;
        private String name;
        /** 属性 */
        private List<Attribute> attributes = new ArrayList<>();
        /** 方法 */
        private List<Method> methods = new ArrayList<>();
    }

    @Data
    public static class Attribute {
        private String name;
        private String type;
        /** + public / - private / # protected */
        private String visibility;
    }

    @Data
    public static class Method {
        private String name;
        private String returnType;
        private String visibility;
    }

    @Data
    public static class ClassRelation {
        private String source;
        private String target;
        /** association / inheritance / composition */
        private String type;
        /** 基数: 1 / n / * */
        private String left;
        private String right;
    }
}
