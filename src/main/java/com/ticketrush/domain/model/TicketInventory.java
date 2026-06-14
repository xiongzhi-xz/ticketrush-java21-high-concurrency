package com.ticketrush.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 票档库存。
 *
 * <p>库存状态必须满足：总库存 = 可售库存 + 锁定库存 + 已售库存。
 * Redis Lua、Redis 锁和 MySQL 乐观锁都围绕这个模型做一致性保护。</p>
 */
public record TicketInventory(
        Long skuId,
        Integer totalStock,
        Integer availableStock,
        Integer lockedStock,
        Integer soldStock,
        Long version,
        LocalDateTime updatedAt
) {

    public TicketInventory {
        Objects.requireNonNull(skuId, "票档 ID 不能为空");
        Objects.requireNonNull(totalStock, "总库存不能为空");
        Objects.requireNonNull(availableStock, "可售库存不能为空");
        Objects.requireNonNull(lockedStock, "锁定库存不能为空");
        Objects.requireNonNull(soldStock, "已售库存不能为空");
        Objects.requireNonNull(version, "库存版本号不能为空");
        if (totalStock < 0 || availableStock < 0 || lockedStock < 0 || soldStock < 0) {
            throw new IllegalArgumentException("库存数量不能小于 0");
        }
        if (totalStock != availableStock + lockedStock + soldStock) {
            throw new IllegalArgumentException("库存数量不守恒");
        }
    }

    public boolean hasEnoughAvailable(int quantity) {
        return quantity > 0 && availableStock >= quantity;
    }

    public TicketInventory reserve(int quantity, LocalDateTime now) {
        validateQuantity(quantity);
        if (!hasEnoughAvailable(quantity)) {
            throw new IllegalArgumentException("可售库存不足");
        }
        return new TicketInventory(
                skuId,
                totalStock,
                availableStock - quantity,
                lockedStock + quantity,
                soldStock,
                version + 1,
                now
        );
    }

    public TicketInventory confirm(int quantity, LocalDateTime now) {
        validateQuantity(quantity);
        if (lockedStock < quantity) {
            throw new IllegalArgumentException("锁定库存不足");
        }
        return new TicketInventory(
                skuId,
                totalStock,
                availableStock,
                lockedStock - quantity,
                soldStock + quantity,
                version + 1,
                now
        );
    }

    public TicketInventory release(int quantity, LocalDateTime now) {
        validateQuantity(quantity);
        if (lockedStock < quantity) {
            throw new IllegalArgumentException("锁定库存不足");
        }
        return new TicketInventory(
                skuId,
                totalStock,
                availableStock + quantity,
                lockedStock - quantity,
                soldStock,
                version + 1,
                now
        );
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("库存操作数量必须大于 0");
        }
    }
}
