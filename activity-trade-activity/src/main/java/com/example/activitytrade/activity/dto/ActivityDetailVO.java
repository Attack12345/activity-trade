package com.example.activitytrade.activity.dto;

import com.example.activitytrade.activity.entity.Activity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动详情 VO（#4）。
 */
public class ActivityDetailVO {

    private Long id;
    private String name;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<SkuVO> skus;

    public static ActivityDetailVO of(Activity activity, List<SkuVO> skus) {
        ActivityDetailVO vo = new ActivityDetailVO();
        vo.setId(activity.getId());
        vo.setName(activity.getName());
        vo.setStatus(activity.getStatus());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setSkus(skus);
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public List<SkuVO> getSkus() { return skus; }
    public void setSkus(List<SkuVO> skus) { this.skus = skus; }
}