package com.example.activitytrade.common.api;

/**
 * 业务错误码。与 docs/development-guide.md §4.1 保持一致。
 */
public final class ErrorCode {

    private ErrorCode() {
    }

    public static final int SUCCESS = 0;
    public static final String SUCCESS_MSG = "ok";

    /** 参数缺失/格式错误 */
    public static final int PARAM_ERROR = 4001;
    /** 未登录或 token 失效 */
    public static final int UNAUTHORIZED = 401;
    /** 无权限（非 admin） */
    public static final int FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    /** 系统异常（兜底） */
    public static final int SYSTEM_ERROR = 1000;

    /** 活动不存在 */
    public static final int ACTIVITY_NOT_FOUND = 2001;
    /** 活动未开始或已结束 */
    public static final int ACTIVITY_NOT_OPEN = 2002;
    /** 活动未预热 */
    public static final int ACTIVITY_NOT_PREHEAT = 2003;
    /** 已售罄 */
    public static final int SOLD_OUT = 2004;
    /** 重复参与（该商品已下过单） */
    public static final int DUPLICATE_BUY = 2005;
    /** 请求被限流/频率超限 */
    public static final int RATE_LIMITED = 2006;

    /** 下单失败（通用） */
    public static final int ORDER_CREATE_FAILED = 3001;
    /** 库存扣减失败 */
    public static final int STOCK_DEDUCT_FAILED = 3002;

    /** 支付校验失败 */
    public static final int PAY_VERIFY_FAILED = 4002;

    /** 权益发放失败 */
    public static final int RIGHTS_ISSUE_FAILED = 5001;

    /** 对账发现数据不一致 */
    public static final int SETTLE_MISMATCH = 6001;
}
