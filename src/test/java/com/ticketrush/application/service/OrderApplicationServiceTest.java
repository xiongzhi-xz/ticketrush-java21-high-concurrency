package com.ticketrush.application.service;

import com.ticketrush.application.dto.OrderCreateMessage;
import com.ticketrush.common.id.OrderNoGenerator;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.OrderStatus;
import com.ticketrush.domain.model.TicketOrder;
import com.ticketrush.domain.repository.TicketOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OrderApplicationServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-06-14T02:30:00Z"), ZoneId.of("Asia/Shanghai"));

    @Test
    void shouldCreatePendingOrderFromMessage() {
        FakeTicketOrderRepository repository = new FakeTicketOrderRepository();
        OrderApplicationService service = new OrderApplicationService(
                repository,
                new OrderNoGenerator(),
                Duration.ofMinutes(15),
                fixedClock
        );

        service.createOrder(message());

        TicketOrder savedOrder = repository.savedOrder.get();
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.inventoryDeductionStrategy()).isEqualTo(InventoryDeductionStrategy.REDIS_LUA);
        assertThat(savedOrder.idempotentKey()).isEqualTo("idem-001");
        assertThat(savedOrder.expireAt()).isAfter(savedOrder.createdAt());
        assertThat(repository.saveCount.get()).isEqualTo(1);
    }

    @Test
    void shouldSkipDuplicatedMessage() {
        FakeTicketOrderRepository repository = new FakeTicketOrderRepository();
        repository.exists = true;
        OrderApplicationService service = new OrderApplicationService(
                repository,
                new OrderNoGenerator(),
                Duration.ofMinutes(15),
                fixedClock
        );

        service.createOrder(message());

        assertThat(repository.saveCount.get()).isZero();
    }

    private OrderCreateMessage message() {
        return new OrderCreateMessage(
                "req-001",
                2001L,
                3001L,
                1001L,
                1,
                InventoryDeductionStrategy.REDIS_LUA,
                "idem-001",
                Instant.parse("2026-06-14T02:29:59Z")
        );
    }

    private static class FakeTicketOrderRepository implements TicketOrderRepository {

        private final AtomicReference<TicketOrder> savedOrder = new AtomicReference<>();
        private final AtomicInteger saveCount = new AtomicInteger();
        private boolean exists;

        @Override
        public Optional<TicketOrder> findByOrderNo(String orderNo) {
            return Optional.empty();
        }

        @Override
        public Optional<TicketOrder> findByIdempotentKey(String idempotentKey) {
            return Optional.ofNullable(savedOrder.get());
        }

        @Override
        public boolean existsByIdempotentKey(String idempotentKey) {
            return exists;
        }

        @Override
        public TicketOrder save(TicketOrder order) {
            saveCount.incrementAndGet();
            savedOrder.set(order);
            return order;
        }

        @Override
        public java.util.List<TicketOrder> findExpiredPendingOrders(java.time.LocalDateTime now, int limit) {
            return java.util.List.of();
        }

        @Override
        public boolean closeExpiredOrder(String orderNo, java.time.LocalDateTime closedAt) {
            return false;
        }
    }
}
