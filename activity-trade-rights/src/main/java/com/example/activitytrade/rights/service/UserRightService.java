package com.example.activitytrade.rights.service;

import com.example.activitytrade.common.util.IdGenerator;
import com.example.activitytrade.rights.entity.UserRight;
import com.example.activitytrade.rights.mapper.UserRightMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 权益服务（M8）：发券（幂等，right_no 唯一）。
 */
@Service
public class UserRightService {

    private static final Logger log = LoggerFactory.getLogger(UserRightService.class);

    private final UserRightMapper userRightMapper;

    public UserRightService(UserRightMapper userRightMapper) {
        this.userRightMapper = userRightMapper;
    }

    /** 发放权益；重复发放（right_no 唯一冲突）幂等跳过 */
    public void issue(String orderNo, Long userId, Long activityId, Integer rightType) {
        UserRight right = new UserRight();
        right.setRightNo(IdGenerator.nextIdStr());
        right.setUserId(userId);
        right.setActivityId(activityId);
        right.setOrderNo(orderNo);
        right.setRightType(rightType == null ? 1 : rightType);
        right.setStatus(1);
        try {
            userRightMapper.insert(right);
            log.info("right issued: orderNo={}, userId={}, rightNo={}", orderNo, userId, right.getRightNo());
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.info("right already issued, skip: orderNo={}", orderNo);
        }
    }
}