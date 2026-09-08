package com.example.activitytrade.activity.controller;

import com.example.activitytrade.activity.dto.ActivityDetailVO;
import com.example.activitytrade.activity.entity.Activity;
import com.example.activitytrade.activity.service.ActivityService;
import com.example.activitytrade.common.api.PageResult;
import com.example.activitytrade.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端活动接口（API 契约 #3/#4）。
 */
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public Result<PageResult<Activity>> list(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(activityService.listForUser(page, size));
    }

    @GetMapping("/{id}")
    public Result<ActivityDetailVO> detail(@PathVariable Long id) {
        return Result.ok(activityService.detail(id));
    }
}