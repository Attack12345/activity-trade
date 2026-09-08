package com.example.activitytrade.activity.controller;

import com.example.activitytrade.activity.dto.ActivityCreateReq;
import com.example.activitytrade.activity.dto.ActivityUpdateReq;
import com.example.activitytrade.activity.dto.SkuUpsertReq;
import com.example.activitytrade.activity.dto.StatusUpdateReq;
import com.example.activitytrade.activity.entity.Activity;
import com.example.activitytrade.activity.service.ActivityService;
import com.example.activitytrade.common.api.PageResult;
import com.example.activitytrade.common.api.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端活动接口（API 契约 #5~#9），/api/admin/** 由拦截器校验 role=1。
 */
@RestController
@RequestMapping("/api/admin/activities")
public class AdminActivityController {

    private final ActivityService activityService;

    public AdminActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public Result<PageResult<Activity>> list(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) Integer status) {
        return Result.ok(activityService.listForAdmin(page, size, status));
    }

    @PostMapping
    public Result<Activity> create(@Valid @RequestBody ActivityCreateReq req) {
        return Result.ok(activityService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Activity> update(@PathVariable Long id, @Valid @RequestBody ActivityUpdateReq req) {
        return Result.ok(activityService.update(id, req));
    }

    @PostMapping("/{id}/status")
    public Result<Activity> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateReq req) {
        return Result.ok(activityService.changeStatus(id, req.getStatus()));
    }

    @PostMapping("/{id}/skus")
    public Result<Integer> saveSkus(@PathVariable Long id, @Valid @RequestBody List<SkuUpsertReq> items) {
        return Result.ok(activityService.saveSkus(id, items));
    }
}