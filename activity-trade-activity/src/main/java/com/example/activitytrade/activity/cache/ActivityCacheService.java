package com.example.activitytrade.activity.cache;

import com.example.activitytrade.activity.dto.ActivityDetailVO;
import com.example.activitytrade.activity.dto.SkuVO;
import com.example.activitytrade.activity.entity.Activity;
import com.example.activitytrade.activity.entity.ActivitySku;
import com.example.activitytrade.activity.entity.ActivityStatus;
import com.example.activitytrade.activity.entity.Product;
import com.example.activitytrade.activity.mapper.ActivityMapper;
import com.example.activitytrade.activity.mapper.ActivitySkuMapper;
import com.example.activitytrade.activity.mapper.ProductMapper;
import com.example.activitytrade.common.api.ErrorCode;
import com.example.activitytrade.common.exception.BizException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 活动缓存服务（M5）：缓存三兄弟 + 预热。
 * 读路径：全局布隆(穿透拦截) → 空值缓存 → Caffeine(本地) → Redis(远程) → 单飞重建(击穿) → DB 兜底。
 * 写路径：prepare() 幂等预热，重建全部缓存与预库存分段。
 */
@Service
public class ActivityCacheService {

    private static final Logger log = LoggerFactory.getLogger(ActivityCacheService.class);

    private final ActivityMapper activityMapper;
    private final ActivitySkuMapper activitySkuMapper;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final Cache<String, ActivityDetailVO> detailCache;

    @Value("${app.seckill.bloom-total:1000000}")
    private long bloomTotal;

    @Value("${app.seckill.bloom-fpp:0.001}")
    private double bloomFpp;

    @Value("${app.seckill.stock-segments:1}")
    private int stockSegments;

    public ActivityCacheService(ActivityMapper activityMapper, ActivitySkuMapper activitySkuMapper,
                                ProductMapper productMapper, RedisTemplate<String, Object> redisTemplate,
                                RedissonClient redissonClient) {
        this.activityMapper = activityMapper;
        this.activitySkuMapper = activitySkuMapper;
        this.productMapper = productMapper;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.detailCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    // ============================== 预热（幂等） ==============================

    /**
     * 预热：活动须处于预热状态(1)。构建全局布隆 + 活动内 sku 布隆 + 活动/商品缓存 + 预库存分段 + 详情缓存。重复执行以最新为准。
     */
    public Map<String, Object> prepare(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_FOUND, "活动不存在");
        }
        if (activity.getStatus() != ActivityStatus.PREHEAT.code()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "活动未处于预热状态(1)");
        }
        List<ActivitySku> skus = skusOf(activityId);

        RBloomFilter<Object> bloomAll = redissonClient.getBloomFilter(CacheKeys.bloomAll());
        bloomAll.tryInit(bloomTotal, bloomFpp);
        bloomAll.add(activityId);

        RBloomFilter<Object> bloomAct = redissonClient.getBloomFilter(CacheKeys.bloomAct(activityId));
        bloomAct.tryInit(Math.max(bloomTotal, 100), bloomFpp);
        skus.forEach(sku -> bloomAct.add(sku.getSkuId()));

        redisTemplate.opsForValue().set(CacheKeys.actInfo(activityId), activity, ttlJitter(Duration.ofHours(1)));

        List<String> stockKeys = new ArrayList<>();
        int seg = Math.max(stockSegments, 1);
        for (ActivitySku sku : skus) {
            int per = sku.getSeckillStock() / seg;
            int remain = sku.getSeckillStock() - per * seg;
            for (int i = 0; i < seg; i++) {
                int value = per + (i < remain ? 1 : 0);
                String key = CacheKeys.stock(activityId, sku.getSkuId(), i);
                redisTemplate.opsForValue().set(key, value, ttlJitter(Duration.ofHours(2)));
                stockKeys.add(key);
            }
            redisTemplate.opsForValue().set(CacheKeys.actSku(activityId, sku.getSkuId()), sku,
                    ttlJitter(Duration.ofMinutes(30)));
        }
        redisTemplate.opsForValue().set(CacheKeys.actSkus(activityId), skus, ttlJitter(Duration.ofMinutes(30)));

        redisTemplate.delete(CacheKeys.actEmpty(activityId));
        rebuildDetail(activity, skus);
        log.info("preheat done: activityId={}, skus={}, stockKeys={}", activityId, skus.size(), stockKeys.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activityId", activityId);
        result.put("bloomAllKey", CacheKeys.bloomAll());
        result.put("bloomActKey", CacheKeys.bloomAct(activityId));
        result.put("stockKeys", stockKeys);
        result.put("status", activity.getStatus());
        return result;
    }

    // ============================== 详情读取 ==============================

    /**
     * 详情读取（多级缓存）。活动未预热时布隆未初始化 → 直接走 DB 兜底。
     */
    public ActivityDetailVO getDetail(Long activityId) {
        RBloomFilter<Object> bloomAll = redissonClient.getBloomFilter(CacheKeys.bloomAll());
        if (bloomAll.isExists() && !bloomAll.contains(activityId)) {
            // 穿透拦截：布隆不存在 ⇒ 一定不存在（布隆无假阴性）
            throw new BizException(ErrorCode.ACTIVITY_NOT_FOUND, "活动不存在");
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(CacheKeys.actEmpty(activityId)))) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_FOUND, "活动不存在");
        }
        ActivityDetailVO vo = detailCache.getIfPresent(CacheKeys.actDetail(activityId));
        if (vo != null) {
            log.info("activity detail cache hit(caffeine): {}", activityId);
            return vo;
        }
        Object cached = redisTemplate.opsForValue().get(CacheKeys.actDetail(activityId));
        if (cached instanceof ActivityDetailVO v) {
            log.info("activity detail cache hit(redis): {}", activityId);
            detailCache.put(CacheKeys.actDetail(activityId), v);
            return v;
        }
        return rebuildDetailSingleFlight(activityId);
    }

    /** 库存快照（Admin #11）：DB 配置库存 vs Redis 预库存分段合计 */
    public Map<String, Object> stockSnapshot(Long activityId) {
        List<ActivitySku> skus = skusOf(activityId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (ActivitySku sku : skus) {
            long redisStock = 0;
            for (int i = 0; i < Math.max(stockSegments, 1); i++) {
                Object v = redisTemplate.opsForValue().get(CacheKeys.stock(activityId, sku.getSkuId(), i));
                if (v instanceof Number n) {
                    redisStock += n.longValue();
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("skuId", sku.getSkuId());
            item.put("dbStock", sku.getSeckillStock());
            item.put("redisStock", redisStock);
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activityId", activityId);
        result.put("items", items);
        result.put("bloomAllReady", redissonClient.getBloomFilter(CacheKeys.bloomAll()).isExists());
        return result;
    }

    /** 脏缓存清理（后台变更：更新/改状态/改 sku 后调用），布隆不删除（幂等由预热重建） */
    public void invalidate(Long activityId) {
        List<String> keys = List.of(
                CacheKeys.actInfo(activityId),
                CacheKeys.actSkus(activityId),
                CacheKeys.actDetail(activityId),
                CacheKeys.actEmpty(activityId));
        redisTemplate.delete(keys);
        detailCache.invalidate(CacheKeys.actDetail(activityId));
        log.info("activity cache invalidated: {}", activityId);
    }

    // ============================== 内部实现 ==============================

    /** 击穿防护：互斥重建（单飞），拿不到锁短暂等待后降级直查 DB */
    private ActivityDetailVO rebuildDetailSingleFlight(Long activityId) {
        String lockKey = CacheKeys.lockRebuild(CacheKeys.actDetail(activityId));
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (locked) {
            try {
                return doRebuild(activityId);
            } finally {
                lock.unlock();
            }
        }
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            Object cached = redisTemplate.opsForValue().get(CacheKeys.actDetail(activityId));
            if (cached instanceof ActivityDetailVO v) {
                detailCache.put(CacheKeys.actDetail(activityId), v);
                return v;
            }
        }
        log.warn("rebuild lock wait timeout, degrade to DB: {}", activityId);
        return doRebuild(activityId);
    }

    private ActivityDetailVO doRebuild(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            redisTemplate.opsForValue().set(CacheKeys.actEmpty(activityId), "1", Duration.ofSeconds(30));
            throw new BizException(ErrorCode.ACTIVITY_NOT_FOUND, "活动不存在");
        }
        List<ActivitySku> skus = skusOf(activityId);
        return rebuildDetail(activity, skus);
    }

    private ActivityDetailVO rebuildDetail(Activity activity, List<ActivitySku> skus) {
        Map<Long, Product> products = productMap(skus);
        List<SkuVO> voList = new ArrayList<>();
        for (ActivitySku sku : skus) {
            SkuVO vo = new SkuVO();
            Product product = products.get(sku.getSkuId());
            vo.setSkuId(sku.getSkuId());
            vo.setTitle(product == null ? String.valueOf(sku.getSkuId()) : product.getTitle());
            vo.setImage(product == null ? "" : product.getImgUrl());
            vo.setSeckillPrice(sku.getSeckillPrice());
            vo.setOriginalPrice(sku.getOriginalPrice());
            vo.setStockLeft(sku.getSeckillStock());
            vo.setStockStatus(sku.getSeckillStock() > 0 ? 1 : 0);
            voList.add(vo);
        }
        ActivityDetailVO vo = ActivityDetailVO.of(activity, voList);
        redisTemplate.opsForValue().set(CacheKeys.actDetail(activity.getId()), vo,
                ttlJitter(Duration.ofSeconds(30)));
        detailCache.put(CacheKeys.actDetail(activity.getId()), vo);
        log.info("activity detail rebuilt: {}", activity.getId());
        return vo;
    }

    private List<ActivitySku> skusOf(Long activityId) {
        return activitySkuMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ActivitySku>()
                        .eq(ActivitySku::getActivityId, activityId));
    }

    private Map<Long, Product> productMap(List<ActivitySku> skus) {
        if (skus.isEmpty()) {
            return Map.of();
        }
        List<Long> skuIds = skus.stream().map(ActivitySku::getSkuId).distinct().collect(Collectors.toList());
        return productMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    /** TTL ±10% 随机抖动（防雪崩） */
    private Duration ttlJitter(Duration base) {
        long seconds = base.toSeconds();
        double factor = ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
        return Duration.ofSeconds(Math.max(1, seconds + (long) (seconds * factor)));
    }
}