package com.ticketrush.domain.model;

public record TicketSearchQuery(
        String keyword,
        Long eventId,
        EventStatus eventStatus,
        SkuStatus skuStatus,
        int page,
        int size
) {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public TicketSearchQuery {
        keyword = normalizeKeyword(keyword);
        page = Math.max(page, 0);
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        size = Math.min(size, MAX_SIZE);
    }

    private static String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
