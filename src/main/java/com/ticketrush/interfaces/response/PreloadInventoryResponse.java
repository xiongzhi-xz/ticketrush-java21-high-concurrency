package com.ticketrush.interfaces.response;

import java.time.Instant;

/**
 * 库存预热响应。
 */
public record PreloadInventoryResponse(
        Long skuId,
        Integer totalStock,
        Integer availableStock,
        Integer lockedStock,
        Integer soldStock,
        Long version,
        Instant preloadedAt
) {
}
