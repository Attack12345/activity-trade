package com.example.activitytrade.order.mq;

import com.example.activitytrade.activity.service.ActivityService;
import com.example.activitytrade.common.mq.MqTopic;
import com.example.activitytrade.order.entity.Order;
import com.example.activitytrade.order.mapper.OrderMapper;
import com.example.activitytrade.settle.service.TradeLogService;
import com.example.activitytrade.stock.service.StockService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 关单延迟消息消费（M8，§8.3）：
 * 超时未支付 → 关单（status=2）+ DB 库存回补 + Redis 预库存回补/删占位 + 占用日志置回滚。
 * 与支付回调经 lock:close:{orderNo} 互斥；重复消费幂等（状态判据）。
 */
@Component
@RocketMQMessageListener(topic = MqTopic.ORDER_CLOSE_DELAY, consumerGroup = "TRADE_CLOSE_CONSUMER")
public class OrderCloseConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseConsumer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrderMapper orderMapper;
    private final ActivityService activityService;
    private final StockService stockService;
    private final TradeLogService tradeLogService;
    private final RedissonClient redissonClient;

    public OrderCloseConsumer(OrderMapper orderMapper, ActivityService activityService,
                              StockService stockService, TradeLogService tradeLogService,
                              RedissonClient redissonClient) {
        this.orderMapper = orderMapper;
        this.activityService = activityService;
        this.stockService = stockService;
        this.tradeLogService = tradeLogService;
        this.redissonClient = redissonClient;
    }

    @Override
    public void onMessage(String body) {
        String orderNo;
        try {
            orderNo = MAPPER.readTree(body).get("orderNo").asText();
        } catch (Exception e) {
            log.error("close msg unparseable: {}", body, e);
            return;
        }
        tradeLogService.record(orderNo + "-close", MqTopic.ORDER_CLOSE_DELAY, body);

        RLock lock = redissonClient.getLock("lock:close:" + orderNo);
        boolean locked = false;
        try {
            locked = lock.tryLock(1, 3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!locked) {
            log.warn("close lock busy, skip this delivery: orderNo={}", orderNo);
            return;
        }
        try {
            Order order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
            if (order == null || order.getStatus() != 0) {
                log.info("close skip(not pending): orderNo={}", orderNo);
                return;
            }
            // DB 库存回补
            activityService.returnSeckillStock(order.getActivityId(), order.getSkuId());
            // Redis 预库存回补 + 删占位
            stockService.compensate(order.getActivityId(), order.getSkuId(), order.getUserId());
            // 占用日志 → 回滚
            stockService.rollbackOccupied(orderNo);
            // 关单
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