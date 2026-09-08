package com.example.activitytrade.security.idempotent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等注解（Redis SETNX 前置防重）。
 * key 为 SpEL 表达式，参数绑定 #p0..#pN（如 #p0.username）。
 * 说明：仅作并发窗口期的前置拦截，最终幂等以 DB 唯一约束兜底。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 前缀，最终 key = idem:{prefix}:{key} */
    String prefix();

    /** SpEL：基于请求参数生成 key，如 #p0.username */
    String key();

    /** key 存活时间（秒），覆盖处理窗口 */
    int ttlSeconds() default 60;
}