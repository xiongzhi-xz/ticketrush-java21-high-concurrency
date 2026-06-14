package com.ticketrush.interfaces.response;

import com.ticketrush.application.dto.ExecutorBenchmarkMode;

import java.time.Instant;

/**
 * 执行器压测响应。
 */
public record ExecutorBenchmarkResponse(
        ExecutorBenchmarkMode mode,
        Integer taskCount,
        Integer blockingMillis,
        Integer cpuTokens,
        Long elapsedMillis,
        Double throughputPerSecond,
        Integer distinctThreadCount,
        Integer virtualThreadTaskCount,
        String sampleThreadName,
        Instant benchmarkAt
) {
}
