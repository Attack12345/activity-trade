package com.example.activitytrade.common.exception;

import java.io.Serial;

/**
 * 业务异常。由全局异常处理器转为对应错误码返回，调用方不自行 catch。
 */
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
