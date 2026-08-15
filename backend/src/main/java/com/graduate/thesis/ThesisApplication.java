package com.graduate.thesis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 毕业论文自动排版系统
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ThesisApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThesisApplication.class, args);
    }
}
