package com.ticketrush.domain.model;

/**
 * 库存扣减策略。
 *
 * <p>后续压测会对比三种方案的吞吐、失败率和一致性表现。</p>
 */
public enum InventoryDeductionStrategy {

    REDIS_LUA,
    REDIS_LOCK,
    MYSQL_OPTIMISTIC_LOCK
}
