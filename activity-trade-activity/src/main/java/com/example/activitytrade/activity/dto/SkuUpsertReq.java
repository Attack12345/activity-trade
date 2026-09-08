package com.example.activitytrade.activity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 活动商品 upsert 项（Admin #8）。
 */
public class SkuUpsertReq {

    @NotNull(message = "skuId 不能为空")
    private Long skuId;

    @NotNull(message = "秒杀价不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "秒杀价必须大于 0")
    private BigDecimal seckillPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "原价必须大于 0")
    private BigDecimal originalPrice;

    @NotNull(message = "秒杀库存不能为空")
    @Min(value = 0, message = "秒杀库存不能为负")
    private Integer seckillStock;

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public BigDecimal getSeckillPrice() { return seckillPrice; }
    public void setSeckillPrice(BigDecimal seckillPrice) { this.seckillPrice = seckillPrice; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public Integer getSeckillStock() { return seckillStock; }
    public void setSeckillStock(Integer seckillStock) { this.seckillStock = seckillStock; }
}