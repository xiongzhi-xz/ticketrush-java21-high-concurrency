package com.ticketrush.application.dto;

import java.time.Instant;

public record TicketEventIndexResult(
        Long eventId,
        int indexedSkuCount,
        Instant indexedAt
) {
}
