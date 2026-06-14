package com.ticketrush.domain.repository;

import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionResult;
import com.ticketrush.domain.model.InventoryDeductionStrategy;

/**
 * 库存扣减策略仓储接口。
 *
 * <p>Redis Lua、Redis 分布式锁、MySQL 乐观锁都实现该接口，
 * 应用层通过策略枚举选择具体方案，便于后续压测对比。</p>
 */
public interface InventoryDeductionRepository {

    InventoryDeductionStrategy strategy();

    InventoryDeductionResult reserve(InventoryDeductionCommand command);

    void release(Long skuId, int quantity);

    void confirm(Long skuId, int quantity);
}
