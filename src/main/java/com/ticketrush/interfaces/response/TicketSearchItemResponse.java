package com.ticketrush.interfaces.response;

import com.ticketrush.domain.model.EventStatus;
import com.ticketrush.domain.model.SkuStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record TicketSearchItemResponse(
        Long eventId,
        Long skuId,
        String eventName,
        String venueName,
        String skuName,
        Long priceFen,
        Integer totalStock,
        EventStatus eventStatus,
        SkuStatus skuStatus,
        LocalDateTime eventTime,
        LocalDateTime saleStartTime,
        LocalDateTime saleEndTime,
        Instant indexedAt
) {
}
