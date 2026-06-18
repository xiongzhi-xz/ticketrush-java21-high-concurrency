package com.ticketrush.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

public record TicketSearchRecord(
        String id,
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

    public TicketSearchRecord {
        Objects.requireNonNull(id, "search document id must not be null");
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(skuId, "sku id must not be null");
        Objects.requireNonNull(eventName, "event name must not be null");
        Objects.requireNonNull(venueName, "venue name must not be null");
        Objects.requireNonNull(skuName, "sku name must not be null");
        Objects.requireNonNull(priceFen, "price must not be null");
        Objects.requireNonNull(totalStock, "total stock must not be null");
        Objects.requireNonNull(eventStatus, "event status must not be null");
        Objects.requireNonNull(skuStatus, "sku status must not be null");
        Objects.requireNonNull(eventTime, "event time must not be null");
        Objects.requireNonNull(saleStartTime, "sale start time must not be null");
        Objects.requireNonNull(saleEndTime, "sale end time must not be null");
        Objects.requireNonNull(indexedAt, "indexed time must not be null");
    }
}
