package com.example.activitytrade.order.mq;

import com.example.activitytrade.common.mq.MqTopic;
import com.example.activitytrade.order.service.OrderCloseService;
import com.example.activitytrade.settle.service.TradeLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 关单延迟消息消费（M8/M9，§8.3）：委托 {@link OrderCloseService}（含锁互斥与双回补）。
 */
@Component
@RocketMQMessageListener(topic = MqTopic.ORDER_CLOSE_DELAY, consumerGroup = "TRADE_CLOSE_CONSUMER")
public class OrderCloseConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseConsumer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrderCloseService orderCloseService;
    private final TradeLogService tradeLogService;

    public OrderCloseConsumer(OrderCloseService orderCloseService, TradeLogService tradeLogService) {
        this.orderCloseService = orderCloseService;
        this.tradeLogService = tradeLogService;
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
        orderCloseService.closeIfPending(orderNo);
    }
}