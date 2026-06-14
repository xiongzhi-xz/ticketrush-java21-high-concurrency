package com.ticketrush.application.dto;

import java.time.Instant;

/**
 * 执行器压测结果。
 */
public record ExecutorBenchmarkResult(
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
