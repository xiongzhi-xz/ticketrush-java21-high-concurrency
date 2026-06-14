package com.ticketrush.infrastructure.redis;

/**
 * Redis 库存 Hash 字段名。
 */
public final class InventoryRedisFields {

    public static final String TOTAL = "total";
    public static final String AVAILABLE = "available";
    public static final String LOCKED = "locked";
    public static final String SOLD = "sold";
    public static final String VERSION = "version";

    private InventoryRedisFields() {
    }
}
