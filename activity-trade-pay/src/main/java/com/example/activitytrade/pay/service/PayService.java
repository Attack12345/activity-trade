package com.example.activitytrade.pay.service;

import com.example.activitytrade.common.api.ErrorCode;
import com.example.activitytrade.common.exception.BizException;
import com.example.activitytrade.common.mq.MqTopic;
import com.example.activitytrade.common.util.IdGenerator;
import com.example.activitytrade.order.entity.Order;
import com.example.activitytrade.order.service.OrderWriteService;
import com.example.activitytrade.pay.dto.PayCallbackReq;
import com.example.activitytrade.pay.dto.PayMockResp;
import com.example.activitytrade.pay.dto.PayResultVO;
import com.example.activitytrade.pay.entity.Payment;
import com.example.activitytrade.pay.mapper.PaymentMapper;
import com.example.activitytrade.pay.util.SignUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务（M8，§8.5 Mock 网关）。
 * - mock：订单待支付校验 → 生成 payNo + HMAC 预签名 → 建支付单
 * - callback：验签/金额 → 与关单互斥(lock:close) → 订单 0→1 → 支付单 0→1 → 发 TRADE_PAY_RESULT
 * 幂等：payNo 唯一 + 订单/支付单条件更新（status=0 才允许转换）+ 与关单同锁。
 */
@Service
public class PayService {

    private static final Logger log = LoggerFactory.getLogger(PayService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PaymentMapper paymentMapper;
    private final OrderWriteService orderWriteService;
    private final RocketMQTemplate rocketMQTemplate;
    private final RedissonClient redissonClient;

    @Value("${app.jwt.secret}")
    private String secret;

    public PayService(PaymentMapper paymentMapper, OrderWriteService orderWriteService,
                      RocketMQTemplate rocketMQTemplate, RedissonClient redissonClient) {
        this.paymentMapper = paymentMapper;
        this.orderWriteService = orderWriteService;
        this.rocketMQTemplate = rocketMQTemplate;
        this.redissonClient = redissonClient;
    }

    @Transactional(rollbackFor = Exception.class)
    public PayMockResp mock(String orderNo) {
        Order order = orderWriteService.getByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "订单状态不允许支付");
        }
        String payNo = IdGenerator.nextIdStr();
        String sign = SignUtil.sign(secret, payNo, orderNo, order.getPrice().toPlainString());
        Payment payment = new Payment();
        payment.setPayNo(payNo);
        payment.setOrderNo(orderNo);
        payment.setUserId(order.getUserId());
        payment.setAmount(order.getPrice());
        payment.setStatus(0);
        paymentMapper.insert(payment);
        log.info("pay mock created: payNo={}, orderNo={}", payNo, orderNo);
        return new PayMockResp(payNo, order.getPrice(), sign);
    }

    public PayResultVO callback(PayCallbackReq req) {
        if (!SignUtil.verify(secret, req.getPayNo(), req.getOrderNo(),
                req.getAmount().toPlainString(), req.getSign())) {
            throw new BizException(ErrorCode.PAY_VERIFY_FAILED, "支付签名校验失败");
        }
        RLock lock = redissonClient.getLock("lock:close:" + req.getOrderNo());
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!locked) {
            throw new BizException(ErrorCode.ORDER_CREATE_FAILED, "支付处理繁忙，请重试");
        }
        try {
            return doCallback(req);
        } finally {
            lock.unlock();
        }
    }

    private PayResultVO doCallback(PayCallbackReq req) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPayNo, req.getPayNo()));
        if (payment == null || !payment.getOrderNo().equals(req.getOrderNo())
                || payment.getAmount().compareTo(req.getAmount()) != 0) {
            throw new BizException(ErrorCode.PAY_VERIFY_FAILED, "支付单校验失败");
        }
        if (payment.getStatus() == 1) {
            log.info("pay callback idempotent skip: payNo={}", req.getPayNo());
            return new PayResultVO(payment.getPayNo(), 1);
        }
        Order order = orderWriteService.getByOrderNo(req.getOrderNo());
        if (order == null || order.getStatus() != 0) {
            throw new BizException(ErrorCode.PAY_VERIFY_FAILED, "订单已关闭，支付失败");
        }
        // 先置订单已支付（status=0 → 1 条件更新，防与关单覆盖），再置支付单
        boolean paid = orderWriteService.markPaid(req.getOrderNo());
        if (!paid) {
            throw new BizException(ErrorCode.PAY_VERIFY_FAILED, "订单已关闭，支付失败");
        }
        paymentMapper.update(null,
                new LambdaUpdateWrapper<Payment>()
                        .eq(Payment::getPayNo, req.getPayNo())
                        .eq(Payment::getStatus, 0)
                        .set(Payment::getStatus, 1)
                        .set(Payment::getCallbackTime, LocalDateTime.now()));
        log.info("pay callback success: payNo={}, orderNo={}", req.getPayNo(), req.getOrderNo());
        sendPayResult(req.getPayNo(), order);
        return new PayResultVO(req.getPayNo(), 1);
    }

    /** 发 TRADE_PAY_RESULT：履约（发券）由 rights 模块消费 */
    private void sendPayResult(String payNo, Order order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("payNo", payNo);
        payload.put("orderNo", order.getOrderNo());
        payload.put("userId", order.getUserId());
        payload.put("activityId", order.getActivityId());
        payload.put("skuId", order.getSkuId());
        try {
            String json = MAPPER.writeValueAsString(payload);
            rocketMQTemplate.syncSend(MqTopic.PAY_RESULT,
                    MessageBuilder.withPayload(json).build(), 3000L);
            log.info("TRADE_PAY_RESULT sent: orderNo={}", order.getOrderNo());
        } catch (Exception e) {
            log.error("send PAY_RESULT failed: orderNo={}", order.getOrderNo(), e);
        }
    }
}