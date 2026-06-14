package com.ticketrush.interfaces.request;

import com.ticketrush.application.dto.ExecutorBenchmarkMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 执行器压测请求。
 */
public record ExecutorBenchmarkRequest(
        ExecutorBenchmarkMode mode,

        @NotNull(message = "任务数量不能为空")
        @Min(value = 1, message = "任务数量不能小于 1")
        @Max(value = 50000, message = "单次任务数量不能超过 50000")
        Integer taskCount,

        @NotNull(message = "阻塞毫秒数不能为空")
        @Min(value = 0, message = "阻塞毫秒数不能小于 0")
        @Max(value = 10000, message = "阻塞毫秒数不能超过 10000")
        Integer blockingMillis,

        @NotNull(message = "CPU token 不能为空")
        @Min(value = 0, message = "CPU token 不能小于 0")
        @Max(value = 10000, message = "CPU token 不能超过 10000")
        Integer cpuTokens,

        @NotNull(message = "超时秒数不能为空")
        @Min(value = 1, message = "超时秒数不能小于 1")
        @Max(value = 300, message = "超时秒数不能超过 300")
        Integer timeoutSeconds
) {
}
