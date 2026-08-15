package com.graduate.thesis.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情: 用户信息 + 其模板/任务/论文文件
 */
@Data
public class UserDetailVO {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String role;
    private Boolean status;
    private List<String> roleNames;
    private LocalDateTime createTime;

    /** 模板数 */
    private long templateCount;
    /** 任务数 */
    private long taskCount;
    /** 文件数 */
    private long paperCount;

    private List<Item> templates;
    private List<Item> tasks;
    private List<Item> papers;

    @Data
    public static class Item {
        private Long id;
        private String name;
        private String status;
        private String extra;
        private LocalDateTime time;
    }
}
