package com.ticketrush.application.dto;

import com.ticketrush.domain.model.InventoryDeductionStrategy;

import java.time.Instant;

/**
 * 订单创建消息。
 *
 * <p>抢票入口只负责库存预占和消息发送，订单落库由 RocketMQ 消费端异步完成。</p>
 */
public record OrderCreateMessage(
        String requestId,
        Long userId,
        Long eventId,
        Long skuId,
        Integer quantity,
        InventoryDeductionStrategy strategy,
        String idempotentKey,
        Instant reservedAt
) {
}
