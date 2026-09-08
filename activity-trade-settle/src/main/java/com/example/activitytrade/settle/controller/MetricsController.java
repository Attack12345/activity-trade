package com.example.activitytrade.settle.controller;

import com.example.activitytrade.common.api.Result;
import com.example.activitytrade.settle.observe.QpsCounter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 指标接口（API 契约 #20）：最近 N 分钟 QPS 采样序列。
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final QpsCounter qpsCounter;

    public MetricsController(QpsCounter qpsCounter) {
        this.qpsCounter = qpsCounter;
    }

    @GetMapping("/qps")
    public Result<List<QpsCounter.Sample>> qps(@RequestParam(defaultValue = "5") int minutes) {
        return Result.ok(qpsCounter.recent(Math.min(Math.max(minutes, 1), 120)));
    }
}