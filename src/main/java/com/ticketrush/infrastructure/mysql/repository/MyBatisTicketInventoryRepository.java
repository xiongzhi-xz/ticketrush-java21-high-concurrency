package com.ticketrush.infrastructure.mysql.repository;

import com.ticketrush.domain.model.TicketInventory;
import com.ticketrush.domain.repository.TicketInventoryRepository;
import com.ticketrush.infrastructure.mysql.mapper.TicketInventoryMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的票档库存仓储实现。
 */
@Repository
public class MyBatisTicketInventoryRepository implements TicketInventoryRepository {

    private final TicketInventoryMapper ticketInventoryMapper;

    public MyBatisTicketInventoryRepository(TicketInventoryMapper ticketInventoryMapper) {
        this.ticketInventoryMapper = ticketInventoryMapper;
    }

    @Override
    public Optional<TicketInventory> findBySkuId(Long skuId) {
        return ticketInventoryMapper.findBySkuId(skuId);
    }

    @Override
    public TicketInventory save(TicketInventory inventory) {
        ticketInventoryMapper.insert(inventory);
        return inventory;
    }

    @Override
    public void release(Long skuId, int quantity) {
        ticketInventoryMapper.release(skuId, quantity);
    }

    @Override
    public void confirm(Long skuId, int quantity) {
        ticketInventoryMapper.confirm(skuId, quantity);
    }
}
