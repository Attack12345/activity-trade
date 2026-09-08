package com.example.activitytrade.pay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 发起支付请求（#15）。
 */
public class PayMockReq {

    @NotBlank(message = "orderNo 不能为空")
    private String orderNo;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
}