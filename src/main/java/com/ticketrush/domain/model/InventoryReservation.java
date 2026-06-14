package com.ticketrush.domain.model;

/**
 * 库存预占结果。
 */
public record InventoryReservation(
        TicketInventory inventory,
        InventoryDeductionResult result
) {
}
