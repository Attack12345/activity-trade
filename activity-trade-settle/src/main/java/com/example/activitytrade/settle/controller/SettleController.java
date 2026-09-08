package com.example.activitytrade.settle.controller;

import com.example.activitytrade.common.api.Result;
import com.example.activitytrade.settle.job.SettleReconcileJob;
import com.example.activitytrade.settle.service.SettleReportVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对账接口（API 契约 #18 report / #19 admin run）。
 */
@RestController
@RequestMapping("/api/settle")
public class SettleController {

    private final SettleReconcileJob reconcileJob;

    public SettleController(SettleReconcileJob reconcileJob) {
        this.reconcileJob = reconcileJob;
    }

    @GetMapping("/report")
    public Result<SettleReportVO> report() {
        SettleReportVO report = reconcileJob.currentReport();
        if (report == null) {
            report = new SettleReportVO(null, false, List.of("对账尚未执行"), 0, 0);
        }
        return Result.ok(report);
    }

    @PostMapping("/admin/run")
    public Result<SettleReportVO> run() {
        return Result.ok(reconcileJob.reconcileNow());
    }
}