package com.ticketrush.common.exception;

import com.ticketrush.common.api.ErrorCode;

/**
 * 业务异常。
 *
 * <p>库存不足、幂等冲突、限流降级等可预期失败都应抛出该异常，
 * 由全局异常处理器转换成稳定的接口响应。</p>
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
