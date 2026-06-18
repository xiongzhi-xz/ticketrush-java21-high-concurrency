package com.ticketrush.domain.model;

import java.util.List;

public record TicketSearchPage(
        List<TicketSearchRecord> records,
        long total,
        int page,
        int size
) {

    public TicketSearchPage {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
