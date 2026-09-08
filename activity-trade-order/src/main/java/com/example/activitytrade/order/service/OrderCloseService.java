package com.example.activitytrade.order.service;

import com.example.activitytrade.activity.service.ActivityService;
import com.example.activitytrade.order.entity.Order;
import com.example.activitytrade.order.mapper.OrderMapper;
import com.example.activitytrade.stock.service.StockService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 关单服务（M9 抽取，供延迟消费与兜底任务复用）：
 * 未支付订单 → status=2 + DB/Redis 双回补 + 占用日志置回滚。
 * lock:close:{orderNo} 与支付回调互斥；状态判据幂等。
 */
@Service
public class OrderCloseService {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseService.class);

    private final OrderMapper orderMapper;
    private final ActivityService activityService;
    private final StockService stockService;
    private final RedissonClient redissonClient;

    public OrderCloseService(OrderMapper orderMapper, ActivityService activityService,
                             StockService stockService, RedissonClient redissonClient) {
        this.orderMapper = orderMapper;
        this.activityService = activityService;
        this.stockService = stockService;
        this.redissonClient = redissonClient;
    }

    /** 关单（幂等）：仅当存在且仍为待支付(0)时执行 */
    public void closeIfPending(String orderNo) {
        RLock lock = redissonClient.getLock("lock:close:" + orderNo);
        boolean locked = false;
        try {
            locked = lock.tryLock(1, 3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!locked) {
            log.warn("close lock busy, skip: orderNo={}", orderNo);
            return;
        }
        try {
            Order order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
            if (order == null || order.getStatus() != 0) {
                log.info("close skip(not pending): orderNo={}", orderNo);
                return;
            }
            activityService.returnSeckillStock(order.getActivityId(), order.getSkuId());
            stockService.compensate(order.getActivityId(), order.getSkuId(), order.getUserId());
            stockService.rollbackOccupied(orderNo);
            orderMapper.update(null,
                    new LambdaUpdateWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo)
                            .set(Order::getStatus, 2)
                            .set(Order::getClosedTime, LocalDateTime.now()));
            log.info("order closed & stock restored: orderNo={}", orderNo);
        } finally {
            lock.unlock();
        }
    }
}