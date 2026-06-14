package com.ticketrush.application.command;

/**
 * 抢票应用命令。
 */
public record RushTicketCommand(
        String requestId,
        Long userId,
        Long eventId,
        Long skuId,
        Integer quantity,
        String idempotentKey
) {
}
