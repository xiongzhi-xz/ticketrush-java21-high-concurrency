package com.ticketrush.interfaces.response;

import java.time.Instant;

/**
 * 参数校验示例响应。
 */
public record ValidationCheckResponse(
        String requestId,
        String scenario,
        int concurrency,
        boolean accepted,
        Instant checkedAt
) {
}
