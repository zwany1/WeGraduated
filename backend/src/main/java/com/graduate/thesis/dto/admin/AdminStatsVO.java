package com.graduate.thesis.dto.admin;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 管理后台概览统计
 */
@Data
public class AdminStatsVO {

    private long userCount;

    private long adminCount;

    private long templateCount;

    private long taskCount;

    private long paperCount;

    /** 今日新增任务量 */
    private long todayTasks;

    /** 任务状态分布: PENDING / PROCESSING / SUCCESS / FAILED */
    private Map<String, Long> taskStatus;

    /** 近 7 天每日新增注册 */
    private List<TrendPoint> registerTrend;

    /** 近 7 天每日新增任务 */
    private List<TrendPoint> taskTrend;

    @Data
    public static class TrendPoint {
        private String date;
        private long count;

        public TrendPoint(String date, long count) {
            this.date = date;
            this.count = count;
        }
    }
}
