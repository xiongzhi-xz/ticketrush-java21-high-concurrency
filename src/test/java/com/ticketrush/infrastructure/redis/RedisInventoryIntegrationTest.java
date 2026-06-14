package com.ticketrush.infrastructure.redis;

import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionResult;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.TicketInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisInventoryIntegrationTest {

    private static final long SKU_ID = 1001L;

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private InventoryRedisKeyFactory keyFactory;
    private final List<String> cleanupKeys = new ArrayList<>();

    @BeforeEach
    void setUp() {
        String host = propertyOrEnv("ticketrush.test.redis.host", "TICKETRUSH_TEST_REDIS_HOST", "localhost");
        int port = Integer.parseInt(propertyOrEnv("ticketrush.test.redis.port", "TICKETRUSH_TEST_REDIS_PORT", "6379"));

        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
        connectionFactory.afterPropertiesSet();
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String pong = connection.ping();
            Assumptions.assumeTrue("PONG".equalsIgnoreCase(pong), "Redis is not available");
        } catch (RuntimeException exception) {
            Assumptions.assumeTrue(false, "Redis is not available: " + exception.getMessage());
        }

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        keyFactory = new InventoryRedisKeyFactory("ticketrush:it:" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        if (redisTemplate != null && !cleanupKeys.isEmpty()) {
            redisTemplate.delete(cleanupKeys);
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void shouldReserveStockAtomicallyWithRedisLua() {
        RedisLuaTicketInventoryRepository repository = new RedisLuaTicketInventoryRepository(
                redisTemplate,
                keyFactory,
                reserveStockScript(),
                Duration.ofMinutes(10)
        );
        repository.save(inventory());
        cleanup(command(InventoryDeductionStrategy.REDIS_LUA, "idem-lua"));

        InventoryDeductionResult result = repository.reserve(command(InventoryDeductionStrategy.REDIS_LUA, "idem-lua"));

        assertThat(result.success()).isTrue();
        assertThat(result.remainingStock()).isEqualTo(8);

        TicketInventory reservedInventory = repository.findBySkuId(SKU_ID).orElseThrow();
        assertThat(reservedInventory.availableStock()).isEqualTo(8);
        assertThat(reservedInventory.lockedStock()).isEqualTo(2);
        assertThat(reservedInventory.version()).isEqualTo(2);

        InventoryDeductionResult duplicated =
                repository.reserve(command(InventoryDeductionStrategy.REDIS_LUA, "idem-lua"));

        assertThat(duplicated.success()).isFalse();
        assertThat(repository.findBySkuId(SKU_ID).orElseThrow().availableStock()).isEqualTo(8);
    }

    @Test
    void shouldReserveStockWithRedisLockAndReleaseLockToken() {
        writeInventoryHash();
        RedisIdempotentGuard idempotentGuard = new RedisIdempotentGuard(redisTemplate, keyFactory);
        RedisLockInventoryDeductionRepository repository = new RedisLockInventoryDeductionRepository(
                redisTemplate,
                keyFactory,
                idempotentGuard,
                unlockInventoryLockScript(),
                Duration.ofMinutes(10),
                Duration.ofSeconds(5)
        );
        InventoryDeductionCommand command = command(InventoryDeductionStrategy.REDIS_LOCK, "idem-lock");
        cleanup(command);

        InventoryDeductionResult result = repository.reserve(command);

        assertThat(result.success()).isTrue();
        assertThat(result.remainingStock()).isEqualTo(8);
        assertThat(redisTemplate.hasKey(keyFactory.inventoryLock(SKU_ID))).isFalse();
        assertThat(redisTemplate.opsForHash().get(keyFactory.inventoryHash(SKU_ID), InventoryRedisFields.AVAILABLE))
                .isEqualTo("8");
        assertThat(redisTemplate.opsForHash().get(keyFactory.inventoryHash(SKU_ID), InventoryRedisFields.LOCKED))
                .isEqualTo("2");

        InventoryDeductionResult duplicated = repository.reserve(command);

        assertThat(duplicated.success()).isFalse();
        assertThat(redisTemplate.opsForHash().get(keyFactory.inventoryHash(SKU_ID), InventoryRedisFields.AVAILABLE))
                .isEqualTo("8");
    }

    private void writeInventoryHash() {
        redisTemplate.opsForHash().putAll(
                keyFactory.inventoryHash(SKU_ID),
                Map.of(
                        InventoryRedisFields.TOTAL, "10",
                        InventoryRedisFields.AVAILABLE, "10",
                        InventoryRedisFields.LOCKED, "0",
                        InventoryRedisFields.SOLD, "0",
                        InventoryRedisFields.VERSION, "1"
                )
        );
    }

    private TicketInventory inventory() {
        return new TicketInventory(SKU_ID, 10, 10, 0, 0, 1L, LocalDateTime.now());
    }

    private InventoryDeductionCommand command(InventoryDeductionStrategy strategy, String idempotentKey) {
        return new InventoryDeductionCommand(
                "req-it",
                2001L,
                3001L,
                SKU_ID,
                2,
                strategy,
                idempotentKey
        );
    }

    private RedisScript<Long> reserveStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(InventoryLuaScripts.RESERVE_STOCK));
        script.setResultType(Long.class);
        return script;
    }

    private RedisScript<Long> unlockInventoryLockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(InventoryLuaScripts.UNLOCK_IF_OWNER));
        script.setResultType(Long.class);
        return script;
    }

    private void cleanup(InventoryDeductionCommand command) {
        cleanupKeys.add(keyFactory.inventoryHash(command.skuId()));
        cleanupKeys.add(keyFactory.inventoryLock(command.skuId()));
        cleanupKeys.add(keyFactory.idempotentKey(command.idempotentKey()));
    }

    private String propertyOrEnv(String propertyName, String envName, String defaultValue) {
        String value = System.getProperty(propertyName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return defaultValue;
    }
}
