package com.ticketrush.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 抢票请求。
 */
public record RushTicketRequest(
        @NotBlank(message = "请求编号不能为空")
        String requestId,

        @NotNull(message = "用户 ID 不能为空")
        Long userId,

        @NotNull(message = "活动 ID 不能为空")
        Long eventId,

        @NotNull(message = "票档 ID 不能为空")
        Long skuId,

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量不能小于 1")
        @Max(value = 10, message = "单次最多购买 10 张")
        Integer quantity,

        String idempotentKey
) {
}
