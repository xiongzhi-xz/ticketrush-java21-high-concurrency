package com.ticketrush.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 幂等标记。
 *
 * <p>Redis Lua 方案在脚本内完成幂等写入；Redis 锁和 MySQL 乐观锁方案复用该组件，
 * 保证三种库存策略都能阻止同一个幂等键重复预占库存。</p>
 */
@Component
public class RedisIdempotentGuard {

    private final StringRedisTemplate redisTemplate;
    private final InventoryRedisKeyFactory keyFactory;

    public RedisIdempotentGuard(StringRedisTemplate redisTemplate, InventoryRedisKeyFactory keyFactory) {
        this.redisTemplate = redisTemplate;
        this.keyFactory = keyFactory;
    }

    public boolean tryMark(String idempotentKey, Duration ttl) {
        Boolean marked = redisTemplate.opsForValue()
                .setIfAbsent(keyFactory.idempotentKey(idempotentKey), "1", ttl);
        return Boolean.TRUE.equals(marked);
    }

    public void clear(String idempotentKey) {
        redisTemplate.delete(keyFactory.idempotentKey(idempotentKey));
    }
}
