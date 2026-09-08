package com.example.activitytrade.pay.dto;

import java.math.BigDecimal;

/**
 * 发起支付响应（#15）。
 */
public class PayMockResp {

    private final String payNo;
    private final BigDecimal amount;
    private final String sign;

    public PayMockResp(String payNo, BigDecimal amount, String sign) {
        this.payNo = payNo;
        this.amount = amount;
        this.sign = sign;
    }

    public String getPayNo() { return payNo; }
    public BigDecimal getAmount() { return amount; }
    public String getSign() { return sign; }
}