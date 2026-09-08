package com.example.activitytrade.settle.config;

import com.example.activitytrade.settle.observe.QpsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 观测拦截注册：/api/** 计数（与认证拦截相互独立）。
 */
@Configuration
public class SettleWebConfig implements WebMvcConfigurer {

    private final QpsInterceptor qpsInterceptor;

    public SettleWebConfig(QpsInterceptor qpsInterceptor) {
        this.qpsInterceptor = qpsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(qpsInterceptor).addPathPatterns("/api/**");
    }
}