package com.ticketrush.application.command;

import com.ticketrush.domain.model.InventoryDeductionStrategy;

/**
 * 抢票应用命令。
 */
public record RushTicketCommand(
        String requestId,
        Long userId,
        Long eventId,
        Long skuId,
        Integer quantity,
        InventoryDeductionStrategy strategy,
        String idempotentKey
) {
}
