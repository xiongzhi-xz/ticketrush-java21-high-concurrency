package com.ticketrush.application.dto;

import java.time.Instant;

/**
 * 订单超时关闭结果。
 */
public record CloseExpiredOrdersResult(
        Integer scannedCount,
        Integer closedCount,
        Integer releasedStockCount,
        Integer skippedCount,
        Integer failedCount,
        Instant closedAt
) {
}
