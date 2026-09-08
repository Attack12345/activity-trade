package com.example.activitytrade.order.mq;

import com.example.activitytrade.common.mq.MqTopic;
import com.example.activitytrade.settle.service.TradeLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 下单消息消费者骨架（M7）：幂等落库 trade_log（观察/对账留痕）。
 * M8 起在此基础上扩展支付/发券等履约处理。
 */
@Component
@RocketMQMessageListener(topic = MqTopic.ORDER_CREATE, consumerGroup = "TRADE_CONSUMER")
public class OrderCreateConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(OrderCreateConsumer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TradeLogService tradeLogService;

    public OrderCreateConsumer(TradeLogService tradeLogService) {
        this.tradeLogService = tradeLogService;
    }

    @Override
    public void onMessage(String body) {
        try {
            JsonNode node = MAPPER.readTree(body);
            String orderNo = node.get("orderNo").asText();
            tradeLogService.record(orderNo, MqTopic.ORDER_CREATE, body);
            log.info("order create msg consumed: orderNo={}", orderNo);
        } catch (Exception e) {
            // 解析/记录失败：标记死信避免无限重试（M9 对账补偿可见）
            log.error("order create consume failed: {}", body, e);
            try {
                String orderNo = MAPPER.readTree(body).get("orderNo").asText();
                tradeLogService.markDead(orderNo);
            } catch (Exception ignored) {
                log.error("unparseable order create payload: {}", body);
            }
        }
    }
}