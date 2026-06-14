package com.ticketrush.infrastructure.redis;

import com.ticketrush.application.service.RushAdmissionGuard;
import com.ticketrush.common.api.ErrorCode;
import com.ticketrush.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 基于 Redis Lua 的抢票准入闸门。
 *
 * <p>它不是库存扣减，而是控制同一热门票档同时进入核心链路的请求数量，
 * 用来保护 Redis、MySQL 和 RocketMQ 下游资源。</p>
 */
@Component
public class RedisRushAdmissionGuard implements RushAdmissionGuard {

    private static final long ADMITTED = 1L;

    private final StringRedisTemplate redisTemplate;
    private final InventoryRedisKeyFactory keyFactory;
    private final RedisScript<Long> acquireAdmissionTokenScript;
    private final RedisScript<Long> releaseAdmissionTokenScript;
    private final boolean enabled;
    private final int maxInFlightPerSku;
    private final Duration tokenTtl;

    public RedisRushAdmissionGuard(
            StringRedisTemplate redisTemplate,
            InventoryRedisKeyFactory keyFactory,
            @Qualifier("acquireAdmissionTokenScript") RedisScript<Long> acquireAdmissionTokenScript,
            @Qualifier("releaseAdmissionTokenScript") RedisScript<Long> releaseAdmissionTokenScript,
            @Value("${ticketrush.rush.admission.enabled:true}") boolean enabled,
            @Value("${ticketrush.rush.admission.max-in-flight-per-sku:500}") int maxInFlightPerSku,
            @Value("${ticketrush.rush.admission.token-ttl:10s}") Duration tokenTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.keyFactory = keyFactory;
        this.acquireAdmissionTokenScript = acquireAdmissionTokenScript;
        this.releaseAdmissionTokenScript = releaseAdmissionTokenScript;
        this.enabled = enabled;
        this.maxInFlightPerSku = maxInFlightPerSku;
        this.tokenTtl = tokenTtl;
    }

    @Override
    public Permit acquire(Long skuId) {
        if (!enabled) {
            return NoopPermit.INSTANCE;
        }

        String admissionKey = keyFactory.rushAdmission(skuId);
        Long result = redisTemplate.execute(
                acquireAdmissionTokenScript,
                List.of(admissionKey),
                String.valueOf(maxInFlightPerSku),
                String.valueOf(Math.max(1, tokenTtl.toSeconds()))
        );
        if (result == null || result != ADMITTED) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, "当前票档排队人数过多，请稍后再试");
        }
        return new RedisAdmissionPermit(redisTemplate, releaseAdmissionTokenScript, admissionKey);
    }

    private enum NoopPermit implements Permit {
        INSTANCE;

        @Override
        public void close() {
        }
    }

    private record RedisAdmissionPermit(
            StringRedisTemplate redisTemplate,
            RedisScript<Long> releaseAdmissionTokenScript,
            String admissionKey
    ) implements Permit {

        @Override
        public void close() {
            redisTemplate.execute(releaseAdmissionTokenScript, List.of(admissionKey));
        }
    }
}
