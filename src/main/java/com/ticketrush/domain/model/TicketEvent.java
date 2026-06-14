package com.ticketrush.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 票务活动。
 *
 * <p>活动可以是演出、景区场次或年卡售卖批次。活动控制整体售卖窗口，
 * 票档控制具体库存和价格。</p>
 */
public record TicketEvent(
        Long id,
        String name,
        String venueName,
        LocalDateTime eventTime,
        LocalDateTime saleStartTime,
        LocalDateTime saleEndTime,
        EventStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public TicketEvent {
        Objects.requireNonNull(name, "活动名称不能为空");
        Objects.requireNonNull(venueName, "场馆名称不能为空");
        Objects.requireNonNull(eventTime, "活动时间不能为空");
        Objects.requireNonNull(saleStartTime, "开售时间不能为空");
        Objects.requireNonNull(saleEndTime, "停售时间不能为空");
        Objects.requireNonNull(status, "活动状态不能为空");
        if (!saleEndTime.isAfter(saleStartTime)) {
            throw new IllegalArgumentException("停售时间必须晚于开售时间");
        }
    }

    public boolean canSellAt(LocalDateTime now) {
        return status.canSell()
                && !now.isBefore(saleStartTime)
                && now.isBefore(saleEndTime);
    }
}
