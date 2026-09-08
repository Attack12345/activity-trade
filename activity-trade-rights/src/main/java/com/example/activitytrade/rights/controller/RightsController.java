package com.example.activitytrade.rights.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activitytrade.common.api.PageResult;
import com.example.activitytrade.common.api.Result;
import com.example.activitytrade.rights.entity.UserRight;
import com.example.activitytrade.rights.mapper.UserRightMapper;
import com.example.activitytrade.security.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的权益（API 契约 #17）。
 */
@RestController
@RequestMapping("/api/rights")
public class RightsController {

    private final UserRightMapper userRightMapper;

    public RightsController(UserRightMapper userRightMapper) {
        this.userRightMapper = userRightMapper;
    }

    @GetMapping("/mine")
    public Result<PageResult<UserRight>> mine(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        Page<UserRight> p = userRightMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UserRight>()
                        .eq(UserRight::getUserId, UserContext.getUserId())
                        .orderByDesc(UserRight::getCreatedAt));
        return Result.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }
}