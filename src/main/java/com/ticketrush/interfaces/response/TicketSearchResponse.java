package com.ticketrush.interfaces.response;

import java.util.List;

public record TicketSearchResponse(
        List<TicketSearchItemResponse> records,
        long total,
        int page,
        int size
) {

    public TicketSearchResponse {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
