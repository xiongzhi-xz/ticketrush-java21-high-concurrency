package com.ticketrush.infrastructure.redis;

import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionResult;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.TicketInventory;
import com.ticketrush.domain.repository.InventoryDeductionRepository;
import com.ticketrush.domain.repository.TicketInventoryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 Redis Lua 的库存仓储实现。
 *
 * <p>Redis 单线程执行 Lua 脚本，能把库存检查、库存预占、幂等写入合并成一个原子操作，
 * 是抢票入口的主防超卖方案。</p>
 */
@Repository
@Primary
public class RedisLuaTicketInventoryRepository implements TicketInventoryRepository, InventoryDeductionRepository {

    private static final long LUA_SUCCESS = 1L;
    private static final long LUA_STOCK_NOT_ENOUGH = 0L;
    private static final long LUA_DUPLICATED = -1L;
    private static final long LUA_INVENTORY_NOT_FOUND = -2L;

    private final StringRedisTemplate redisTemplate;
    private final InventoryRedisKeyFactory keyFactory;
    private final RedisScript<Long> reserveStockScript;
    private final Duration idempotentKeyTtl;

    public RedisLuaTicketInventoryRepository(
            StringRedisTemplate redisTemplate,
            InventoryRedisKeyFactory keyFactory,
            @Qualifier("reserveStockScript")
            RedisScript<Long> reserveStockScript,
            @Value("${ticketrush.rush.idempotent-key-ttl:10m}") Duration idempotentKeyTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.keyFactory = keyFactory;
        this.reserveStockScript = reserveStockScript;
        this.idempotentKeyTtl = idempotentKeyTtl;
    }

    @Override
    public InventoryDeductionStrategy strategy() {
        return InventoryDeductionStrategy.REDIS_LUA;
    }

    @Override
    public Optional<TicketInventory> findBySkuId(Long skuId) {
        String inventoryKey = keyFactory.inventoryHash(skuId);
        Map<Object, Object> values = redisTemplate.opsForHash().entries(inventoryKey);
        if (values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TicketInventory(
                skuId,
                intValue(values.get(InventoryRedisFields.TOTAL)),
                intValue(values.get(InventoryRedisFields.AVAILABLE)),
                intValue(values.get(InventoryRedisFields.LOCKED)),
                intValue(values.get(InventoryRedisFields.SOLD)),
                longValue(values.get(InventoryRedisFields.VERSION)),
                LocalDateTime.now()
        ));
    }

    @Override
    public TicketInventory save(TicketInventory inventory) {
        redisTemplate.opsForHash().putAll(
                keyFactory.inventoryHash(inventory.skuId()),
                Map.of(
                        InventoryRedisFields.TOTAL, inventory.totalStock().toString(),
                        InventoryRedisFields.AVAILABLE, inventory.availableStock().toString(),
                        InventoryRedisFields.LOCKED, inventory.lockedStock().toString(),
                        InventoryRedisFields.SOLD, inventory.soldStock().toString(),
                        InventoryRedisFields.VERSION, inventory.version().toString()
                )
        );
        return inventory;
    }

    @Override
    public InventoryDeductionResult reserve(InventoryDeductionCommand command) {
        String inventoryKey = keyFactory.inventoryHash(command.skuId());
        String idempotentKey = keyFactory.idempotentKey(command.idempotentKey());
        Long luaResult = redisTemplate.execute(
                reserveStockScript,
                List.of(inventoryKey, idempotentKey),
                command.quantity().toString(),
                String.valueOf(idempotentTtlSeconds())
        );
        long resultCode = luaResult == null ? LUA_INVENTORY_NOT_FOUND : luaResult;
        Integer remainingStock = readAvailableStock(command.skuId()).orElse(null);
        return toDeductionResult(command, resultCode, remainingStock);
    }

    @Override
    public void release(Long skuId, int quantity) {
        String inventoryKey = keyFactory.inventoryHash(skuId);
        redisTemplate.opsForHash().increment(inventoryKey, InventoryRedisFields.AVAILABLE, quantity);
        redisTemplate.opsForHash().increment(inventoryKey, InventoryRedisFields.LOCKED, -quantity);
        redisTemplate.opsForHash().increment(inventoryKey, InventoryRedisFields.VERSION, 1);
    }

    @Override
    public void confirm(Long skuId, int quantity) {
        String inventoryKey = keyFactory.inventoryHash(skuId);
        redisTemplate.opsForHash().increment(inventoryKey, InventoryRedisFields.LOCKED, -quantity);
        redisTemplate.opsForHash().increment(inventoryKey, InventoryRedisFields.SOLD, quantity);
        redisTemplate.opsForHash().increment(inventoryKey, InventoryRedisFields.VERSION, 1);
    }

    private InventoryDeductionResult toDeductionResult(
            InventoryDeductionCommand command,
            long resultCode,
            Integer remainingStock
    ) {
        if (resultCode == LUA_SUCCESS) {
            return InventoryDeductionResult.success(
                    command.skuId(),
                    command.quantity(),
                    command.strategy(),
                    remainingStock
            );
        }
        if (resultCode == LUA_STOCK_NOT_ENOUGH) {
            return InventoryDeductionResult.failure(
                    command.skuId(),
                    command.quantity(),
                    command.strategy(),
                    remainingStock,
                    "可售库存不足"
            );
        }
        if (resultCode == LUA_DUPLICATED) {
            return InventoryDeductionResult.failure(
                    command.skuId(),
                    command.quantity(),
                    command.strategy(),
                    remainingStock,
                    "重复请求"
            );
        }
        return InventoryDeductionResult.failure(
                command.skuId(),
                command.quantity(),
                command.strategy(),
                remainingStock,
                "库存不存在或未预热"
        );
    }

    private Optional<Integer> readAvailableStock(Long skuId) {
        Object value = redisTemplate.opsForHash().get(
                keyFactory.inventoryHash(skuId),
                InventoryRedisFields.AVAILABLE
        );
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(intValue(value));
    }

    private long idempotentTtlSeconds() {
        return Math.max(1, idempotentKeyTtl.toSeconds());
    }

    private int intValue(Object value) {
        return Integer.parseInt(String.valueOf(value));
    }

    private long longValue(Object value) {
        return Long.parseLong(String.valueOf(value));
    }
}
