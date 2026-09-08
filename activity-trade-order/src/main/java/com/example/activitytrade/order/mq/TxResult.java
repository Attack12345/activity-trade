package com.example.activitytrade.order.mq;

/**
 * 事务消息提交结果（本地进程内传递，供发送方读取）。
 */
public class TxResult {

    /** true=本地事务已提交 */
    private boolean commit;
    /** 本地事务失败时的业务错误码 */
    private Integer errorCode;
    /** 订单号 */
    private String orderNo;

    public boolean isCommit() {
        return commit;
    }

    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
}