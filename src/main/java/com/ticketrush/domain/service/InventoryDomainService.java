package com.ticketrush.domain.service;

import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionResult;
import com.ticketrush.domain.model.InventoryReservation;
import com.ticketrush.domain.model.TicketInventory;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 库存领域服务。
 *
 * <p>这里不直接访问 Redis 或 MySQL，只负责库存业务规则。后续三种防超卖实现都应该遵守
 * 相同的领域语义：先预占锁定库存，订单确认后转为已售，订单失败或超时后释放。</p>
 */
public class InventoryDomainService {

    private final Clock clock;

    public InventoryDomainService(Clock clock) {
        this.clock = clock;
    }

    public InventoryReservation reserve(TicketInventory inventory, InventoryDeductionCommand command) {
        if (!inventory.hasEnoughAvailable(command.quantity())) {
            InventoryDeductionResult result = InventoryDeductionResult.failure(
                    command.skuId(),
                    command.quantity(),
                    command.strategy(),
                    inventory.availableStock(),
                    "可售库存不足"
            );
            return new InventoryReservation(inventory, result);
        }

        TicketInventory reservedInventory = inventory.reserve(command.quantity(), LocalDateTime.now(clock));
        InventoryDeductionResult result = InventoryDeductionResult.success(
                command.skuId(),
                command.quantity(),
                command.strategy(),
                reservedInventory.availableStock()
        );
        return new InventoryReservation(reservedInventory, result);
    }
}
