package com.ticketrush.application.command;

import com.ticketrush.domain.model.EventStatus;
import com.ticketrush.domain.model.SkuStatus;

public record TicketSearchCommand(
        String keyword,
        Long eventId,
        EventStatus eventStatus,
        SkuStatus skuStatus,
        int page,
        int size
) {
}
