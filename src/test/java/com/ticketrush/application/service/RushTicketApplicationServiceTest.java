package com.ticketrush.application.service;

import com.ticketrush.application.command.PreloadInventoryCommand;
import com.ticketrush.application.command.RushTicketCommand;
import com.ticketrush.application.dto.OrderCreateMessage;
import com.ticketrush.application.dto.PreloadInventoryResult;
import com.ticketrush.application.dto.RushTicketResult;
import com.ticketrush.common.api.ErrorCode;
import com.ticketrush.common.exception.BusinessException;
import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionResult;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.TicketInventory;
import com.ticketrush.domain.repository.InventoryDeductionRepository;
import com.ticketrush.domain.repository.TicketInventoryRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RushTicketApplicationServiceTest {

    @Test
    void shouldReserveStockByVirtualThread() {
        FakeInventoryRepository repository = new FakeInventoryRepository();
        FakeOrderCreateMessagePublisher publisher = new FakeOrderCreateMessagePublisher();
        repository.reserveResult = InventoryDeductionResult.success(
                1001L,
                1,
                InventoryDeductionStrategy.REDIS_LUA,
                99
        );
        ExecutorService executor = virtualThreadExecutor();
        try {
            RushTicketApplicationService service = new RushTicketApplicationService(
                    repository,
                    List.of(repository),
                    publisher,
                    executor,
                    Duration.ofSeconds(2)
            );

            RushTicketResult result = service.rush(command("req-001", null));

            assertThat(result.accepted()).isTrue();
            assertThat(result.remainingStock()).isEqualTo(99);
            assertThat(result.processedByVirtualThread()).isTrue();
            assertThat(result.processedThreadName()).startsWith("rush-test-vt-");
            assertThat(repository.latestCommand.get().idempotentKey())
                    .isEqualTo("rush:2001:3001:1001:req-001");
            assertThat(publisher.latestMessage.get().idempotentKey())
                    .isEqualTo("rush:2001:3001:1001:req-001");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldUseRequestedDeductionStrategy() {
        FakeInventoryRepository repository = new FakeInventoryRepository(InventoryDeductionStrategy.REDIS_LOCK);
        FakeOrderCreateMessagePublisher publisher = new FakeOrderCreateMessagePublisher();
        repository.reserveResult = InventoryDeductionResult.success(
                1001L,
                1,
                InventoryDeductionStrategy.REDIS_LOCK,
                98
        );
        ExecutorService executor = virtualThreadExecutor();
        try {
            RushTicketApplicationService service = new RushTicketApplicationService(
                    repository,
                    List.of(repository),
                    publisher,
                    executor,
                    Duration.ofSeconds(2)
            );

            RushTicketResult result = service.rush(command(
                    "req-002",
                    InventoryDeductionStrategy.REDIS_LOCK,
                    null
            ));

            assertThat(result.strategy()).isEqualTo(InventoryDeductionStrategy.REDIS_LOCK);
            assertThat(repository.latestCommand.get().strategy()).isEqualTo(InventoryDeductionStrategy.REDIS_LOCK);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldReleaseReservedStockWhenOrderMessagePublishFailed() {
        FakeInventoryRepository repository = new FakeInventoryRepository();
        FakeOrderCreateMessagePublisher publisher = new FakeOrderCreateMessagePublisher();
        publisher.publishResult = false;
        repository.reserveResult = InventoryDeductionResult.success(
                1001L,
                1,
                InventoryDeductionStrategy.REDIS_LUA,
                99
        );
        ExecutorService executor = virtualThreadExecutor();
        try {
            RushTicketApplicationService service = new RushTicketApplicationService(
                    repository,
                    List.of(repository),
                    publisher,
                    executor,
                    Duration.ofSeconds(2)
            );

            assertThatThrownBy(() -> service.rush(command("req-003", null)))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.errorCode()).isEqualTo(ErrorCode.SERVICE_DEGRADED);
                    });
            assertThat(repository.releaseCount.get()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldMapDuplicatedRequestToBusinessException() {
        FakeInventoryRepository repository = new FakeInventoryRepository();
        FakeOrderCreateMessagePublisher publisher = new FakeOrderCreateMessagePublisher();
        repository.reserveResult = InventoryDeductionResult.failure(
                1001L,
                1,
                InventoryDeductionStrategy.REDIS_LUA,
                99,
                "重复请求"
        );
        ExecutorService executor = virtualThreadExecutor();
        try {
            RushTicketApplicationService service = new RushTicketApplicationService(
                    repository,
                    List.of(repository),
                    publisher,
                    executor,
                    Duration.ofSeconds(2)
            );

            assertThatThrownBy(() -> service.rush(command("req-001", "custom-key")))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.errorCode()).isEqualTo(ErrorCode.IDEMPOTENT_CONFLICT);
                    });
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldPreloadInventory() {
        FakeInventoryRepository repository = new FakeInventoryRepository();
        FakeOrderCreateMessagePublisher publisher = new FakeOrderCreateMessagePublisher();
        ExecutorService executor = virtualThreadExecutor();
        try {
            RushTicketApplicationService service = new RushTicketApplicationService(
                    repository,
                    List.of(repository),
                    publisher,
                    executor,
                    Duration.ofSeconds(2)
            );

            PreloadInventoryResult result = service.preloadInventory(new PreloadInventoryCommand(1001L, 100));

            assertThat(result.skuId()).isEqualTo(1001L);
            assertThat(result.totalStock()).isEqualTo(100);
            assertThat(result.availableStock()).isEqualTo(100);
            assertThat(result.lockedStock()).isZero();
            assertThat(result.soldStock()).isZero();
            assertThat(repository.savedInventory.get().version()).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    private RushTicketCommand command(String requestId, String idempotentKey) {
        return command(requestId, null, idempotentKey);
    }

    private RushTicketCommand command(
            String requestId,
            InventoryDeductionStrategy strategy,
            String idempotentKey
    ) {
        return new RushTicketCommand(
                requestId,
                2001L,
                3001L,
                1001L,
                1,
                strategy,
                idempotentKey
        );
    }

    private ExecutorService virtualThreadExecutor() {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("rush-test-vt-", 0).factory());
    }

    private static class FakeInventoryRepository implements TicketInventoryRepository, InventoryDeductionRepository {

        private final AtomicReference<InventoryDeductionCommand> latestCommand = new AtomicReference<>();
        private final AtomicReference<TicketInventory> savedInventory = new AtomicReference<>();
        private final AtomicInteger releaseCount = new AtomicInteger();
        private final InventoryDeductionStrategy strategy;
        private InventoryDeductionResult reserveResult;

        private FakeInventoryRepository() {
            this(InventoryDeductionStrategy.REDIS_LUA);
        }

        private FakeInventoryRepository(InventoryDeductionStrategy strategy) {
            this.strategy = strategy;
        }

        @Override
        public InventoryDeductionStrategy strategy() {
            return strategy;
        }

        @Override
        public Optional<TicketInventory> findBySkuId(Long skuId) {
            return Optional.ofNullable(savedInventory.get());
        }

        @Override
        public TicketInventory save(TicketInventory inventory) {
            savedInventory.set(inventory);
            return inventory;
        }

        public InventoryDeductionResult reserve(InventoryDeductionCommand command) {
            latestCommand.set(command);
            return reserveResult;
        }

        @Override
        public void release(Long skuId, int quantity) {
            releaseCount.incrementAndGet();
        }

        @Override
        public void confirm(Long skuId, int quantity) {
        }
    }

    private static class FakeOrderCreateMessagePublisher implements OrderCreateMessagePublisher {

        private final AtomicReference<OrderCreateMessage> latestMessage = new AtomicReference<>();
        private boolean publishResult = true;

        @Override
        public boolean publish(OrderCreateMessage message) {
            latestMessage.set(message);
            return publishResult;
        }
    }
}
