package com.example.activitytrade.pay.controller;

import com.example.activitytrade.common.api.Result;
import com.example.activitytrade.pay.dto.PayCallbackReq;
import com.example.activitytrade.pay.dto.PayMockReq;
import com.example.activitytrade.pay.dto.PayMockResp;
import com.example.activitytrade.pay.dto.PayResultVO;
import com.example.activitytrade.pay.service.PayService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付接口（M8，#15 mock 发起 / #16 callback 回调）。
 */
@RestController
@RequestMapping("/api/pay")
public class PayController {

    private final PayService payService;

    public PayController(PayService payService) {
        this.payService = payService;
    }

    @PostMapping("/mock")
    public Result<PayMockResp> mock(@Valid @RequestBody PayMockReq req) {
        return Result.ok(payService.mock(req.getOrderNo()));
    }

    @PostMapping("/callback")
    public Result<PayResultVO> callback(@Valid @RequestBody PayCallbackReq req) {
        return Result.ok(payService.callback(req));
    }
}