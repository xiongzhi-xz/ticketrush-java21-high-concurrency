package com.ticketrush.application.dto;

import com.ticketrush.domain.model.TicketSearchRecord;

import java.util.List;

public record TicketSearchResult(
        List<TicketSearchRecord> records,
        long total,
        int page,
        int size
) {

    public TicketSearchResult {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
