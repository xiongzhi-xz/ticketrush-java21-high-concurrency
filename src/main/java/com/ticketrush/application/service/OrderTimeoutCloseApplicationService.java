package com.ticketrush.application.service;

import com.ticketrush.application.dto.CloseExpiredOrdersResult;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.TicketOrder;
import com.ticketrush.domain.repository.InventoryDeductionRepository;
import com.ticketrush.domain.repository.TicketOrderRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 订单超时关闭应用服务。
 *
 * <p>只有订单从 PENDING 原子更新为 CLOSED 后，才释放锁定库存。这样重复扫描同一订单时，
 * `closeExpiredOrder` 只会成功一次，避免重复释放库存。</p>
 */
@Service
public class OrderTimeoutCloseApplicationService {

    private final TicketOrderRepository ticketOrderRepository;
    private final Map<InventoryDeductionStrategy, InventoryDeductionRepository> deductionRepositories;
    private final Clock clock;

    public OrderTimeoutCloseApplicationService(
            TicketOrderRepository ticketOrderRepository,
            List<InventoryDeductionRepository> deductionRepositories
    ) {
        this(ticketOrderRepository, deductionRepositories, Clock.systemDefaultZone());
    }

    OrderTimeoutCloseApplicationService(
            TicketOrderRepository ticketOrderRepository,
            List<InventoryDeductionRepository> deductionRepositories,
            Clock clock
    ) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.deductionRepositories = toStrategyMap(deductionRepositories);
        this.clock = clock;
    }

    public CloseExpiredOrdersResult closeExpiredOrders(int batchSize) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<TicketOrder> expiredOrders = ticketOrderRepository.findExpiredPendingOrders(now, batchSize);

        int closedCount = 0;
        int releasedStockCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (TicketOrder order : expiredOrders) {
            if (!order.isExpired(now)) {
                skippedCount++;
                continue;
            }

            try {
                boolean closed = ticketOrderRepository.closeExpiredOrder(order.orderNo(), now);
                if (!closed) {
                    skippedCount++;
                    continue;
                }
                deductionRepository(order.inventoryDeductionStrategy()).release(order.skuId(), order.quantity());
                closedCount++;
                releasedStockCount++;
            } catch (RuntimeException exception) {
                failedCount++;
            }
        }

        return new CloseExpiredOrdersResult(
                expiredOrders.size(),
                closedCount,
                releasedStockCount,
                skippedCount,
                failedCount,
                Instant.now(clock)
        );
    }

    private InventoryDeductionRepository deductionRepository(InventoryDeductionStrategy strategy) {
        InventoryDeductionRepository repository = deductionRepositories.get(strategy);
        if (repository == null) {
            throw new IllegalStateException("不支持的库存扣减策略：" + strategy);
        }
        return repository;
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
}
