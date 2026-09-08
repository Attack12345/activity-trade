package com.example.activitytrade.security.auth.dto;

/**
 * 登录响应。
 */
public class LoginResp {

    private final String token;
    private final Long userId;
    private final String nickname;
    private final Integer role;

    public LoginResp(String token, Long userId, String nickname, Integer role) {
        this.token = token;
        this.userId = userId;
        this.nickname = nickname;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public Integer getRole() {
        return role;
    }
}