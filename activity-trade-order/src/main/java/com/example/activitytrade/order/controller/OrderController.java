package com.example.activitytrade.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activitytrade.common.api.PageResult;
import com.example.activitytrade.common.api.Result;
import com.example.activitytrade.order.entity.Order;
import com.example.activitytrade.order.mapper.OrderMapper;
import com.example.activitytrade.order.service.OrderWriteService;
import com.example.activitytrade.security.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单查询接口（API 契约 #13 mine / #14 result）。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderMapper orderMapper;
    private final OrderWriteService orderWriteService;

    public OrderController(OrderMapper orderMapper, OrderWriteService orderWriteService) {
        this.orderMapper = orderMapper;
        this.orderWriteService = orderWriteService;
    }

    @GetMapping("/mine")
    public Result<PageResult<Order>> mine(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        Page<Order> p = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, UserContext.getUserId())
                        .orderByDesc(Order::getCreatedAt));
        return Result.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @GetMapping("/result/{orderNo}")
    public Result<Order> result(@PathVariable String orderNo) {
        Order order = orderWriteService.getByOrderNo(orderNo);
        if (order != null && !order.getUserId().equals(UserContext.getUserId())) {
            return Result.fail(com.example.activitytrade.common.api.ErrorCode.FORBIDDEN, "无权查看该订单");
        }
        return Result.ok(order);
    }
}