package com.ticketrush.application.service;

import com.ticketrush.application.command.ExecutorBenchmarkCommand;
import com.ticketrush.application.dto.ExecutorBenchmarkMode;
import com.ticketrush.application.dto.ExecutorBenchmarkResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutorBenchmarkApplicationServiceTest {

    @Test
    void shouldRunBenchmarkWithVirtualThreads() {
        ExecutorService virtualExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("benchmark-vt-", 0).factory()
        );
        ExecutorService traditionalExecutor = Executors.newFixedThreadPool(2);
        try {
            ExecutorBenchmarkApplicationService service = new ExecutorBenchmarkApplicationService(
                    virtualExecutor,
                    traditionalExecutor
            );

            ExecutorBenchmarkResult result = service.run(new ExecutorBenchmarkCommand(
                    ExecutorBenchmarkMode.VIRTUAL_THREAD,
                    20,
                    1,
                    1,
                    5
            ));

            assertThat(result.mode()).isEqualTo(ExecutorBenchmarkMode.VIRTUAL_THREAD);
            assertThat(result.taskCount()).isEqualTo(20);
            assertThat(result.virtualThreadTaskCount()).isEqualTo(20);
            assertThat(result.throughputPerSecond()).isPositive();
        } finally {
            virtualExecutor.shutdownNow();
            traditionalExecutor.shutdownNow();
        }
    }

    @Test
    void shouldRunBenchmarkWithTraditionalThreadPool() {
        ExecutorService virtualExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("benchmark-vt-", 0).factory()
        );
        ExecutorService traditionalExecutor = Executors.newFixedThreadPool(
                2,
                Thread.ofPlatform().name("benchmark-pt-", 0).factory()
        );
        try {
            ExecutorBenchmarkApplicationService service = new ExecutorBenchmarkApplicationService(
                    virtualExecutor,
                    traditionalExecutor
            );

            ExecutorBenchmarkResult result = service.run(new ExecutorBenchmarkCommand(
                    ExecutorBenchmarkMode.TRADITIONAL_THREAD_POOL,
                    20,
                    1,
                    1,
                    5
            ));

            assertThat(result.mode()).isEqualTo(ExecutorBenchmarkMode.TRADITIONAL_THREAD_POOL);
            assertThat(result.taskCount()).isEqualTo(20);
            assertThat(result.virtualThreadTaskCount()).isZero();
            assertThat(result.distinctThreadCount()).isLessThanOrEqualTo(2);
        } finally {
            virtualExecutor.shutdownNow();
            traditionalExecutor.shutdownNow();
        }
    }
}
