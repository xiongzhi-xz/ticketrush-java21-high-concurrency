package com.ticketrush.infrastructure.mysql.mapper;

import com.ticketrush.domain.model.OrderStatus;
import com.ticketrush.domain.model.TicketOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 票务订单 MyBatis Mapper。
 */
@Mapper
public interface TicketOrderMapper {

    Optional<TicketOrder> findByOrderNo(@Param("orderNo") String orderNo);

    Optional<TicketOrder> findByIdempotentKey(@Param("idempotentKey") String idempotentKey);

    int countByIdempotentKey(@Param("idempotentKey") String idempotentKey);

    int insert(TicketOrder order);

    int updateStatus(@Param("orderNo") String orderNo, @Param("status") OrderStatus status);

    List<TicketOrder> findExpiredPendingOrders(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    int closeExpiredOrder(
            @Param("orderNo") String orderNo,
            @Param("closedAt") LocalDateTime closedAt,
            @Param("pendingStatus") OrderStatus pendingStatus,
            @Param("closedStatus") OrderStatus closedStatus
    );
}
