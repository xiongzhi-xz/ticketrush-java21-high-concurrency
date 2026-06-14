package com.ticketrush.infrastructure.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 库存 Redis Key 工厂。
 *
 * <p>库存 Key 统一封装，避免业务代码到处拼接字符串。热点库存会以票档 skuId 为粒度缓存，
 * 后续 Sentinel 热点参数限流也会围绕 skuId 做保护。</p>
 */
@Component
public class InventoryRedisKeyFactory {

    private final String keyPrefix;

    public InventoryRedisKeyFactory(@Value("${ticketrush.redis.key-prefix:ticketrush}") String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String inventoryHash(Long skuId) {
        return keyPrefix + ":inventory:" + skuId;
    }

    public String inventoryLock(Long skuId) {
        return keyPrefix + ":inventory:lock:" + skuId;
    }

    public String idempotentKey(String idempotentKey) {
        return keyPrefix + ":idempotent:" + idempotentKey;
    }

    public String rushAdmission(Long skuId) {
        return keyPrefix + ":rush:admission:" + skuId;
    }
}
