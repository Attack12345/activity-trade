package com.example.activitytrade.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activitytrade.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper（M6）。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}