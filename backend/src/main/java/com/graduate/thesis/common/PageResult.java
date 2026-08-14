package com.graduate.thesis.common;

import lombok.Data;

import java.util.List;

/**
 * 通用分页结果
 */
@Data
public class PageResult<T> {

    private long total;

    private List<T> records;

    public static <T> PageResult<T> of(long total, List<T> records) {
        PageResult<T> r = new PageResult<>();
        r.total = total;
        r.records = records;
        return r;
    }
}
