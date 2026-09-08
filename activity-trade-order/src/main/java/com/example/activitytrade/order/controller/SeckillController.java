package com.example.activitytrade.order.controller;

import com.example.activitytrade.common.api.Result;
import com.example.activitytrade.order.dto.SeckillReq;
import com.example.activitytrade.order.dto.SeckillResp;
import com.example.activitytrade.order.service.SeckillService;
import com.example.activitytrade.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 抢购接口（API 契约 #12，需登录）。
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/order")
    public Result<SeckillResp> order(@Valid @RequestBody SeckillReq req) {
        Long userId = UserContext.getUserId();
        return Result.ok(seckillService.seckill(userId, req.getActivityId(), req.getSkuId()));
    }
}