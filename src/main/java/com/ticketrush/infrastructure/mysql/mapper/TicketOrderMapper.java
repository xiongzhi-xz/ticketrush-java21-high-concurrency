package com.ticketrush.infrastructure.mysql.mapper;

import com.ticketrush.domain.model.OrderStatus;
import com.ticketrush.domain.model.TicketOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
