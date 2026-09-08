package com.example.activitytrade.bootstrap.web;

import com.example.activitytrade.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端探针（M3 用于验证 /api/admin/** 角色校验；M4 起被真实管理接口替代/并存）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("pong");
    }
}