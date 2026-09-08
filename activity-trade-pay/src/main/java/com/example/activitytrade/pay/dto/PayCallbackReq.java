package com.example.activitytrade.pay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 支付回调请求（#16，模拟支付网关回调）。
 */
public class PayCallbackReq {

    @NotBlank(message = "payNo 不能为空")
    private String payNo;

    @NotBlank(message = "orderNo 不能为空")
    private String orderNo;

    @NotNull(message = "amount 不能为空")
    private BigDecimal amount;

    @NotBlank(message = "sign 不能为空")
    private String sign;

    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }
}