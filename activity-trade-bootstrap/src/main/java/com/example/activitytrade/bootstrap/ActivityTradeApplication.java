package com.example.activitytrade.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 营销活动交易系统（秒杀） 启动入口。
 * 扫描根包 com.example.activitytrade，覆盖全部业务模块（多模块单体组件扫描依赖根包）。
 */
@SpringBootApplication(scanBasePackages = "com.example.activitytrade")
@MapperScan("com.example.activitytrade")
@EnableScheduling
public class ActivityTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityTradeApplication.class, args);
    }
}
