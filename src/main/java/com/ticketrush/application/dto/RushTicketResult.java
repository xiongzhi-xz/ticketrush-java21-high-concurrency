package com.ticketrush.application.dto;

import com.ticketrush.domain.model.InventoryDeductionStrategy;

import java.time.Instant;

/**
 * 抢票结果。
 */
public record RushTicketResult(
        boolean accepted,
        String requestId,
        Long userId,
        Long eventId,
        Long skuId,
        Integer quantity,
        InventoryDeductionStrategy strategy,
        Integer remainingStock,
        String idempotentKey,
        String message,
        String processedThreadName,
        boolean processedByVirtualThread,
        Instant acceptedAt
) {
}
