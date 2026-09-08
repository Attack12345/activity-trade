package com.example.activitytrade.order.service;

import com.example.activitytrade.activity.entity.Activity;
import com.example.activitytrade.activity.entity.ActivitySku;
import com.example.activitytrade.activity.service.ActivityService;
import com.example.activitytrade.common.api.ErrorCode;
import com.example.activitytrade.common.exception.BizException;
import com.example.activitytrade.common.util.IdGenerator;
import com.example.activitytrade.order.dto.SeckillResp;
import com.example.activitytrade.order.mq.TradeProducer;
import com.example.activitytrade.order.mq.TxResult;
import com.example.activitytrade.security.limiter.SlidingWindowRateLimiter;
import com.example.activitytrade.stock.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 抢购服务（M7，§8.1 第 5 步升级为事务消息）：
 * 限流 → 校验 → Redis Lua 预减 → RocketMQ 事务消息（半消息+本地事务+回查）下单 → 失败回补预减 → 发送关单延迟消息。
 */
@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);
    private static final String RATE_PATH = "/api/seckill/order";

    private final SlidingWindowRateLimiter rateLimiter;
    private final ActivityService activityService;
    private final StockService stockService;
    private final TradeProducer tradeProducer;

    public SeckillService(SlidingWindowRateLimiter rateLimiter, ActivityService activityService,
                          StockService stockService, TradeProducer tradeProducer) {
        this.rateLimiter = rateLimiter;
        this.activityService = activityService;
        this.stockService = stockService;
        this.tradeProducer = tradeProducer;
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
        // 4) 事务消息下单（半消息 → 本地事务 → 回查）；本地事务失败则回补预减
        String orderNo = IdGenerator.nextIdStr();
        TxResult tx = new TxResult();
        TradeProducer.Arg arg = new TradeProducer.Arg(orderNo, userId, activityId, skuId, tx);
        try {
            tradeProducer.sendOrderCreate(arg);
        } catch (Exception e) {
            log.error("send order tx message failed, compensate: act={},sku={},user={}", activityId, skuId, userId, e);
            stockService.compensate(activityId, skuId, userId);
            throw new BizException(ErrorCode.ORDER_CREATE_FAILED, "下单失败，请重试");
        }
        if (!tx.isCommit()) {
            stockService.compensate(activityId, skuId, userId);
            Integer code = tx.getErrorCode();
            if (code != null && code == ErrorCode.STOCK_DEDUCT_FAILED) {
                throw new BizException(ErrorCode.SOLD_OUT, "已售罄");
            }
            if (code != null && code == ErrorCode.DUPLICATE_BUY) {
                throw new BizException(ErrorCode.DUPLICATE_BUY, "重复参与");
            }
            throw new BizException(ErrorCode.ORDER_CREATE_FAILED, "下单失败，请重试");
        }
        // 5) 关单延迟消息（30min 未支付关单；仅发送，M8 消费）
        try {
            tradeProducer.sendCloseDelay(orderNo);
        } catch (Exception e) {
            log.warn("send close delay message failed, orderNo={}", orderNo, e);
        }
        return new SeckillResp(orderNo, 0);
    }
}