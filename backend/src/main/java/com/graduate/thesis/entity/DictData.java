package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典数据
 */
@Data
@TableName("t_dict_data")
public class DictData {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String dictType;

    private String dictLabel;

    private String dictValue;

    private Integer dictSort;

    private Boolean status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
