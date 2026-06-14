package com.ticketrush.domain.repository;

import com.ticketrush.domain.model.TicketEvent;

import java.util.Optional;

/**
 * 票务活动仓储接口。
 */
public interface TicketEventRepository {

    Optional<TicketEvent> findById(Long eventId);

    TicketEvent save(TicketEvent event);
}
