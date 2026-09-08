package com.example.activitytrade.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activitytrade.stock.entity.StockDeductLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存扣减日志 Mapper（M6）。
 */
@Mapper
public interface StockDeductLogMapper extends BaseMapper<StockDeductLog> {
}