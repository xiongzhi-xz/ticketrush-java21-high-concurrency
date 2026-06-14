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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 抢票应用服务。
 *
 * <p>应用层负责编排抢票用例：构建幂等键、选择扣减策略、把库存预占提交到虚拟线程执行，
 * 并把基础设施结果转换成稳定的业务响应或业务异常。</p>
 */
@Service
public class RushTicketApplicationService {

    private final TicketInventoryRepository inventoryRepository;
    private final Map<InventoryDeductionStrategy, InventoryDeductionRepository> deductionRepositories;
    private final OrderCreateMessagePublisher orderCreateMessagePublisher;
    private final ExecutorService virtualThreadExecutor;
    private final Duration reserveTimeout;

    public RushTicketApplicationService(
            TicketInventoryRepository inventoryRepository,
            List<InventoryDeductionRepository> deductionRepositories,
            OrderCreateMessagePublisher orderCreateMessagePublisher,
            @Qualifier("ticketRushVirtualThreadExecutor") ExecutorService virtualThreadExecutor,
            @Value("${ticketrush.rush.reserve-timeout:2s}") Duration reserveTimeout
    ) {
        this.inventoryRepository = inventoryRepository;
        this.deductionRepositories = toStrategyMap(deductionRepositories);
        this.orderCreateMessagePublisher = orderCreateMessagePublisher;
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.reserveTimeout = reserveTimeout;
    }

    public RushTicketResult rush(RushTicketCommand command) {
        String idempotentKey = normalizeIdempotentKey(command);
        InventoryDeductionStrategy strategy = normalizeStrategy(command.strategy());
        InventoryDeductionCommand deductionCommand = new InventoryDeductionCommand(
                command.requestId(),
                command.userId(),
                command.eventId(),
                command.skuId(),
                command.quantity(),
                strategy,
                idempotentKey
        );

        AtomicReference<String> workerThreadName = new AtomicReference<>("unknown");
        AtomicBoolean workerVirtual = new AtomicBoolean(false);
        InventoryDeductionResult deductionResult = reserveInVirtualThread(
                deductionCommand,
                workerThreadName,
                workerVirtual
        );

        if (!deductionResult.success()) {
            throw toBusinessException(deductionResult);
        }
        publishOrderCreateMessage(deductionCommand);

        return new RushTicketResult(
                true,
                command.requestId(),
                command.userId(),
                command.eventId(),
                command.skuId(),
                command.quantity(),
                deductionResult.strategy(),
                deductionResult.remainingStock(),
                idempotentKey,
                "抢票请求已受理，等待异步创建订单",
                workerThreadName.get(),
                workerVirtual.get(),
                Instant.now()
        );
    }

    private void publishOrderCreateMessage(InventoryDeductionCommand command) {
        OrderCreateMessage message = new OrderCreateMessage(
                command.requestId(),
                command.userId(),
                command.eventId(),
                command.skuId(),
                command.quantity(),
                command.strategy(),
                command.idempotentKey(),
                Instant.now()
        );
        try {
            boolean published = orderCreateMessagePublisher.publish(message);
            if (!published) {
                releaseReservedStock(command);
                throw new BusinessException(ErrorCode.SERVICE_DEGRADED, "订单创建消息发送失败，库存已释放");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            releaseReservedStock(command);
            throw new BusinessException(ErrorCode.SERVICE_DEGRADED, "订单创建消息发送异常，库存已释放");
        }
    }

    private void releaseReservedStock(InventoryDeductionCommand command) {
        deductionRepository(command.strategy()).release(command.skuId(), command.quantity());
    }

    public PreloadInventoryResult preloadInventory(PreloadInventoryCommand command) {
        TicketInventory inventory = new TicketInventory(
                command.skuId(),
                command.totalStock(),
                command.totalStock(),
                0,
                0,
                1L,
                LocalDateTime.now()
        );
        TicketInventory savedInventory = inventoryRepository.save(inventory);
        return new PreloadInventoryResult(
                savedInventory.skuId(),
                savedInventory.totalStock(),
                savedInventory.availableStock(),
                savedInventory.lockedStock(),
                savedInventory.soldStock(),
                savedInventory.version(),
                Instant.now()
        );
    }

    private InventoryDeductionResult reserveInVirtualThread(
            InventoryDeductionCommand command,
            AtomicReference<String> workerThreadName,
            AtomicBoolean workerVirtual
    ) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                        Thread currentThread = Thread.currentThread();
                        workerThreadName.set(currentThread.getName());
                        workerVirtual.set(currentThread.isVirtual());
                        return deductionRepository(command.strategy()).reserve(command);
                    }, virtualThreadExecutor)
                    .orTimeout(reserveTimeout.toMillis(), TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException exception) {
            throw new BusinessException(ErrorCode.SERVICE_DEGRADED, "库存预占超时或执行失败");
        }
    }

    private BusinessException toBusinessException(InventoryDeductionResult result) {
        if ("重复请求".equals(result.message())) {
            return new BusinessException(ErrorCode.IDEMPOTENT_CONFLICT, result.message());
        }
        if ("可售库存不足".equals(result.message())) {
            return new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, result.message());
        }
        if ("库存锁竞争失败".equals(result.message()) || "乐观锁版本冲突".equals(result.message())) {
            return new BusinessException(ErrorCode.STOCK_DEDUCT_FAILED, result.message());
        }
        return new BusinessException(ErrorCode.STOCK_DEDUCT_FAILED, result.message());
    }

    private InventoryDeductionRepository deductionRepository(InventoryDeductionStrategy strategy) {
        InventoryDeductionRepository repository = deductionRepositories.get(strategy);
        if (repository == null) {
            throw new BusinessException(ErrorCode.STOCK_DEDUCT_FAILED, "不支持的库存扣减策略：" + strategy);
        }
        return repository;
    }

    private InventoryDeductionStrategy normalizeStrategy(InventoryDeductionStrategy strategy) {
        if (strategy == null) {
            return InventoryDeductionStrategy.REDIS_LUA;
        }
        return strategy;
    }

    private Map<InventoryDeductionStrategy, InventoryDeductionRepository> toStrategyMap(
            List<InventoryDeductionRepository> repositories
    ) {
        Map<InventoryDeductionStrategy, InventoryDeductionRepository> strategyMap =
                new EnumMap<>(InventoryDeductionStrategy.class);
        for (InventoryDeductionRepository repository : repositories) {
            strategyMap.put(repository.strategy(), repository);
        }
        return Map.copyOf(strategyMap);
    }

    private String normalizeIdempotentKey(RushTicketCommand command) {
        if (command.idempotentKey() != null && !command.idempotentKey().isBlank()) {
            return command.idempotentKey();
        }
        String requestId = command.requestId() == null || command.requestId().isBlank()
                ? UUID.randomUUID().toString()
                : command.requestId();
        return "rush:%d:%d:%d:%s".formatted(
                command.userId(),
                command.eventId(),
                command.skuId(),
                requestId
        );
    }
}
