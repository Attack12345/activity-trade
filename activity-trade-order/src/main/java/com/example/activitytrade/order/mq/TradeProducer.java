package com.example.activitytrade.order.mq;

import com.example.activitytrade.common.mq.MqTopic;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 交易消息生产者（M7）：事务消息下单 + 关单延迟消息。
 */
@Component
public class TradeProducer {

    private static final Logger log = LoggerFactory.getLogger(TradeProducer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RocketMQTemplate rocketMQTemplate;
    private final int closeDelayLevel;

    public TradeProducer(RocketMQTemplate rocketMQTemplate,
                         @Value("${app.seckill.close-delay-level:18}") int closeDelayLevel) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.closeDelayLevel = closeDelayLevel;
    }

    /** 下单事务消息（半消息）；本地事务同步执行，结果写入 arg.result */
    public void sendOrderCreate(Arg arg) {
        Message<String> msg = MessageBuilder.withPayload(payload(arg)).build();
        rocketMQTemplate.sendMessageInTransaction(MqTopic.ORDER_CREATE, msg, arg);
        log.info("order tx message sent: orderNo={}, localTxCommit={}",
                arg.getOrderNo(), arg.getResult().isCommit());
    }

    /** 关单延迟消息（仅发送与留痕；消费在 M8） */
    public void sendCloseDelay(String orderNo) {
        String body = "{\"orderNo\":\"" + orderNo + "\"}";
        Message<String> msg = MessageBuilder.withPayload(body).build();
        rocketMQTemplate.syncSend(MqTopic.ORDER_CLOSE_DELAY, msg, 3000L, closeDelayLevel);
    }

    static String orderNoOf(Message msg) {
        try {
            String payload = (String) msg.getPayload();
            return MAPPER.readTree(payload).get("orderNo").asText();
        } catch (Exception e) {
            throw new IllegalStateException("bad order tx payload", e);
        }
    }

    private String payload(Arg arg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderNo", arg.getOrderNo());
        map.put("userId", arg.getUserId());
        map.put("activityId", arg.getActivityId());
        map.put("skuId", arg.getSkuId());
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("serialize payload failed", e);
        }
    }

    /** 本地事务参数（进程内传递） */
    public static class Arg {
        private final String orderNo;
        private final Long userId;
        private final Long activityId;
        private final Long skuId;
        private final TxResult result;

        public Arg(String orderNo, Long userId, Long activityId, Long skuId, TxResult result) {
            this.orderNo = orderNo;
            this.userId = userId;
            this.activityId = activityId;
            this.skuId = skuId;
            this.result = result;
        }

        public String getOrderNo() { return orderNo; }
        public Long getUserId() { return userId; }
        public Long getActivityId() { return activityId; }
        public Long getSkuId() { return skuId; }
        public TxResult getResult() { return result; }
    }
}