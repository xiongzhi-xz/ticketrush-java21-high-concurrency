package com.ticketrush.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 库存预热请求。
 */
public record PreloadInventoryRequest(
        @NotNull(message = "票档 ID 不能为空")
        Long skuId,

        @NotNull(message = "总库存不能为空")
        @Min(value = 1, message = "总库存不能小于 1")
        @Max(value = 1000000, message = "本地预热库存不能超过 1000000")
        Integer totalStock
) {
}
