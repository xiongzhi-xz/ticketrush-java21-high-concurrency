package com.ticketrush.domain.model;

import java.util.Objects;

/**
 * 库存扣减命令。
 */
public record InventoryDeductionCommand(
        String requestId,
        Long userId,
        Long eventId,
        Long skuId,
        Integer quantity,
        InventoryDeductionStrategy strategy,
        String idempotentKey
) {

    public InventoryDeductionCommand {
        Objects.requireNonNull(requestId, "请求编号不能为空");
        Objects.requireNonNull(userId, "用户 ID 不能为空");
        Objects.requireNonNull(eventId, "活动 ID 不能为空");
        Objects.requireNonNull(skuId, "票档 ID 不能为空");
        Objects.requireNonNull(quantity, "扣减数量不能为空");
        Objects.requireNonNull(strategy, "库存扣减策略不能为空");
        Objects.requireNonNull(idempotentKey, "幂等键不能为空");
        if (quantity <= 0) {
            throw new IllegalArgumentException("扣减数量必须大于 0");
        }
    }
}
