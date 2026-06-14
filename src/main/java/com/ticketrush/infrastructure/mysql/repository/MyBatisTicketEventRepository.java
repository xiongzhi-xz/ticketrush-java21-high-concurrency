package com.ticketrush.infrastructure.mysql.repository;

import com.ticketrush.domain.model.TicketEvent;
import com.ticketrush.domain.repository.TicketEventRepository;
import com.ticketrush.infrastructure.mysql.mapper.TicketEventMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的票务活动仓储实现。
 */
@Repository
public class MyBatisTicketEventRepository implements TicketEventRepository {

    private final TicketEventMapper ticketEventMapper;

    public MyBatisTicketEventRepository(TicketEventMapper ticketEventMapper) {
        this.ticketEventMapper = ticketEventMapper;
    }

    @Override
    public Optional<TicketEvent> findById(Long eventId) {
        return ticketEventMapper.findById(eventId);
    }

    @Override
    public TicketEvent save(TicketEvent event) {
        ticketEventMapper.insert(event);
        return event;
    }
}
