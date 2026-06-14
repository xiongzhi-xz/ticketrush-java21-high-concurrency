package com.ticketrush.interfaces.controller;

import com.ticketrush.application.command.ExecutorBenchmarkCommand;
import com.ticketrush.application.dto.ExecutorBenchmarkResult;
import com.ticketrush.application.service.ExecutorBenchmarkApplicationService;
import com.ticketrush.common.api.ApiResponse;
import com.ticketrush.interfaces.request.ExecutorBenchmarkRequest;
import com.ticketrush.interfaces.response.ExecutorBenchmarkResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 压测辅助接口。
 */
@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final ExecutorBenchmarkApplicationService executorBenchmarkApplicationService;

    public BenchmarkController(ExecutorBenchmarkApplicationService executorBenchmarkApplicationService) {
        this.executorBenchmarkApplicationService = executorBenchmarkApplicationService;
    }

    @PostMapping("/executors")
    public ApiResponse<ExecutorBenchmarkResponse> benchmarkExecutors(
            @Valid @RequestBody ExecutorBenchmarkRequest request
    ) {
        ExecutorBenchmarkResult result = executorBenchmarkApplicationService.run(new ExecutorBenchmarkCommand(
                request.mode(),
                request.taskCount(),
                request.blockingMillis(),
                request.cpuTokens(),
                request.timeoutSeconds()
        ));
        return ApiResponse.success(toResponse(result));
    }

    private ExecutorBenchmarkResponse toResponse(ExecutorBenchmarkResult result) {
        return new ExecutorBenchmarkResponse(
                result.mode(),
                result.taskCount(),
                result.blockingMillis(),
                result.cpuTokens(),
                result.elapsedMillis(),
                result.throughputPerSecond(),
                result.distinctThreadCount(),
                result.virtualThreadTaskCount(),
                result.sampleThreadName(),
                result.benchmarkAt()
        );
    }
}
