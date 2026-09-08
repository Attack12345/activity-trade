package com.example.activitytrade.order.service;

import com.example.activitytrade.activity.entity.Activity;
import com.example.activitytrade.activity.entity.ActivitySku;
import com.example.activitytrade.activity.entity.Product;
import com.example.activitytrade.activity.service.ActivityService;
import com.example.activitytrade.common.api.ErrorCode;
import com.example.activitytrade.common.exception.BizException;
import com.example.activitytrade.order.dto.SeckillResp;
import com.example.activitytrade.order.entity.Order;
import com.example.activitytrade.security.limiter.SlidingWindowRateLimiter;
import com.example.activitytrade.stock.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 抢购服务（M6，§8.1 1~5 步；第 5 步 M7 替换为事务消息）：
 * 限流 → 活动/商品校验 → Redis Lua 预减 → 本地事务建单；失败回补预减。
 */
@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);
    private static final String RATE_PATH = "/api/seckill/order";

    private final SlidingWindowRateLimiter rateLimiter;
    private final ActivityService activityService;
    private final StockService stockService;
    private final OrderWriteService orderWriteService;

    public SeckillService(SlidingWindowRateLimiter rateLimiter, ActivityService activityService,
                          StockService stockService, OrderWriteService orderWriteService) {
        this.rateLimiter = rateLimiter;
        this.activityService = activityService;
        this.stockService = stockService;
        this.orderWriteService = orderWriteService;
    }

    public SeckillResp seckill(Long userId, Long activityId, Long skuId) {
        // 1) 限流（单用户 1s 窗口）
        if (!rateLimiter.tryAcquire(userId, RATE_PATH)) {
            throw new BizException(ErrorCode.RATE_LIMITED, "请求过于频繁，请稍后再试");
        }
        // 2) 活动校验：存在 + 进行中 + 时间窗口
        Activity activity = activityService.getActivityOrThrow(activityId);
        if (activity.getStatus() != 2) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_OPEN, "活动未开始或已结束");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_OPEN, "活动未开始或已结束");
        }
        ActivitySku sku = activityService.getSku(activityId, skuId);
        if (sku == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "商品不属于该活动");
        }
        // 3) Redis Lua 原子预减
        StockService.Result result = stockService.preReduce(activityId, skuId, userId);
        if (result == StockService.Result.DUPLICATE) {
            throw new BizException(ErrorCode.DUPLICATE_BUY, "重复参与");
        }
        if (result == StockService.Result.SOLD_OUT) {
            throw new BizException(ErrorCode.SOLD_OUT, "已售罄");
        }
        // 4) 本地事务落库；任何失败回补预减
        Product product = activityService.getProduct(skuId);
        try {
            Order order = orderWriteService.createSeckillOrder(userId, activity, sku, product);
            return new SeckillResp(order.getOrderNo(), order.getStatus());
        } catch (BizException e) {
            stockService.compensate(activityId, skuId, userId);
            throw e;
        } catch (Exception e) {
            log.error("seckill order failed, compensate: user={},act={},sku={}", userId, activityId, skuId, e);
            stockService.compensate(activityId, skuId, userId);
            throw new BizException(ErrorCode.ORDER_CREATE_FAILED, "下单失败，请重试");
        }
    }
}