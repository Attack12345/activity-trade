package com.example.activitytrade.order.service;

import com.example.activitytrade.activity.entity.Activity;
import com.example.activitytrade.activity.entity.ActivitySku;
import com.example.activitytrade.activity.entity.Product;
import com.example.activitytrade.activity.service.ActivityService;
import com.example.activitytrade.common.api.ErrorCode;
import com.example.activitytrade.common.exception.BizException;
import com.example.activitytrade.common.util.IdGenerator;
import com.example.activitytrade.order.entity.Order;
import com.example.activitytrade.order.entity.OrderItem;
import com.example.activitytrade.order.mapper.OrderItemMapper;
import com.example.activitytrade.order.mapper.OrderMapper;
import com.example.activitytrade.stock.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单写入（M6 同步简化版；M7 由事务消息替换调用方，本地事务内容固定）。
 * M6 边界：抢购下单同步完成建单 + DB 乐观锁扣减 + 占用日志。
 */
@Service
public class OrderWriteService {

    private static final Logger log = LoggerFactory.getLogger(OrderWriteService.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ActivityService activityService;
    private final StockService stockService;

    public OrderWriteService(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                             ActivityService activityService, StockService stockService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.activityService = activityService;
        this.stockService = stockService;
    }

    /**
     * 本地事务：DB 乐观锁扣减库存 → 插入订单+明细 → 记录库存占用。
     * DB 库存不足抛 3002；唯一约束冲突抛 2005（重复参与）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createSeckillOrder(Long userId, Activity activity, ActivitySku sku, Product product) {
        int rows = activityService.deductSeckillStock(sku.getId());
        if (rows == 0) {
            throw new BizException(ErrorCode.STOCK_DEDUCT_FAILED, "库存扣减失败");
        }
        String orderNo = IdGenerator.nextIdStr();
        String title = product == null ? "秒杀商品-" + sku.getSkuId() : product.getTitle();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setActivityId(activity.getId());
        order.setSkuId(sku.getSkuId());
        order.setSkuTitle(title);
        order.setPrice(sku.getSeckillPrice());
        order.setStatus(0);
        try {
            orderMapper.insert(order);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("duplicate seckill order: user={},act={},sku={}", userId, activity.getId(), sku.getSkuId());
            throw new BizException(ErrorCode.DUPLICATE_BUY, "重复参与");
        }

        OrderItem item = new OrderItem();
        item.setOrderNo(orderNo);
        item.setSkuId(sku.getSkuId());
        item.setTitle(title);
        item.setPrice(sku.getSeckillPrice());
        item.setQty(1);
        orderItemMapper.insert(item);

        stockService.logOccupied(orderNo, activity.getId(), sku.getSkuId());
        log.info("seckill order created: orderNo={},user={},act={},sku={}", orderNo, userId, activity.getId(), sku.getSkuId());
        return order;
    }
}