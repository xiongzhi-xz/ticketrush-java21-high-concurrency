package com.ticketrush.application.service;

import com.ticketrush.application.dto.CloseExpiredOrdersResult;
import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionResult;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.OrderStatus;
import com.ticketrush.domain.model.TicketOrder;
import com.ticketrush.domain.repository.InventoryDeductionRepository;
import com.ticketrush.domain.repository.TicketOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTimeoutCloseApplicationServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-06-14T02:30:00Z"), ZoneId.of("Asia/Shanghai"));

    @Test
    void shouldCloseExpiredOrderAndReleaseLockedStock() {
        FakeTicketOrderRepository orderRepository = new FakeTicketOrderRepository();
        FakeInventoryDeductionRepository deductionRepository =
                new FakeInventoryDeductionRepository(InventoryDeductionStrategy.REDIS_LUA);
        orderRepository.expiredOrders = List.of(expiredOrder("TR001", InventoryDeductionStrategy.REDIS_LUA));
        orderRepository.closeResult = true;
        OrderTimeoutCloseApplicationService service = new OrderTimeoutCloseApplicationService(
                orderRepository,
                List.of(deductionRepository),
                fixedClock
        );

        CloseExpiredOrdersResult result = service.closeExpiredOrders(100);

        assertThat(result.scannedCount()).isEqualTo(1);
        assertThat(result.closedCount()).isEqualTo(1);
        assertThat(result.releasedStockCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        assertThat(deductionRepository.releaseCount.get()).isEqualTo(1);
    }

    @Test
    void shouldSkipReleaseWhenOrderWasAlreadyClosedByAnotherWorker() {
        FakeTicketOrderRepository orderRepository = new FakeTicketOrderRepository();
        FakeInventoryDeductionRepository deductionRepository =
                new FakeInventoryDeductionRepository(InventoryDeductionStrategy.REDIS_LUA);
        orderRepository.expiredOrders = List.of(expiredOrder("TR001", InventoryDeductionStrategy.REDIS_LUA));
        orderRepository.closeResult = false;
        OrderTimeoutCloseApplicationService service = new OrderTimeoutCloseApplicationService(
                orderRepository,
                List.of(deductionRepository),
                fixedClock
        );

        CloseExpiredOrdersResult result = service.closeExpiredOrders(100);

        assertThat(result.closedCount()).isZero();
        assertThat(result.releasedStockCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(deductionRepository.releaseCount.get()).isZero();
    }

    private TicketOrder expiredOrder(String orderNo, InventoryDeductionStrategy strategy) {
        LocalDateTime now = LocalDateTime.now(fixedClock);
        return new TicketOrder(
                1L,
                orderNo,
                2001L,
                3001L,
                1001L,
                2,
                0L,
                strategy,
                OrderStatus.PENDING,
                "idem-" + orderNo,
                now.minusMinutes(30),
                now.minusMinutes(15),
                null,
                null
        );
    }

    private static class FakeTicketOrderRepository implements TicketOrderRepository {

        private List<TicketOrder> expiredOrders = List.of();
        private boolean closeResult;

        @Override
        public Optional<TicketOrder> findByOrderNo(String orderNo) {
            return Optional.empty();
        }

        @Override
        public Optional<TicketOrder> findByIdempotentKey(String idempotentKey) {
            return Optional.empty();
        }

        @Override
        public boolean existsByIdempotentKey(String idempotentKey) {
            return false;
        }

        @Override
        public TicketOrder save(TicketOrder order) {
            return order;
        }

        @Override
        public List<TicketOrder> findExpiredPendingOrders(LocalDateTime now, int limit) {
            return expiredOrders;
        }

        @Override
        public boolean closeExpiredOrder(String orderNo, LocalDateTime closedAt) {
            return closeResult;
        }
    }

    private static class FakeInventoryDeductionRepository implements InventoryDeductionRepository {

        private final InventoryDeductionStrategy strategy;
        private final AtomicInteger releaseCount = new AtomicInteger();

        private FakeInventoryDeductionRepository(InventoryDeductionStrategy strategy) {
            this.strategy = strategy;
        }

        @Override
        public InventoryDeductionStrategy strategy() {
            return strategy;
        }

        @Override
        public InventoryDeductionResult reserve(InventoryDeductionCommand command) {
            return InventoryDeductionResult.success(command.skuId(), command.quantity(), strategy, 0);
        }

        @Override
        public void release(Long skuId, int quantity) {
            releaseCount.incrementAndGet();
        }

        @Override
        public void confirm(Long skuId, int quantity) {
        }
    }
}
