package com.example.activitytrade.settle.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对账报告 VO。
 */
public class SettleReportVO {

    private final LocalDateTime lastRunAt;
    private final boolean ok;
    private final List<String> issues;
    private final int rolledLogs;
    private final int resentDead;

    public SettleReportVO(LocalDateTime lastRunAt, boolean ok, List<String> issues,
                          int rolledLogs, int resentDead) {
        this.lastRunAt = lastRunAt;
        this.ok = ok;
        this.issues = issues;
        this.rolledLogs = rolledLogs;
        this.resentDead = resentDead;
    }

    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public boolean isOk() { return ok; }
    public List<String> getIssues() { return issues; }
    public int getRolledLogs() { return rolledLogs; }
    public int getResentDead() { return resentDead; }
}