package com.example.activitytrade.security.interceptor;

import com.example.activitytrade.common.api.Result;
import com.example.activitytrade.security.UserContext;
import com.example.activitytrade.security.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录态与角色校验拦截器。
 * - 解析 Authorization: Bearer <token>，注入 {@link UserContext}
 * - /api/admin/** 要求 role=1，否则 403
 * - 未登录/无效 token → code 401
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PATH_PREFIX = "/api/admin/";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String header = request.getHeader("Authorization");
        String token = (header != null && header.startsWith(BEARER_PREFIX))
                ? header.substring(BEARER_PREFIX.length()) : null;
        if (token == null || token.isBlank()) {
            return reject(response, 401, "未登录或登录已过期");
        }
        Claims claims = jwtUtil.parse(token);
        if (claims == null) {
            return reject(response, 401, "未登录或登录已过期");
        }
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        Integer role = claims.get("role", Integer.class);
        if (request.getRequestURI().startsWith(ADMIN_PATH_PREFIX) && (role == null || role != 1)) {
            return reject(response, 403, "无权限");
        }
        UserContext.set(userId, username, role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean reject(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
        return false;
    }
}