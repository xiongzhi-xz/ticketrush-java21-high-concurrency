package com.ticketrush.interfaces.response;

import java.time.Instant;

public record TicketEventIndexResponse(
        Long eventId,
        int indexedSkuCount,
        Instant indexedAt
) {
}
