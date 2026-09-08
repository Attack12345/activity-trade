package com.example.activitytrade.order.mq;

import com.example.activitytrade.activity.entity.Activity;
import com.example.activitytrade.activity.entity.ActivitySku;
import com.example.activitytrade.activity.entity.Product;
import com.example.activitytrade.activity.service.ActivityService;
import com.example.activitytrade.common.api.ErrorCode;
import com.example.activitytrade.common.exception.BizException;
import com.example.activitytrade.order.service.OrderWriteService;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 下单事务消息监听（M7，§8.1 第 5 步）。
 * - executeLocalTransaction：本地事务（DB 扣减+建单+明细+占用日志），成功 COMMIT / 失败 ROLLBACK
 * - checkLocalTransaction：半消息回查，以订单是否存在为准
 */
@Component
@RocketMQTransactionListener
public class SeckillOrderTxListener implements RocketMQLocalTransactionListener {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderTxListener.class);

    private final OrderWriteService orderWriteService;
    private final ActivityService activityService;

    public SeckillOrderTxListener(OrderWriteService orderWriteService, ActivityService activityService) {
        this.orderWriteService = orderWriteService;
        this.activityService = activityService;
    }

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        TradeProducer.Arg a = (TradeProducer.Arg) arg;
        try {
            Activity act = activityService.getActivityOrThrow(a.getActivityId());
            ActivitySku sku = activityService.getSku(a.getActivityId(), a.getSkuId());
            if (sku == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "商品不属于该活动");
            }
            Product product = activityService.getProduct(a.getSkuId());
            orderWriteService.createSeckillOrder(a.getUserId(), act, sku, product, a.getOrderNo());
            a.getResult().setCommit(true);
            a.getResult().setOrderNo(a.getOrderNo());
            log.info("local tx COMMIT: orderNo={}", a.getOrderNo());
            return RocketMQLocalTransactionState.COMMIT;
        } catch (BizException e) {
            log.warn("local tx ROLLBACK(biz): orderNo={}, code={}, msg={}", a.getOrderNo(), e.getCode(), e.getMessage());
            a.getResult().setCommit(false);
            a.getResult().setErrorCode(e.getCode());
            return RocketMQLocalTransactionState.ROLLBACK;
        } catch (Exception e) {
            log.error("local tx ROLLBACK(system): orderNo={}", a.getOrderNo(), e);
            a.getResult().setCommit(false);
            a.getResult().setErrorCode(ErrorCode.ORDER_CREATE_FAILED);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String orderNo = TradeProducer.orderNoOf(msg);
        boolean exists = orderWriteService.existsByOrderNo(orderNo);
        log.info("tx check(orderNo={}) -> {}", orderNo, exists ? "COMMIT" : "ROLLBACK");
        return exists ? RocketMQLocalTransactionState.COMMIT : RocketMQLocalTransactionState.ROLLBACK;
    }
}