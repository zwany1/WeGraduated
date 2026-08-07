package com.graduate.thesis.engine.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

/**
 * 封面信息配置
 */
@Getter
@Setter
public class CoverConfig {

    /** 是否生成封面(默认 false, 需模板显式开启) */
    private Boolean enabled;

    private String title;
    private String college;
    private String major;
    private String studentName;
    private String studentNo;
    private String teacherUnit;
    private String teacher;
    private String teacherTitle;
    private String topicType;
    private String date;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled) && title != null && !title.trim().isEmpty();
    }

    public static CoverConfig parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(json, CoverConfig.class);
        } catch (Exception e) {
            return null;
        }
    }
}
