package com.ticketrush.infrastructure.mysql.mapper;

import com.ticketrush.domain.model.EventStatus;
import com.ticketrush.domain.model.TicketEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 票务活动 MyBatis Mapper。
 */
@Mapper
public interface TicketEventMapper {

    Optional<TicketEvent> findById(@Param("id") Long id);

    int insert(TicketEvent event);

    int updateStatus(@Param("id") Long id, @Param("status") EventStatus status);
}
