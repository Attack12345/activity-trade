package com.example.activitytrade.security.limiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 自研滑动窗口限流器（M6）：Redis ZSET + Lua 原子执行。
 * key = rl:sw:{userId}:{path}，窗口 1s，阈值 app.seckill.rate-qps（单用户/秒）。
 */
@Component
public class SlidingWindowRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);
    private static final long WINDOW_MS = 1000L;

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> script;

    @Value("${app.seckill.rate-qps:20}")
    private long qps;

    public SlidingWindowRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>(readLua("lua/sliding_window.lua"), Long.class);
    }

    /** 尝试获取 1 个许可；超限返回 false */
    public boolean tryAcquire(Long userId, String path) {
        String key = "rl:sw:" + userId + ":" + path;
        Long result = redisTemplate.execute(script, List.of(key),
                System.currentTimeMillis(), WINDOW_MS, qps);
        return result != null && result == 1L;
    }

    private static String readLua(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("lua script not found: " + path, e);
        }
    }
}