package com.ticketrush.domain.service;

import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.InventoryReservation;
import com.ticketrush.domain.model.TicketInventory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryDomainServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-06-14T02:30:00Z"), ZoneId.of("Asia/Shanghai"));
    private final InventoryDomainService inventoryDomainService = new InventoryDomainService(fixedClock);

    @Test
    void shouldReserveInventoryWhenStockIsEnough() {
        TicketInventory inventory = new TicketInventory(1001L, 100, 100, 0, 0, 1L, now());
        InventoryDeductionCommand command = command(2);

        InventoryReservation reservation = inventoryDomainService.reserve(inventory, command);

        assertThat(reservation.result().success()).isTrue();
        assertThat(reservation.inventory().availableStock()).isEqualTo(98);
        assertThat(reservation.inventory().lockedStock()).isEqualTo(2);
        assertThat(reservation.inventory().soldStock()).isZero();
        assertThat(reservation.inventory().version()).isEqualTo(2L);
    }

    @Test
    void shouldRejectReservationWhenStockIsNotEnough() {
        TicketInventory inventory = new TicketInventory(1001L, 100, 1, 0, 99, 8L, now());
        InventoryDeductionCommand command = command(2);

        InventoryReservation reservation = inventoryDomainService.reserve(inventory, command);

        assertThat(reservation.result().success()).isFalse();
        assertThat(reservation.result().message()).isEqualTo("可售库存不足");
        assertThat(reservation.inventory()).isEqualTo(inventory);
    }

    @Test
    void shouldRejectBrokenInventoryInvariant() {
        assertThatThrownBy(() -> new TicketInventory(1001L, 100, 90, 5, 0, 1L, now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("库存数量不守恒");
    }

    private InventoryDeductionCommand command(int quantity) {
        return new InventoryDeductionCommand(
                "req-001",
                2001L,
                3001L,
                1001L,
                quantity,
                InventoryDeductionStrategy.REDIS_LUA,
                "user:2001:sku:1001"
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(fixedClock);
    }
}
