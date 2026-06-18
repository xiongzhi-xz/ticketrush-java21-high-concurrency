package com.ticketrush.domain.repository;

import com.ticketrush.domain.model.TicketSearchPage;
import com.ticketrush.domain.model.TicketSearchQuery;
import com.ticketrush.domain.model.TicketSearchRecord;

import java.util.List;

public interface TicketSearchRepository {

    void saveAll(List<TicketSearchRecord> records);

    TicketSearchPage search(TicketSearchQuery query);
}
