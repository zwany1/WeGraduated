package com.graduate.thesis.dto;

import lombok.Data;

/**
 * 封面配置保存参数(允许为空以删除封面)
 */
@Data
public class CoverConfigSaveDTO {

    private String pageConfig;
}
