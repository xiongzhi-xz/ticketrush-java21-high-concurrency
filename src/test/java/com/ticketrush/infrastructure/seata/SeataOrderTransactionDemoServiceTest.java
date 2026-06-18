package com.ticketrush.infrastructure.seata;

import com.ticketrush.common.api.ErrorCode;
import com.ticketrush.common.exception.BusinessException;
import com.ticketrush.common.id.OrderNoGenerator;
import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.OrderStatus;
import com.ticketrush.domain.model.TicketInventory;
import com.ticketrush.domain.model.TicketOrder;
import com.ticketrush.domain.repository.TicketOrderRepository;
import com.ticketrush.infrastructure.mysql.mapper.TicketInventoryMapper;
import io.seata.spring.annotation.GlobalTransactional;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeataOrderTransactionDemoServiceTest {

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-06-18T02:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Test
    void shouldDeclareSeataGlobalTransaction() throws NoSuchMethodException {
        Method method = SeataOrderTransactionDemoService.class.getMethod(
                "reserveMysqlInventoryAndCreatePendingOrder",
                InventoryDeductionCommand.class
        );

        GlobalTransactional annotation = method.getAnnotation(GlobalTransactional.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("ticketrush-seata-mysql-rush-order");
        assertThat(annotation.rollbackFor()).contains(Exception.class);
    }

    @Test
    void shouldReserveInventoryAndCreatePendingOrder() {
        FakeTicketInventoryMapper inventoryMapper = new FakeTicketInventoryMapper();
        inventoryMapper.inventory = Optional.of(new TicketInventory(
                1001L,
                10,
                10,
                0,
                0,
                3L,
                LocalDateTime.now(fixedClock)
        ));
        inventoryMapper.reserveRows = 1;
        FakeTicketOrderRepository orderRepository = new FakeTicketOrderRepository();
        SeataOrderTransactionDemoService service = service(inventoryMapper, orderRepository);

        TicketOrder order = service.reserveMysqlInventoryAndCreatePendingOrder(command());

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.inventoryDeductionStrategy()).isEqualTo(InventoryDeductionStrategy.MYSQL_OPTIMISTIC_LOCK);
        assertThat(order.idempotentKey()).isEqualTo("idem-seata-001");
        assertThat(inventoryMapper.reserveCount.get()).isEqualTo(1);
        assertThat(orderRepository.saveCount.get()).isEqualTo(1);
    }

    @Test
    void shouldSkipInventoryWhenOrderAlreadyExists() {
        FakeTicketInventoryMapper inventoryMapper = new FakeTicketInventoryMapper();
        FakeTicketOrderRepository orderRepository = new FakeTicketOrderRepository();
        TicketOrder existingOrder = order("TR-EXISTING");
        orderRepository.existingOrder = Optional.of(existingOrder);
        SeataOrderTransactionDemoService service = service(inventoryMapper, orderRepository);

        TicketOrder order = service.reserveMysqlInventoryAndCreatePendingOrder(command());

        assertThat(order).isSameAs(existingOrder);
        assertThat(inventoryMapper.reserveCount.get()).isZero();
        assertThat(orderRepository.saveCount.get()).isZero();
    }

    @Test
    void shouldRejectNonMysqlStrategy() {
        SeataOrderTransactionDemoService service = service(
                new FakeTicketInventoryMapper(),
                new FakeTicketOrderRepository()
        );
        InventoryDeductionCommand command = new InventoryDeductionCommand(
                "req-seata-001",
                2001L,
                3001L,
                1001L,
                1,
                InventoryDeductionStrategy.REDIS_LUA,
                "idem-seata-001"
        );

        assertThatThrownBy(() -> service.reserveMysqlInventoryAndCreatePendingOrder(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.STOCK_DEDUCT_FAILED));
    }

    @Test
    void shouldPropagateOrderSaveFailureForGlobalRollback() {
        FakeTicketInventoryMapper inventoryMapper = new FakeTicketInventoryMapper();
        inventoryMapper.inventory = Optional.of(new TicketInventory(
                1001L,
                10,
                10,
                0,
                0,
                1L,
                LocalDateTime.now(fixedClock)
        ));
        inventoryMapper.reserveRows = 1;
        FakeTicketOrderRepository orderRepository = new FakeTicketOrderRepository();
        orderRepository.failOnSave = true;
        SeataOrderTransactionDemoService service = service(inventoryMapper, orderRepository);

        assertThatThrownBy(() -> service.reserveMysqlInventoryAndCreatePendingOrder(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("订单写入失败");
        assertThat(inventoryMapper.reserveCount.get()).isEqualTo(1);
    }

    private SeataOrderTransactionDemoService service(
            TicketInventoryMapper inventoryMapper,
            TicketOrderRepository orderRepository
    ) {
        return new SeataOrderTransactionDemoService(
                inventoryMapper,
                orderRepository,
                new OrderNoGenerator(),
                Duration.ofMinutes(15),
                fixedClock
        );
    }

    private InventoryDeductionCommand command() {
        return new InventoryDeductionCommand(
                "req-seata-001",
                2001L,
                3001L,
                1001L,
                1,
                InventoryDeductionStrategy.MYSQL_OPTIMISTIC_LOCK,
                "idem-seata-001"
        );
    }

    private TicketOrder order(String orderNo) {
        LocalDateTime now = LocalDateTime.now(fixedClock);
        return new TicketOrder(
                1L,
                orderNo,
                2001L,
                3001L,
                1001L,
                1,
                0L,
                InventoryDeductionStrategy.MYSQL_OPTIMISTIC_LOCK,
                OrderStatus.PENDING,
                "idem-seata-001",
                now,
                now.plusMinutes(15),
                null,
                null
        );
    }

    private static class FakeTicketInventoryMapper implements TicketInventoryMapper {

        private Optional<TicketInventory> inventory = Optional.empty();
        private int reserveRows;
        private final AtomicInteger reserveCount = new AtomicInteger();

        @Override
        public Optional<TicketInventory> findBySkuId(Long skuId) {
            return inventory;
        }

        @Override
        public int insert(TicketInventory inventory) {
            return 1;
        }

        @Override
        public int reserveByOptimisticLock(Long skuId, int quantity, long version) {
            reserveCount.incrementAndGet();
            return reserveRows;
        }

        @Override
        public int confirm(Long skuId, int quantity) {
            return 1;
        }

        @Override
        public int release(Long skuId, int quantity) {
            return 1;
        }
    }

    private class FakeTicketOrderRepository implements TicketOrderRepository {

        private Optional<TicketOrder> existingOrder = Optional.empty();
        private final AtomicReference<TicketOrder> savedOrder = new AtomicReference<>();
        private final AtomicInteger saveCount = new AtomicInteger();
        private boolean failOnSave;

        @Override
        public Optional<TicketOrder> findByOrderNo(String orderNo) {
            return Optional.empty();
        }

        @Override
        public Optional<TicketOrder> findByIdempotentKey(String idempotentKey) {
            return existingOrder;
        }

        @Override
        public boolean existsByIdempotentKey(String idempotentKey) {
            return existingOrder.isPresent();
        }

        @Override
        public TicketOrder save(TicketOrder order) {
            if (failOnSave) {
                throw new IllegalStateException("订单写入失败");
            }
            saveCount.incrementAndGet();
            savedOrder.set(order);
            return order;
        }

        @Override
        public List<TicketOrder> findExpiredPendingOrders(LocalDateTime now, int limit) {
            return List.of();
        }

        @Override
        public boolean closeExpiredOrder(String orderNo, LocalDateTime closedAt) {
            return false;
        }
    }
}
