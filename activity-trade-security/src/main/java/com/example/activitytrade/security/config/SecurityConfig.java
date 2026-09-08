package com.example.activitytrade.security.config;

import com.example.activitytrade.security.util.JwtUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全基础 Bean：JwtUtil / PasswordEncoder。
 * 与拦截器配置（WebConfig）分离，避免循环依赖。
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public JwtUtil jwtUtil(SecurityProperties props) {
        return new JwtUtil(props.getSecret(), props.getExpireMinutes());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}