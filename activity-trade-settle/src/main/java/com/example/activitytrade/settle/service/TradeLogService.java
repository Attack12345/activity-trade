package com.example.activitytrade.settle.service;

import com.example.activitytrade.settle.entity.TradeLog;
import com.example.activitytrade.settle.mapper.TradeLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 业务消息日志服务（M7）：消费幂等落库，biz_no 唯一（uq_biz）。
 */
@Service
public class TradeLogService {

    private static final Logger log = LoggerFactory.getLogger(TradeLogService.class);

    private final TradeLogMapper tradeLogMapper;

    public TradeLogService(TradeLogMapper tradeLogMapper) {
        this.tradeLogMapper = tradeLogMapper;
    }

    /** 记录消息处理痕迹；重复记录（biz_no 冲突）直接忽略（幂等） */
    public void record(String bizNo, String topic, String payload) {
        try {
            TradeLog entry = new TradeLog();
            entry.setBizNo(bizNo);
            entry.setTopic(topic);
            entry.setPayload(payload);
            entry.setStatus(0);
            entry.setRetry(0);
            tradeLogMapper.insert(entry);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.debug("trade_log already recorded, skip: bizNo={}, topic={}", bizNo, topic);
        }
    }

    /** 死信/失败标记（status=1），供 M9 对账补偿 */
    public void markDead(String bizNo) {
        tradeLogMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TradeLog>()
                        .eq(TradeLog::getBizNo, bizNo)
                        .set(TradeLog::getStatus, 1));
    }
}