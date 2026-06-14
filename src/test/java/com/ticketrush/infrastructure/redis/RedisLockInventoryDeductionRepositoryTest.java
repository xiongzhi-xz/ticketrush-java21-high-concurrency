package com.ticketrush.infrastructure.redis;

import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionResult;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisLockInventoryDeductionRepositoryTest {

    private static final Duration IDEMPOTENT_TTL = Duration.ofMinutes(10);
    private static final Duration LOCK_TTL = Duration.ofSeconds(3);

    @SuppressWarnings("unchecked")
    @Test
    void shouldReserveStockWithRedisLock() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        RedisIdempotentGuard idempotentGuard = mock(RedisIdempotentGuard.class);
        RedisScript<Long> unlockScript = mock(RedisScript.class);
        InventoryRedisKeyFactory keyFactory = new InventoryRedisKeyFactory("ticketrush");
        RedisLockInventoryDeductionRepository repository = new RedisLockInventoryDeductionRepository(
                redisTemplate,
                keyFactory,
                idempotentGuard,
                unlockScript,
                IDEMPOTENT_TTL,
                LOCK_TTL
        );
        InventoryDeductionCommand command = command();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(idempotentGuard.tryMark(command.idempotentKey(), IDEMPOTENT_TTL)).thenReturn(true);
        when(valueOperations.setIfAbsent(eq("ticketrush:inventory:lock:1001"), anyString(), eq(LOCK_TTL)))
                .thenReturn(true);
        when(hashOperations.get("ticketrush:inventory:1001", InventoryRedisFields.AVAILABLE))
                .thenReturn("10");
        when(redisTemplate.execute(eq(unlockScript), eq(List.of("ticketrush:inventory:lock:1001")), anyString()))
                .thenReturn(1L);

        InventoryDeductionResult result = repository.reserve(command);

        assertThat(result.success()).isTrue();
        assertThat(result.strategy()).isEqualTo(InventoryDeductionStrategy.REDIS_LOCK);
        assertThat(result.remainingStock()).isEqualTo(8);
        verify(hashOperations).increment("ticketrush:inventory:1001", InventoryRedisFields.AVAILABLE, -2);
        verify(hashOperations).increment("ticketrush:inventory:1001", InventoryRedisFields.LOCKED, 2);
        verify(hashOperations).increment("ticketrush:inventory:1001", InventoryRedisFields.VERSION, 1);
    }

    private InventoryDeductionCommand command() {
        return new InventoryDeductionCommand(
                "req-001",
                2001L,
                3001L,
                1001L,
                2,
                InventoryDeductionStrategy.REDIS_LOCK,
                "idem-001"
        );
    }
}
