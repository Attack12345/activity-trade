package com.example.activitytrade.order.job;

import com.example.activitytrade.order.entity.Order;
import com.example.activitytrade.order.mapper.OrderMapper;
import com.example.activitytrade.order.service.OrderCloseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 关单兜底任务（M9，§8.4）：每分钟扫描「待支付且超过 close-minutes」的订单 → 关单回补。
 * 延迟消息链路失效时的最终一致性保底。
 */
@Component
public class OrderCloseJob {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseJob.class);

    private final OrderMapper orderMapper;
    private final OrderCloseService orderCloseService;

    @Value("${app.settle.close-minutes:30}")
    private int closeMinutes;

    public OrderCloseJob(OrderMapper orderMapper, OrderCloseService orderCloseService) {
        this.orderMapper = orderMapper;
        this.orderCloseService = orderCloseService;
    }

    @Scheduled(cron = "${app.settle.close-cron}")
    public void closeOverdueOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(closeMinutes);
        List<Order> overdue = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, 0)
                        .le(Order::getCreatedAt, deadline)
                        .last("limit 500"));
        if (!overdue.isEmpty()) {
            log.info("close job found {} overdue pending order(s), deadline={}", overdue.size(), deadline);
        }
        overdue.forEach(o -> orderCloseService.closeIfPending(o.getOrderNo()));
    }
}