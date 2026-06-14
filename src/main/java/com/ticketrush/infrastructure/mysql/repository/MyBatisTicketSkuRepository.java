package com.ticketrush.infrastructure.mysql.repository;

import com.ticketrush.domain.model.TicketSku;
import com.ticketrush.domain.repository.TicketSkuRepository;
import com.ticketrush.infrastructure.mysql.mapper.TicketSkuMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的票档仓储实现。
 */
@Repository
public class MyBatisTicketSkuRepository implements TicketSkuRepository {

    private final TicketSkuMapper ticketSkuMapper;

    public MyBatisTicketSkuRepository(TicketSkuMapper ticketSkuMapper) {
        this.ticketSkuMapper = ticketSkuMapper;
    }

    @Override
    public Optional<TicketSku> findById(Long skuId) {
        return ticketSkuMapper.findById(skuId);
    }

    @Override
    public List<TicketSku> findByEventId(Long eventId) {
        return ticketSkuMapper.findByEventId(eventId);
    }

    @Override
    public TicketSku save(TicketSku sku) {
        ticketSkuMapper.insert(sku);
        return sku;
    }
}
