package com.example.activitytrade.activity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 活动状态流转请求（Admin #7）。
 */
public class StatusUpdateReq {

    @NotNull(message = "status 不能为空")
    @Min(value = 0, message = "status 非法")
    @Max(value = 3, message = "status 非法")
    private Integer status;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}