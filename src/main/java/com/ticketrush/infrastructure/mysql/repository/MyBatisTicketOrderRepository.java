package com.ticketrush.infrastructure.mysql.repository;

import com.ticketrush.domain.model.TicketOrder;
import com.ticketrush.domain.repository.TicketOrderRepository;
import com.ticketrush.infrastructure.mysql.mapper.TicketOrderMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的订单仓储实现。
 */
@Repository
public class MyBatisTicketOrderRepository implements TicketOrderRepository {

    private final TicketOrderMapper ticketOrderMapper;

    public MyBatisTicketOrderRepository(TicketOrderMapper ticketOrderMapper) {
        this.ticketOrderMapper = ticketOrderMapper;
    }

    @Override
    public Optional<TicketOrder> findByOrderNo(String orderNo) {
        return ticketOrderMapper.findByOrderNo(orderNo);
    }

    @Override
    public Optional<TicketOrder> findByIdempotentKey(String idempotentKey) {
        return ticketOrderMapper.findByIdempotentKey(idempotentKey);
    }

    @Override
    public boolean existsByIdempotentKey(String idempotentKey) {
        return ticketOrderMapper.countByIdempotentKey(idempotentKey) > 0;
    }

    @Override
    public TicketOrder save(TicketOrder order) {
        ticketOrderMapper.insert(order);
        return order;
    }

    @Override
    public List<TicketOrder> findExpiredPendingOrders(LocalDateTime now, int limit) {
        return ticketOrderMapper.findExpiredPendingOrders(now, limit);
    }

    @Override
    public boolean closeExpiredOrder(String orderNo, LocalDateTime closedAt) {
        return ticketOrderMapper.closeExpiredOrder(
                orderNo,
                closedAt,
                com.ticketrush.domain.model.OrderStatus.PENDING,
                com.ticketrush.domain.model.OrderStatus.CLOSED
        ) == 1;
    }
}
