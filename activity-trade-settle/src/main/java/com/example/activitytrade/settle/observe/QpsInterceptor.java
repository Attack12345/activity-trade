package com.example.activitytrade.settle.observe;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 观测拦截器：为 /api/** 计数 QPS。
 */
@Component
public class QpsInterceptor implements HandlerInterceptor {

    private final QpsCounter qpsCounter;

    public QpsInterceptor(QpsCounter qpsCounter) {
        this.qpsCounter = qpsCounter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        qpsCounter.hit();
        return true;
    }
}