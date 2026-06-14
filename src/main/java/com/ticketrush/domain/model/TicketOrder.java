package com.ticketrush.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 票务订单。
 *
 * <p>抢票入口只负责库存预占和消息入队，订单最终创建由 RocketMQ 消费端完成。
 * 幂等键用于防止用户重复请求或消息重复消费导致重复下单。</p>
 */
public record TicketOrder(
        Long id,
        String orderNo,
        Long userId,
        Long eventId,
        Long skuId,
        Integer quantity,
        Long amountFen,
        InventoryDeductionStrategy inventoryDeductionStrategy,
        OrderStatus status,
        String idempotentKey,
        LocalDateTime createdAt,
        LocalDateTime expireAt,
        LocalDateTime paidAt,
        LocalDateTime canceledAt
) {

    public TicketOrder {
        Objects.requireNonNull(orderNo, "订单号不能为空");
        Objects.requireNonNull(userId, "用户 ID 不能为空");
        Objects.requireNonNull(eventId, "活动 ID 不能为空");
        Objects.requireNonNull(skuId, "票档 ID 不能为空");
        Objects.requireNonNull(quantity, "购买数量不能为空");
        Objects.requireNonNull(amountFen, "订单金额不能为空");
        Objects.requireNonNull(inventoryDeductionStrategy, "库存扣减策略不能为空");
        Objects.requireNonNull(status, "订单状态不能为空");
        Objects.requireNonNull(idempotentKey, "幂等键不能为空");
        Objects.requireNonNull(createdAt, "订单创建时间不能为空");
        Objects.requireNonNull(expireAt, "订单过期时间不能为空");
        if (quantity <= 0) {
            throw new IllegalArgumentException("购买数量必须大于 0");
        }
        if (amountFen < 0) {
            throw new IllegalArgumentException("订单金额不能小于 0");
        }
        if (!expireAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("订单过期时间必须晚于创建时间");
        }
    }

    public boolean isExpired(LocalDateTime now) {
        return status.canClose() && !now.isBefore(expireAt);
    }
}
