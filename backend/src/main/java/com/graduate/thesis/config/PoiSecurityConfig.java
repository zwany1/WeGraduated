package com.graduate.thesis.config;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * POI 全局安全参数一次性初始化:
 * 放宽 ZIP 压缩比限制 —— 论文 docx 可能内嵌字体(odttf), 压缩率极高被 POI 误判为 zip 炸弹.
 * ZipSecureFile 的压缩比是 JVM 全局静态值, 在启动时设置一次即可,
 * 避免每次排版/对比时重复调用(全局静态写入, 并发无额外开销).
 */
@Component
public class PoiSecurityConfig {

    @PostConstruct
    public void init() {
        ZipSecureFile.setMinInflateRatio(0.001);
    }
}