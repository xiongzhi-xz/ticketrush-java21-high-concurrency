package com.ticketrush.domain.repository;

import com.ticketrush.domain.model.TicketOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 票务订单仓储接口。
 */
public interface TicketOrderRepository {

    Optional<TicketOrder> findByOrderNo(String orderNo);

    Optional<TicketOrder> findByIdempotentKey(String idempotentKey);

    boolean existsByIdempotentKey(String idempotentKey);

    TicketOrder save(TicketOrder order);

    List<TicketOrder> findExpiredPendingOrders(LocalDateTime now, int limit);

    boolean closeExpiredOrder(String orderNo, LocalDateTime closedAt);
}
