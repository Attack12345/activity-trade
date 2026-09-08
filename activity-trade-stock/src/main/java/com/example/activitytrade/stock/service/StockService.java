package com.example.activitytrade.stock.service;

import com.example.activitytrade.stock.entity.StockDeductLog;
import com.example.activitytrade.stock.mapper.StockDeductLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 库存服务（M6）：Redis Lua 预减（原子）/ 回补 + 库存扣减日志。
 * key 约定见 docs/development-guide.md §4.3：stock:{act}:{sku}:{seg}、buyer:{act}:{sku}:{userId}。
 */
@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final StockDeductLogMapper stockDeductLogMapper;
    private final DefaultRedisScript<Long> seckillScript;

    @Value("${app.seckill.stock-segments:1}")
    private int stockSegments;

    @Value("${app.seckill.stock-ttl-minutes:120}")
    private int stockTtlMinutes;

    public StockService(RedisTemplate<String, Object> redisTemplate, StockDeductLogMapper stockDeductLogMapper) {
        this.redisTemplate = redisTemplate;
        this.stockDeductLogMapper = stockDeductLogMapper;
        this.seckillScript = new DefaultRedisScript<>(readLua("lua/seckill.lua"), Long.class);
    }

    /** 抢购结果 */
    public enum Result {
        /** 预减成功 */
        SUCCESS,
        /** 售罄（已回补，无副作用） */
        SOLD_OUT,
        /** 已参与（同一用户重复抢购） */
        DUPLICATE
    }

    /** 分段分配：segment = userId % segments（默认 1 段） */
    public int segmentOf(Long userId) {
        return (int) (Math.floorMod(userId, Math.max(stockSegments, 1)));
    }

    /** 原子预减（Lua）：先占位、后扣减、负则回滚 */
    public Result preReduce(Long activityId, Long skuId, Long userId) {
        int segment = segmentOf(userId);
        String stockKey = stockKey(activityId, skuId, segment);
        String buyerKey = buyerKey(activityId, skuId, userId);
        long ttl = stockTtlMinutes * 60L;
        Long code = redisTemplate.execute(seckillScript, List.of(stockKey, buyerKey), ttl);
        if (code == null) {
            log.error("seckill lua returned null: act={},sku={},user={}", activityId, skuId, userId);
            return Result.SOLD_OUT;
        }
        if (code == 1) {
            return Result.SUCCESS;
        }
        if (code == -2) {
            return Result.DUPLICATE;
        }
        return Result.SOLD_OUT;
    }

    /** 回补：预库存 +1 并删除用户占位（DB 落库失败/取消订单时调用） */
    public void compensate(Long activityId, Long skuId, Long userId) {
        int segment = segmentOf(userId);
        redisTemplate.opsForValue()
                .increment(stockKey(activityId, skuId, segment));
        redisTemplate.delete(buyerKey(activityId, skuId, userId));
        log.info("stock compensated: act={},sku={},user={},seg={}", activityId, skuId, userId, segment);
    }

    /** 记录库存占用日志（随订单事务一起提交） */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void logOccupied(String orderNo, Long activityId, Long skuId) {
        StockDeductLog logEntry = new StockDeductLog();
        logEntry.setBizNo(orderNo);
        logEntry.setActivityId(activityId);
        logEntry.setSkuId(skuId);
        logEntry.setOrderNo(orderNo);
        logEntry.setDelta(1);
        logEntry.setStatus(0);
        stockDeductLogMapper.insert(logEntry);
    }

    /** M8：占用日志置回滚（关单/退款），幂等 */
    public int rollbackOccupied(String orderNo) {
        return stockDeductLogMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<StockDeductLog>()
                        .eq(StockDeductLog::getOrderNo, orderNo)
                        .eq(StockDeductLog::getStatus, 0)
                        .set(StockDeductLog::getStatus, 2));
    }

    private String stockKey(Long activityId, Long skuId, int segment) {
        return "stock:" + activityId + ":" + skuId + ":" + segment;
    }

    private String buyerKey(Long activityId, Long skuId, Long userId) {
        return "buyer:" + activityId + ":" + skuId + ":" + userId;
    }

    private static String readLua(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("lua script not found: " + path, e);
        }
    }
}