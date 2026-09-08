package com.example.activitytrade.activity.cache;

/**
 * Redis Key 命名（对应 docs/development-guide.md §4.3，含 M5 扩展键）。
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /** 活动信息 JSON */
    public static String actInfo(Long activityId) {
        return "act:info:" + activityId;
    }

    /** 活动商品 JSON（单条） */
    public static String actSku(Long activityId, Long skuId) {
        return "act:sku:" + activityId + ":" + skuId;
    }

    /** 活动商品集合 JSON（活动详情读取用） */
    public static String actSkus(Long activityId) {
        return "act:skus:" + activityId;
    }

    /** 活动详情 VO JSON（含商品信息与库存快照，TTL 较短） */
    public static String actDetail(Long activityId) {
        return "act:detail:" + activityId;
    }

    /** 空值缓存 key（穿透兜底，TTL 短） */
    public static String actEmpty(Long activityId) {
        return "act:empty:" + activityId;
    }

    /** 全局活动 id 布隆过滤器名 */
    public static String bloomAll() {
        return "bm:act:all";
    }

    /** 活动内 skuId 布隆过滤器名 */
    public static String bloomAct(Long activityId) {
        return "bm:act:" + activityId;
    }

    /** 预库存分段 key（M6 抢购减库存用） */
    public static String stock(Long activityId, Long skuId, int segment) {
        return "stock:" + activityId + ":" + skuId + ":" + segment;
    }

    /** 缓存重建互斥锁（击穿单飞） */
    public static String lockRebuild(String key) {
        return "lock:rebuild:" + key;
    }
}