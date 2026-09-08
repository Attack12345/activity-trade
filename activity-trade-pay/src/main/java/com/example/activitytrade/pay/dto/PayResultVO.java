package com.example.activitytrade.pay.dto;

/**
 * 支付结果（#16 回调响应）。
 */
public class PayResultVO {

    private final String payNo;
    /** 1=已支付 */
    private final Integer status;

    public PayResultVO(String payNo, Integer status) {
        this.payNo = payNo;
        this.status = status;
    }

    public String getPayNo() { return payNo; }
    public Integer getStatus() { return status; }
}