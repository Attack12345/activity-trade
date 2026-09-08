package com.example.activitytrade.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activitytrade.pay.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付 Mapper（M8）。
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}