package com.example.activitytrade.settle.job;

import com.example.activitytrade.settle.entity.TradeLog;
import com.example.activitytrade.settle.mapper.TradeLogMapper;
import com.example.activitytrade.settle.observe.QpsCounter;
import com.example.activitytrade.settle.service.SettleReportVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 对账任务（M9，§8.4）：
 * ① 活动已结束仍占用的扣减日志 → 回滚
 * ② trade_log 待补偿 → 按原 payload 重发（retry+1，>5 日志告警）
 * ③ 汇总校验：库存非负 且 占用数 == 进行中订单数（否则记 issue）
 * ④ QPS 采样健康确认
 */
@Component
public class SettleReconcileJob {

    private static final Logger log = LoggerFactory.getLogger(SettleReconcileJob.class);
    private static final int MAX_RETRY = 5;

    private final JdbcTemplate jdbcTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final TradeLogMapper tradeLogMapper;
    private final QpsCounter qpsCounter;

    private volatile SettleReportVO lastReport;

    public SettleReconcileJob(JdbcTemplate jdbcTemplate, RocketMQTemplate rocketMQTemplate,
                              TradeLogMapper tradeLogMapper, QpsCounter qpsCounter) {
        this.jdbcTemplate = jdbcTemplate;
        this.rocketMQTemplate = rocketMQTemplate;
        this.tradeLogMapper = tradeLogMapper;
        this.qpsCounter = qpsCounter;
    }

    @Scheduled(cron = "${app.settle.cron}")
    public void reconcile() {
        runReconcile();
    }

    /** 手动触发（/api/admin/settle/run） */
    public SettleReportVO reconcileNow() {
        return runReconcile();
    }

    public SettleReportVO currentReport() {
        return lastReport;
    }

    private SettleReportVO runReconcile() {
        List<String> issues = new ArrayList<>();

        // ① 活动已结束仍占用 → 置回滚
        int rolled = jdbcTemplate.update(
                "UPDATE stock_deduct_log d JOIN activity a ON a.id = d.activity_id " +
                        "SET d.status = 2, d.updated_at = NOW() " +
                        "WHERE d.status IN (0,1) AND a.end_time < NOW()");
        if (rolled > 0) {
            issues.add("活动已结束后回滚占用日志 " + rolled + " 条");
            log.warn("reconcile: rolled {} occupied log(s) after activity ended", rolled);
        }

        // ② 死信待补偿 → 重发
        int resent = resendDeadLetters(issues);

        // ③ 汇总校验
        int mismatch = verifyInvariants(issues);

        // ④ 指标采样确认
        int qpsSamples = qpsCounter.recent(1).size();

        boolean ok = rolled == 0 && mismatch == 0;
        lastReport = new SettleReportVO(LocalDateTime.now(), ok, issues, rolled, resent);
        log.info("reconcile done: ok={}, rolled={}, resent={}, mismatch={}, qpsSamples={}",
                ok, rolled, resent, mismatch, qpsSamples);
        return lastReport;
    }

    private int resendDeadLetters(List<String> issues) {
        List<TradeLog> deads = tradeLogMapper.selectList(
                new LambdaQueryWrapper<TradeLog>()
                        .eq(TradeLog::getStatus, 1)
                        .lt(TradeLog::getRetry, MAX_RETRY));
        int resent = 0;
        for (TradeLog dead : deads) {
            boolean sent = false;
            try {
                if (StringUtils.hasText(dead.getPayload())) {
                    rocketMQTemplate.syncSend(dead.getTopic(), MessageBuilder.withPayload(dead.getPayload()).build(), 3000L);
                    resent++;
                    sent = true;
                }
            } catch (Exception e) {
                log.error("resend dead letter failed: bizNo={}, topic={}", dead.getBizNo(), dead.getTopic(), e);
            }
            tradeLogMapper.update(null,
                    new LambdaUpdateWrapper<TradeLog>()
                            .eq(TradeLog::getBizNo, dead.getBizNo())
                            .set(TradeLog::getRetry, dead.getRetry() + 1)
                            .set(TradeLog::getStatus, sent ? 2 : 1));
            if (!sent && dead.getRetry() + 1 >= MAX_RETRY) {
                log.error("reconcile ALERT: dead letter retried too many times, bizNo={}", dead.getBizNo());
                issues.add("死信重试超限: " + dead.getBizNo());
            }
        }
        return resent;
    }

    private int verifyInvariants(List<String> issues) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT s.activity_id, s.sku_id, s.seckill_stock AS db_stock, " +
                        "(SELECT COUNT(*) FROM orders o WHERE o.activity_id = s.activity_id " +
                        "  AND o.sku_id = s.sku_id AND o.status IN (0,1)) AS active_orders, " +
                        "(SELECT COUNT(*) FROM stock_deduct_log d WHERE d.activity_id = s.activity_id " +
                        "  AND d.sku_id = s.sku_id AND d.status IN (0,1)) AS occupied " +
                        "FROM activity_sku s");
        int mismatch = 0;
        for (Map<String, Object> row : rows) {
            long dbStock = ((Number) row.get("db_stock")).longValue();
            long activeOrders = ((Number) row.get("active_orders")).longValue();
            long occupied = ((Number) row.get("occupied")).longValue();
            if (dbStock < 0 || activeOrders != occupied) {
                mismatch++;
                String msg = "sku(act=" + row.get("activity_id") + ",sku=" + row.get("sku_id")
                        + ") db_stock=" + dbStock + " active_orders=" + activeOrders + " occupied=" + occupied;
                issues.add(msg);
                log.warn("reconcile mismatch: {}", msg);
            }
        }
        return mismatch;
    }
}