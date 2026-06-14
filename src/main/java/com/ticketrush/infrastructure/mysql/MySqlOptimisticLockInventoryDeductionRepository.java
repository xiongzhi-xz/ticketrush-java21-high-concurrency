package com.ticketrush.infrastructure.mysql;

import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionResult;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.TicketInventory;
import com.ticketrush.domain.repository.InventoryDeductionRepository;
import com.ticketrush.infrastructure.mysql.mapper.TicketInventoryMapper;
import com.ticketrush.infrastructure.redis.RedisIdempotentGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * 基于 MySQL 乐观锁的库存扣减方案。
 *
 * <p>该方案依赖 `version` 字段和 `available_stock >= quantity` 条件防止并发超卖。
 * 在高并发热点票档下，数据库写冲突会明显增加，因此主要用于和 Redis 方案做性能对比。</p>
 */
@Repository
public class MySqlOptimisticLockInventoryDeductionRepository implements InventoryDeductionRepository {

    private final TicketInventoryMapper ticketInventoryMapper;
    private final RedisIdempotentGuard idempotentGuard;
    private final Duration idempotentKeyTtl;

    public MySqlOptimisticLockInventoryDeductionRepository(
            TicketInventoryMapper ticketInventoryMapper,
            RedisIdempotentGuard idempotentGuard,
            @Value("${ticketrush.rush.idempotent-key-ttl:10m}") Duration idempotentKeyTtl
    ) {
        this.ticketInventoryMapper = ticketInventoryMapper;
        this.idempotentGuard = idempotentGuard;
        this.idempotentKeyTtl = idempotentKeyTtl;
    }

    @Override
    public InventoryDeductionStrategy strategy() {
        return InventoryDeductionStrategy.MYSQL_OPTIMISTIC_LOCK;
    }

    @Override
    public InventoryDeductionResult reserve(InventoryDeductionCommand command) {
        if (!idempotentGuard.tryMark(command.idempotentKey(), idempotentKeyTtl)) {
            return failure(command, null, "重复请求");
        }

        try {
            Optional<TicketInventory> inventoryOptional = ticketInventoryMapper.findBySkuId(command.skuId());
            if (inventoryOptional.isEmpty()) {
                idempotentGuard.clear(command.idempotentKey());
                return failure(command, null, "库存不存在或未预热");
            }

            TicketInventory inventory = inventoryOptional.get();
            if (!inventory.hasEnoughAvailable(command.quantity())) {
                return failure(command, inventory.availableStock(), "可售库存不足");
            }

            int updatedRows = ticketInventoryMapper.reserveByOptimisticLock(
                    command.skuId(),
                    command.quantity(),
                    inventory.version()
            );
            if (updatedRows == 1) {
                return InventoryDeductionResult.success(
                        command.skuId(),
                        command.quantity(),
                        strategy(),
                        inventory.availableStock() - command.quantity()
                );
            }

            idempotentGuard.clear(command.idempotentKey());
            return failure(command, inventory.availableStock(), "乐观锁版本冲突");
        } catch (RuntimeException exception) {
            idempotentGuard.clear(command.idempotentKey());
            throw exception;
        }
    }

    private InventoryDeductionResult failure(
            InventoryDeductionCommand command,
            Integer remainingStock,
            String message
    ) {
        return InventoryDeductionResult.failure(
                command.skuId(),
                command.quantity(),
                strategy(),
                remainingStock,
                message
        );
    }
}
