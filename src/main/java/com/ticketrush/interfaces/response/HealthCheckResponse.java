package com.ticketrush.interfaces.response;

import java.time.Instant;

/**
 * 系统健康检查响应。
 */
public record HealthCheckResponse(
        String application,
        String status,
        String javaVersion,
        boolean virtualThreadsEnabled,
        boolean currentThreadVirtual,
        String virtualThreadNamePrefix,
        Instant checkedAt
) {
}
