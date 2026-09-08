package com.example.activitytrade.activity.dto;

import java.math.BigDecimal;

/**
 * 活动详情中的商品 VO（#4）。
 */
public class SkuVO {

    private Long skuId;
    private String title;
    private String image;
    private BigDecimal seckillPrice;
    private BigDecimal originalPrice;
    /** 剩余可抢库存（M4 阶段 = 配置库存，M6 起接入真实已售数） */
    private Integer stockLeft;
    /** 1=可抢 0=售罄 */
    private Integer stockStatus;

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public BigDecimal getSeckillPrice() { return seckillPrice; }
    public void setSeckillPrice(BigDecimal seckillPrice) { this.seckillPrice = seckillPrice; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public Integer getStockLeft() { return stockLeft; }
    public void setStockLeft(Integer stockLeft) { this.stockLeft = stockLeft; }
    public Integer getStockStatus() { return stockStatus; }
    public void setStockStatus(Integer stockStatus) { this.stockStatus = stockStatus; }
}