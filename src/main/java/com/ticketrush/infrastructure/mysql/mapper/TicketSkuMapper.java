package com.ticketrush.infrastructure.mysql.mapper;

import com.ticketrush.domain.model.SkuStatus;
import com.ticketrush.domain.model.TicketSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 票档 MyBatis Mapper。
 */
@Mapper
public interface TicketSkuMapper {

    Optional<TicketSku> findById(@Param("id") Long id);

    List<TicketSku> findByEventId(@Param("eventId") Long eventId);

    int insert(TicketSku sku);

    int updateStatus(@Param("id") Long id, @Param("status") SkuStatus status);
}
