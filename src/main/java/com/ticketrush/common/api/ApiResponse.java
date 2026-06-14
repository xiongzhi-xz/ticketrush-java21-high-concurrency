package com.ticketrush.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * HTTP 接口统一响应体。
 *
 * <p>统一响应格式能让抢票、下单、库存等接口在高并发压测时更容易统计成功率、
 * 失败原因和错误码分布。</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data, Instant.now());
    }

    public static ApiResponse<Void> ok() {
        return success(null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode.code(), errorCode.message());
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return failure(errorCode.code(), message);
    }

    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null, Instant.now());
    }
}
