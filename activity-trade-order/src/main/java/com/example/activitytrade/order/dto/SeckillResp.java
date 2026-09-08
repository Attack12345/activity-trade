package com.example.activitytrade.order.dto;

/**
 * 抢购响应（#12）。
 */
public class SeckillResp {

    private final String orderNo;
    /** 0=待支付（下单成功） */
    private final Integer status;

    public SeckillResp(String orderNo, Integer status) {
        this.orderNo = orderNo;
        this.status = status;
    }

    public String getOrderNo() { return orderNo; }
    public Integer getStatus() { return status; }
}