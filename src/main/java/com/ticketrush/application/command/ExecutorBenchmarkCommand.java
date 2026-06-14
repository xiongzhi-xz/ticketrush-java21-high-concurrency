package com.ticketrush.application.command;

import com.ticketrush.application.dto.ExecutorBenchmarkMode;

/**
 * 执行器压测命令。
 */
public record ExecutorBenchmarkCommand(
        ExecutorBenchmarkMode mode,
        Integer taskCount,
        Integer blockingMillis,
        Integer cpuTokens,
        Integer timeoutSeconds
) {
}
