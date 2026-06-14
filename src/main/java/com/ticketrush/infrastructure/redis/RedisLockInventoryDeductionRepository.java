package com.ticketrush.infrastructure.redis;

import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionResult;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.repository.InventoryDeductionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 基于 Redis 分布式锁的库存扣减方案。
 *
 * <p>该方案先通过 SET NX 获取票档粒度锁，再在锁内读写库存 Hash。它比 Lua 方案更容易理解，
 * 但锁竞争和多次网络往返会降低高并发吞吐，适合作为防超卖方案对比。</p>
 */
@Repository
public class RedisLockInventoryDeductionRepository implements InventoryDeductionRepository {

    private final StringRedisTemplate redisTemplate;
    private final InventoryRedisKeyFactory keyFactory;
    private final RedisIdempotentGuard idempotentGuard;
    private final RedisScript<Long> unlockInventoryLockScript;
    private final Duration idempotentKeyTtl;
    private final Duration lockTtl;

    public RedisLockInventoryDeductionRepository(
            StringRedisTemplate redisTemplate,
            InventoryRedisKeyFactory keyFactory,
            RedisIdempotentGuard idempotentGuard,
            @Qualifier("unlockInventoryLockScript") RedisScript<Long> unlockInventoryLockScript,
            @Value("${ticketrush.rush.idempotent-key-ttl:10m}") Duration idempotentKeyTtl,
            @Value("${ticketrush.rush.lock-ttl:3s}") Duration lockTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.keyFactory = keyFactory;
        this.idempotentGuard = idempotentGuard;
        this.unlockInventoryLockScript = unlockInventoryLockScript;
        this.idempotentKeyTtl = idempotentKeyTtl;
        this.lockTtl = lockTtl;
    }

    @Override
    public InventoryDeductionStrategy strategy() {
        return InventoryDeductionStrategy.REDIS_LOCK;
    }

    @Override
    public InventoryDeductionResult reserve(InventoryDeductionCommand command) {
        if (!idempotentGuard.tryMark(command.idempotentKey(), idempotentKeyTtl)) {
            return failure(command, readAvailableStock(command.skuId()).orElse(null), "重复请求");
        }

        String lockKey = keyFactory.inventoryLock(command.skuId());
        String lockToken = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, lockTtl);
        if (!Boolean.TRUE.equals(locked)) {
            idempotentGuard.clear(command.idempotentKey());
            return failure(command, readAvailableStock(command.skuId()).orElse(null), "库存锁竞争失败");
        }

        try {
            Optional<Integer> availableStock = readAvailableStock(command.skuId());
            if (availableStock.isEmpty()) {
                idempotentGuard.clear(command.idempotentKey());
                return failure(command, null, "库存不存在或未预热");
            }
            if (availableStock.get() < command.quantity()) {
                return failure(command, availableStock.get(), "可售库存不足");
            }

            String inventoryKey = keyFactory.inventoryHash(command.skuId());
            redisTemplate.opsForHash().increment(inventoryKey, InventoryRedisFields.AVAILABLE, -command.quantity());
            redisTemplate.opsForHash().increment(inventoryKey, InventoryRedisFields.LOCKED, command.quantity());
            redisTemplate.opsForHash().increment(inventoryKey, InventoryRedisFields.VERSION, 1);
            return InventoryDeductionResult.success(
                    command.skuId(),
                    command.quantity(),
                    strategy(),
                    availableStock.get() - command.quantity()
            );
        } catch (RuntimeException exception) {
            idempotentGuard.clear(command.idempotentKey());
            throw exception;
        } finally {
            unlock(lockKey, lockToken);
        }
    }

    private Optional<Integer> readAvailableStock(Long skuId) {
        Object value = redisTemplate.opsForHash().get(
                keyFactory.inventoryHash(skuId),
                InventoryRedisFields.AVAILABLE
        );
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(String.valueOf(value)));
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

    private void unlock(String lockKey, String lockToken) {
        redisTemplate.execute(unlockInventoryLockScript, List.of(lockKey), lockToken);
    }
}
