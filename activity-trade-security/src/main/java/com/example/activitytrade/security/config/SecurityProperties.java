package com.example.activitytrade.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置（application.yml: app.jwt）。
 */
@ConfigurationProperties(prefix = "app.jwt")
public class SecurityProperties {

    /** HS256 密钥（≥32 字节） */
    private String secret;

    /** 过期分钟数 */
    private int expireMinutes = 120;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(int expireMinutes) {
        this.expireMinutes = expireMinutes;
    }
}