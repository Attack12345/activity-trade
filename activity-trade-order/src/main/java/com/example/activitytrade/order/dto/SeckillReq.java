package com.example.activitytrade.order.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 抢购请求（#12）。
 */
public class SeckillReq {

    @NotNull(message = "activityId 不能为空")
    private Long activityId;

    @NotNull(message = "skuId 不能为空")
    private Long skuId;

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
}