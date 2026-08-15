package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统参数
 */
@Data
@TableName("t_config")
public class Config {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configName;

    private String configKey;

    private String configValue;

    /** 1内置 0自定义 */
    private Boolean configType;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
