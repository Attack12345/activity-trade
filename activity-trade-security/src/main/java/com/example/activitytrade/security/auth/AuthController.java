package com.example.activitytrade.security.auth;

import com.example.activitytrade.common.api.Result;
import com.example.activitytrade.security.auth.dto.LoginReq;
import com.example.activitytrade.security.auth.dto.LoginResp;
import com.example.activitytrade.security.auth.dto.RegisterReq;
import com.example.activitytrade.security.idempotent.Idempotent;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（M3，API 契约 #1/#2）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Idempotent(prefix = "register", key = "#p0.username", ttlSeconds = 30)
    public Result<LoginResp> register(@Valid @RequestBody RegisterReq req) {
        return Result.ok(authService.register(req));
    }

    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(authService.login(req));
    }
}