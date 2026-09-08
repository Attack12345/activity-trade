package com.example.activitytrade.rights.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 权益实体（user_right 表）。
 */
@TableName("user_right")
public class UserRight {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String rightNo;

    private Long userId;

    private Long activityId;

    private String orderNo;

    /** 1=优惠券 */
    private Integer rightType;

    /** 0待发放 1已发放 2发放失败 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRightNo() { return rightNo; }
    public void setRightNo(String rightNo) { this.rightNo = rightNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Integer getRightType() { return rightType; }
    public void setRightType(Integer rightType) { this.rightType = rightType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}