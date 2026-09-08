package com.example.activitytrade.activity.entity;

import com.example.activitytrade.common.exception.BizException;
import com.example.activitytrade.common.api.ErrorCode;

/**
 * 活动状态机：0草稿 → 1预热 → 2进行中 → 3结束（严格相邻推进）。
 */
public enum ActivityStatus {

    DRAFT(0),
    PREHEAT(1),
    ONGOING(2),
    ENDED(3);

    private final int code;

    ActivityStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static ActivityStatus of(int code) {
        for (ActivityStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new BizException(ErrorCode.PARAM_ERROR, "非法活动状态: " + code);
    }

    /** 校验并返回新状态：仅允许相邻推进（如 0→1），否则抛 4001 */
    public ActivityStatus transitTo(int newCode) {
        ActivityStatus target = of(newCode);
        if (target.code != this.code + 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "活动状态跳转非法");
        }
        return target;
    }
}