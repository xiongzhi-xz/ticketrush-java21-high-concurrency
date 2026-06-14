package com.ticketrush.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 票档。
 *
 * <p>同一个活动下可以有多个票档，例如普通票、VIP 票、早鸟票。
 * 库存扣减以票档为最小单位。</p>
 */
public record TicketSku(
        Long id,
        Long eventId,
        String name,
        Long priceFen,
        Integer totalStock,
        LocalDateTime saleStartTime,
        LocalDateTime saleEndTime,
        SkuStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public TicketSku {
        Objects.requireNonNull(eventId, "活动 ID 不能为空");
        Objects.requireNonNull(name, "票档名称不能为空");
        Objects.requireNonNull(priceFen, "票档价格不能为空");
        Objects.requireNonNull(totalStock, "总库存不能为空");
        Objects.requireNonNull(saleStartTime, "票档开售时间不能为空");
        Objects.requireNonNull(saleEndTime, "票档停售时间不能为空");
        Objects.requireNonNull(status, "票档状态不能为空");
        if (priceFen < 0) {
            throw new IllegalArgumentException("票档价格不能小于 0");
        }
        if (totalStock < 0) {
            throw new IllegalArgumentException("票档总库存不能小于 0");
        }
        if (!saleEndTime.isAfter(saleStartTime)) {
            throw new IllegalArgumentException("票档停售时间必须晚于开售时间");
        }
    }

    public boolean canSellAt(LocalDateTime now) {
        return status.canSell()
                && !now.isBefore(saleStartTime)
                && now.isBefore(saleEndTime);
    }
}
