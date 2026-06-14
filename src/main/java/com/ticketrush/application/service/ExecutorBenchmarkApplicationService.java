package com.ticketrush.application.service;

import com.ticketrush.application.command.ExecutorBenchmarkCommand;
import com.ticketrush.application.dto.ExecutorBenchmarkMode;
import com.ticketrush.application.dto.ExecutorBenchmarkResult;
import com.ticketrush.common.api.ErrorCode;
import com.ticketrush.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 执行器压测应用服务。
 *
 * <p>该服务用于构造相同的 I/O 等待任务，分别交给虚拟线程执行器和传统固定线程池执行。
 * 后续压测报告会基于该入口观察吞吐、耗时和线程数量差异。</p>
 */
@Service
public class ExecutorBenchmarkApplicationService {

    private final ExecutorService virtualThreadExecutor;
    private final ExecutorService traditionalExecutor;

    public ExecutorBenchmarkApplicationService(
            @Qualifier("ticketRushVirtualThreadExecutor") ExecutorService virtualThreadExecutor,
            @Qualifier("ticketRushTraditionalExecutor") ExecutorService traditionalExecutor
    ) {
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.traditionalExecutor = traditionalExecutor;
    }

    public ExecutorBenchmarkResult run(ExecutorBenchmarkCommand command) {
        ExecutorBenchmarkMode mode = normalizeMode(command.mode());
        ExecutorService executor = selectExecutor(mode);
        int taskCount = command.taskCount();
        int blockingMillis = command.blockingMillis();
        int cpuTokens = command.cpuTokens();
        int timeoutSeconds = command.timeoutSeconds();

        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        AtomicInteger virtualThreadTaskCount = new AtomicInteger();
        long startedAt = System.nanoTime();

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>(taskCount);
            for (int index = 0; index < taskCount; index++) {
                futures.add(CompletableFuture.runAsync(
                        () -> executeBenchmarkTask(blockingMillis, cpuTokens, threadNames, virtualThreadTaskCount),
                        executor
                ));
            }

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .join();
        } catch (CompletionException exception) {
            throw new BusinessException(ErrorCode.SERVICE_DEGRADED, "执行器压测超时或执行失败");
        }

        long elapsedMillis = Math.max(1, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        double throughput = taskCount * 1000.0 / elapsedMillis;

        return new ExecutorBenchmarkResult(
                mode,
                taskCount,
                blockingMillis,
                cpuTokens,
                elapsedMillis,
                throughput,
                threadNames.size(),
                virtualThreadTaskCount.get(),
                threadNames.stream().findFirst().orElse("unknown"),
                Instant.now()
        );
    }

    private ExecutorBenchmarkMode normalizeMode(ExecutorBenchmarkMode mode) {
        if (mode == null) {
            return ExecutorBenchmarkMode.VIRTUAL_THREAD;
        }
        return mode;
    }

    private ExecutorService selectExecutor(ExecutorBenchmarkMode mode) {
        return switch (mode) {
            case VIRTUAL_THREAD -> virtualThreadExecutor;
            case TRADITIONAL_THREAD_POOL -> traditionalExecutor;
        };
    }

    private void executeBenchmarkTask(
            int blockingMillis,
            int cpuTokens,
            Set<String> threadNames,
            AtomicInteger virtualThreadTaskCount
    ) {
        Thread currentThread = Thread.currentThread();
        threadNames.add(currentThread.getName());
        if (currentThread.isVirtual()) {
            virtualThreadTaskCount.incrementAndGet();
        }
        sleep(blockingMillis);
        consumeCpu(cpuTokens);
    }

    private void sleep(int blockingMillis) {
        if (blockingMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(blockingMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SERVICE_DEGRADED, "执行器压测任务被中断");
        }
    }

    private void consumeCpu(int cpuTokens) {
        long value = 0;
        for (int index = 0; index < cpuTokens * 1000; index++) {
            value += index % 7;
        }
        if (value == Long.MIN_VALUE) {
            throw new IllegalStateException("unreachable");
        }
    }
}
