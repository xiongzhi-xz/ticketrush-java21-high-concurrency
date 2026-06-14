package com.ticketrush.application.dto;

import java.time.Instant;

/**
 * 库存预热结果。
 */
public record PreloadInventoryResult(
        Long skuId,
        Integer totalStock,
        Integer availableStock,
        Integer lockedStock,
        Integer soldStock,
        Long version,
        Instant preloadedAt
) {
}
