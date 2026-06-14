package com.ticketrush.interfaces.controller;

import com.ticketrush.common.api.ApiResponse;
import com.ticketrush.config.VirtualThreadProperties;
import com.ticketrush.interfaces.request.ValidationCheckRequest;
import com.ticketrush.interfaces.response.HealthCheckResponse;
import com.ticketrush.interfaces.response.ValidationCheckResponse;
import jakarta.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 系统基础接口。
 *
 * <p>该控制器只放工程骨架验证能力，不承载票务业务逻辑。</p>
 */
@Validated
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final Environment environment;
    private final VirtualThreadProperties virtualThreadProperties;

    public SystemController(Environment environment, VirtualThreadProperties virtualThreadProperties) {
        this.environment = environment;
        this.virtualThreadProperties = virtualThreadProperties;
    }

    @GetMapping("/health")
    public ApiResponse<HealthCheckResponse> health() {
        HealthCheckResponse response = new HealthCheckResponse(
                environment.getProperty("spring.application.name", "ticketrush"),
                "UP",
                Runtime.version().toString(),
                virtualThreadProperties.enabled(),
                Thread.currentThread().isVirtual(),
                virtualThreadProperties.namePrefix(),
                Instant.now()
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/validation-check")
    public ApiResponse<ValidationCheckResponse> validationCheck(@Valid @RequestBody ValidationCheckRequest request) {
        ValidationCheckResponse response = new ValidationCheckResponse(
                request.requestId(),
                request.scenario(),
                request.concurrency(),
                true,
                Instant.now()
        );
        return ApiResponse.success(response);
    }
}
