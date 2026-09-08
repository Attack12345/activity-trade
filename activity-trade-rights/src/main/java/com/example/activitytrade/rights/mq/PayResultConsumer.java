package com.example.activitytrade.rights.mq;

import com.example.activitytrade.common.mq.MqTopic;
import com.example.activitytrade.rights.service.UserRightService;
import com.example.activitytrade.settle.service.TradeLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 支付成功履约消费（M8，单段）：发券 user_right(status=1)；异常标记死信（M9 补偿可见）。
 */
@Component
@RocketMQMessageListener(topic = MqTopic.PAY_RESULT, consumerGroup = "TRADE_PAY_CONSUMER")
public class PayResultConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(PayResultConsumer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final UserRightService userRightService;
    private final TradeLogService tradeLogService;

    public PayResultConsumer(UserRightService userRightService, TradeLogService tradeLogService) {
        this.userRightService = userRightService;
        this.tradeLogService = tradeLogService;
    }

    @Override
    public void onMessage(String body) {
        try {
            JsonNode node = MAPPER.readTree(body);
            String orderNo = node.get("orderNo").asText();
            Long userId = node.get("userId").asLong();
            Long activityId = node.get("activityId").asLong();
            Integer rightType = node.has("rightType") ? node.get("rightType").asInt() : 1;
            tradeLogService.record(orderNo + "-right", MqTopic.PAY_RESULT, body);
            userRightService.issue(orderNo, userId, activityId, rightType);
            log.info("pay result consumed(issue right): orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("pay result consume failed: {}", body, e);
            try {
                String orderNo = MAPPER.readTree(body).get("orderNo").asText();
                tradeLogService.markDead(orderNo + "-right");
            } catch (Exception ignored) {
                log.error("unparseable pay result payload: {}", body);
            }
        }
    }
}