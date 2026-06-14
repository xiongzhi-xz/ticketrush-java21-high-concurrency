package com.ticketrush.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 参数校验示例请求。
 *
 * <p>该请求用于验证统一参数校验响应格式，后续业务接口沿用同样的校验方式。</p>
 */
public record ValidationCheckRequest(
        @NotBlank(message = "请求编号不能为空")
        String requestId,

        @NotBlank(message = "压测场景不能为空")
        String scenario,

        @NotNull(message = "并发数不能为空")
        @Min(value = 1, message = "并发数不能小于 1")
        @Max(value = 100000, message = "并发数不能超过 100000")
        Integer concurrency
) {
}
