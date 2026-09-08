package com.example.activitytrade.security.idempotent;

import com.example.activitytrade.common.api.ErrorCode;
import com.example.activitytrade.common.exception.BizException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @Idempotent 切面：SETNX 防重，执行后删除（允许顺序重试），并发窗口期内拦截重复提交。
 */
@Aspect
@Component
public class IdempotentAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);
    private static final String KEY_PREFIX = "idem:";
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final RedisTemplate<String, Object> redisTemplate;

    public IdempotentAspect(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        String redisKey = buildKey(pjp, idempotent);
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", Duration.ofSeconds(idempotent.ttlSeconds()));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请勿重复提交");
        }
        try {
            return pjp.proceed();
        } finally {
            redisTemplate.delete(redisKey);
        }
    }

    private String buildKey(ProceedingJoinPoint pjp, Idempotent idempotent) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        Object[] args = pjp.getArgs();
        for (int i = 0; i < args.length; i++) {
            ctx.setVariable("p" + i, args[i]);
        }
        String keyPart;
        try {
            keyPart = PARSER.parseExpression(idempotent.key()).getValue(ctx, String.class);
        } catch (RuntimeException e) {
            log.warn("idempotent spEL eval failed, fallback to method name. key={}", idempotent.key());
            keyPart = pjp.getSignature().toShortString();
        }
        return KEY_PREFIX + idempotent.prefix() + ":" + keyPart;
    }
}