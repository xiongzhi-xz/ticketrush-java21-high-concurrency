package com.ticketrush.common.api;

import org.springframework.http.HttpStatus;

/**
 * 全局错误码。
 *
 * <p>错误码按场景分段，方便后续在压测报告和监控面板中区分参数错误、
 * 并发控制失败、库存不足、限流降级等问题。</p>
 */
public enum ErrorCode {

    SUCCESS("00000", "成功", HttpStatus.OK),

    PARAM_INVALID("A0400", "请求参数不合法", HttpStatus.BAD_REQUEST),
    REQUEST_BODY_INVALID("A0401", "请求体格式不合法", HttpStatus.BAD_REQUEST),
    IDEMPOTENT_CONFLICT("A0429", "重复请求，请勿频繁提交", HttpStatus.TOO_MANY_REQUESTS),

    UNAUTHORIZED("A0301", "用户未登录", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("A0303", "用户无权限", HttpStatus.FORBIDDEN),

    STOCK_NOT_ENOUGH("B0401", "库存不足", HttpStatus.CONFLICT),
    STOCK_DEDUCT_FAILED("B0402", "库存扣减失败", HttpStatus.CONFLICT),
    ORDER_CREATE_FAILED("B0501", "订单创建失败", HttpStatus.INTERNAL_SERVER_ERROR),

    RATE_LIMITED("C0429", "请求过于频繁，请稍后再试", HttpStatus.TOO_MANY_REQUESTS),
    SERVICE_DEGRADED("C0503", "服务繁忙，请稍后再试", HttpStatus.SERVICE_UNAVAILABLE),

    INTERNAL_ERROR("B0001", "系统内部异常", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
