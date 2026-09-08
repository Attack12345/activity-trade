package com.example.activitytrade.common.mq;

/**
 * MQ Topic/Tag 常量（docs/development-guide.md §4.3）。
 */
public final class MqTopic {

    private MqTopic() {
    }

    /** 下单（事务消息） */
    public static final String ORDER_CREATE = "TRADE_ORDER_CREATE";

    /** 关单（延迟消息） */
    public static final String ORDER_CLOSE_DELAY = "TRADE_ORDER_CLOSE_DELAY";

    /** 支付成功 */
    public static final String PAY_RESULT = "TRADE_PAY_RESULT";

    /** 权益发放 */
    public static final String RIGHTS_ISSUE = "TRADE_RIGHTS_ISSUE";

    /** 统一 Tag */
    public static final String TAG_MAIN = "TAG_MAIN";
}