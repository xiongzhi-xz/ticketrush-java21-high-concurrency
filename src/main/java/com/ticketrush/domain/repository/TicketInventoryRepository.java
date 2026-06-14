package com.ticketrush.domain.repository;

import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionResult;
import com.ticketrush.domain.model.TicketInventory;

import java.util.Optional;

/**
 * 票档库存仓储接口。
 */
public interface TicketInventoryRepository {

    Optional<TicketInventory> findBySkuId(Long skuId);

    TicketInventory save(TicketInventory inventory);

    InventoryDeductionResult reserve(InventoryDeductionCommand command);

    void release(Long skuId, int quantity);

    void confirm(Long skuId, int quantity);
}
