package com.graduate.thesis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * ER 图生成请求(Chen 记法: 实体矩形/属性椭圆/关系菱形)
 */
@Data
public class ErDTO {

    private Integer fontSize = 12;
    private List<ErEntity> entities = new ArrayList<>();
    private List<ErRelation> relations = new ArrayList<>();

    @Data
    public static class ErEntity {
        private String name;
        private List<ErAttr> attrs = new ArrayList<>();
    }

    @Data
    public static class ErAttr {
        private String name;
        private Boolean key = false;
    }

    @Data
    public static class ErRelation {
        private String from;
        private String to;
        private String label;
        /** 基数标注, 如 1:N / N:M (两端数字用:分隔, 靠近 from 的放前面) */
        private String cardinality;
        /** 关系属性(如 购买数量/购买时间) */
        private List<ErAttr> attrs = new ArrayList<>();
    }
}
