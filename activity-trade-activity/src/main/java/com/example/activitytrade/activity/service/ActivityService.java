package com.example.activitytrade.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activitytrade.activity.cache.ActivityCacheService;
import com.example.activitytrade.activity.dto.ActivityCreateReq;
import com.example.activitytrade.activity.dto.ActivityDetailVO;
import com.example.activitytrade.activity.dto.ActivityUpdateReq;
import com.example.activitytrade.activity.dto.SkuUpsertReq;
import com.example.activitytrade.activity.dto.SkuVO;
import com.example.activitytrade.activity.entity.Activity;
import com.example.activitytrade.activity.entity.ActivitySku;
import com.example.activitytrade.activity.entity.ActivityStatus;
import com.example.activitytrade.activity.entity.Product;
import com.example.activitytrade.activity.mapper.ActivityMapper;
import com.example.activitytrade.activity.mapper.ActivitySkuMapper;
import com.example.activitytrade.activity.mapper.ProductMapper;
import com.example.activitytrade.common.api.ErrorCode;
import com.example.activitytrade.common.api.PageResult;
import com.example.activitytrade.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 活动/商品服务（M4）。
 * 注意：M4 使用秒杀库存作为可抢库存展示，真实已售统计 M6 接入。
 * M5：详情读取改走 {@link ActivityCacheService}（多级缓存）。
 */
@Service
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final ActivitySkuMapper activitySkuMapper;
    private final ProductMapper productMapper;
    private final ActivityCacheService activityCacheService;

    public ActivityService(ActivityMapper activityMapper, ActivitySkuMapper activitySkuMapper,
                           ProductMapper productMapper, ActivityCacheService activityCacheService) {
        this.activityMapper = activityMapper;
        this.activitySkuMapper = activitySkuMapper;
        this.productMapper = productMapper;
        this.activityCacheService = activityCacheService;
    }

    /** 用户端列表：仅显示预热(1)/进行中(2)，按开始时间升序 */
    public PageResult<Activity> listForUser(long page, long size) {
        Page<Activity> p = activityMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Activity>()
                        .in(Activity::getStatus, ActivityStatus.PREHEAT.code(), ActivityStatus.ONGOING.code())
                        .orderByAsc(Activity::getStartTime));
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 管理端列表：全状态，可按状态过滤 */
    public PageResult<Activity> listForAdmin(long page, long size, Integer status) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .eq(status != null, Activity::getStatus, status)
                .orderByDesc(Activity::getCreatedAt);
        Page<Activity> p = activityMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    public ActivityDetailVO detail(Long activityId) {
        return activityCacheService.getDetail(activityId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Activity create(ActivityCreateReq req) {
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        Activity activity = new Activity();
        activity.setName(req.getName().trim());
        activity.setStartTime(req.getStartTime());
        activity.setEndTime(req.getEndTime());
        activity.setStatus(ActivityStatus.DRAFT.code());
        activity.setVersion(0);
        activityMapper.insert(activity);
        return activity;
    }

    @Transactional(rollbackFor = Exception.class)
    public Activity update(Long activityId, ActivityUpdateReq req) {
        Activity activity = requireActivity(activityId);
        if (req.getName() != null && !req.getName().isBlank()) {
            activity.setName(req.getName().trim());
        }
        if (req.getStartTime() != null) {
            activity.setStartTime(req.getStartTime());
        }
        if (req.getEndTime() != null) {
            activity.setEndTime(req.getEndTime());
        }
        if (!activity.getEndTime().isAfter(activity.getStartTime())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        activityMapper.updateById(activity);
        activityCacheService.invalidate(activityId);
        return activity;
    }

    @Transactional(rollbackFor = Exception.class)
    public Activity changeStatus(Long activityId, Integer newStatus) {
        Activity activity = requireActivity(activityId);
        ActivityStatus source = ActivityStatus.of(activity.getStatus());
        ActivityStatus target = source.transitTo(newStatus);
        activityMapper.update(null,
                new LambdaUpdateWrapper<Activity>()
                        .eq(Activity::getId, activityId)
                        .set(Activity::getStatus, target.code()));
        activity.setStatus(target.code());
        activityCacheService.invalidate(activityId);
        return activity;
    }

    /** 批量 upsert 活动商品（activity_id + sku_id 唯一，重复则更新价格/库存） */
    @Transactional(rollbackFor = Exception.class)
    public int saveSkus(Long activityId, List<SkuUpsertReq> items) {
        requireActivity(activityId);
        int count = 0;
        for (SkuUpsertReq item : items) {
            ActivitySku exist = activitySkuMapper.selectOne(
                    new LambdaQueryWrapper<ActivitySku>()
                            .eq(ActivitySku::getActivityId, activityId)
                            .eq(ActivitySku::getSkuId, item.getSkuId()));
            if (exist == null) {
                ActivitySku sku = new ActivitySku();
                sku.setActivityId(activityId);
                sku.setSkuId(item.getSkuId());
                sku.setSeckillPrice(item.getSeckillPrice());
                sku.setOriginalPrice(item.getOriginalPrice() == null ? item.getSeckillPrice() : item.getOriginalPrice());
                sku.setSeckillStock(item.getSeckillStock());
                sku.setVersion(0);
                activitySkuMapper.insert(sku);
            } else {
                activitySkuMapper.update(null,
                        new LambdaUpdateWrapper<ActivitySku>()
                                .eq(ActivitySku::getId, exist.getId())
                                .set(ActivitySku::getSeckillPrice, item.getSeckillPrice())
                                .set(ActivitySku::getOriginalPrice, item.getOriginalPrice() == null
                                        ? item.getSeckillPrice() : item.getOriginalPrice())
                                .set(ActivitySku::getSeckillStock, item.getSeckillStock()));
            }
            count++;
        }
        activityCacheService.invalidate(activityId);
        return count;
    }

    private Activity requireActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_FOUND, "活动不存在");
        }
        return activity;
    }

    private Map<Long, Product> productMap(List<ActivitySku> skus) {
        if (skus.isEmpty()) {
            return Map.of();
        }
        List<Long> skuIds = skus.stream().map(ActivitySku::getSkuId).distinct().collect(Collectors.toList());
        return productMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}