package com.ticketrush.application.service;

import com.ticketrush.application.command.IndexTicketEventCommand;
import com.ticketrush.application.command.TicketSearchCommand;
import com.ticketrush.application.dto.TicketEventIndexResult;
import com.ticketrush.application.dto.TicketSearchResult;
import com.ticketrush.common.api.ErrorCode;
import com.ticketrush.common.exception.BusinessException;
import com.ticketrush.domain.model.TicketEvent;
import com.ticketrush.domain.model.TicketSearchPage;
import com.ticketrush.domain.model.TicketSearchQuery;
import com.ticketrush.domain.model.TicketSearchRecord;
import com.ticketrush.domain.model.TicketSku;
import com.ticketrush.domain.repository.TicketEventRepository;
import com.ticketrush.domain.repository.TicketSearchRepository;
import com.ticketrush.domain.repository.TicketSkuRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class TicketSearchApplicationService {

    private final TicketEventRepository ticketEventRepository;
    private final TicketSkuRepository ticketSkuRepository;
    private final TicketSearchRepository ticketSearchRepository;
    private final Clock clock;

    public TicketSearchApplicationService(
            TicketEventRepository ticketEventRepository,
            TicketSkuRepository ticketSkuRepository,
            TicketSearchRepository ticketSearchRepository
    ) {
        this(ticketEventRepository, ticketSkuRepository, ticketSearchRepository, Clock.systemDefaultZone());
    }

    TicketSearchApplicationService(
            TicketEventRepository ticketEventRepository,
            TicketSkuRepository ticketSkuRepository,
            TicketSearchRepository ticketSearchRepository,
            Clock clock
    ) {
        this.ticketEventRepository = ticketEventRepository;
        this.ticketSkuRepository = ticketSkuRepository;
        this.ticketSearchRepository = ticketSearchRepository;
        this.clock = clock;
    }

    public TicketEventIndexResult indexEvent(IndexTicketEventCommand command) {
        if (command == null || command.eventId() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "eventId must not be null");
        }
        TicketEvent event = ticketEventRepository.findById(command.eventId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_INVALID, "ticket event not found"));
        List<TicketSku> skus = ticketSkuRepository.findByEventId(command.eventId());
        Instant indexedAt = Instant.now(clock);
        List<TicketSearchRecord> records = skus.stream()
                .map(sku -> toSearchRecord(event, sku, indexedAt))
                .toList();

        ticketSearchRepository.saveAll(records);
        return new TicketEventIndexResult(command.eventId(), records.size(), indexedAt);
    }

    public TicketSearchResult search(TicketSearchCommand command) {
        TicketSearchQuery query = new TicketSearchQuery(
                command == null ? null : command.keyword(),
                command == null ? null : command.eventId(),
                command == null ? null : command.eventStatus(),
                command == null ? null : command.skuStatus(),
                command == null ? 0 : command.page(),
                command == null ? 20 : command.size()
        );
        TicketSearchPage page = ticketSearchRepository.search(query);
        return new TicketSearchResult(page.records(), page.total(), page.page(), page.size());
    }

    private TicketSearchRecord toSearchRecord(TicketEvent event, TicketSku sku, Instant indexedAt) {
        return new TicketSearchRecord(
                "%d:%d".formatted(event.id(), sku.id()),
                event.id(),
                sku.id(),
                event.name(),
                event.venueName(),
                sku.name(),
                sku.priceFen(),
                sku.totalStock(),
                event.status(),
                sku.status(),
                event.eventTime(),
                later(event.saleStartTime(), sku.saleStartTime()),
                earlier(event.saleEndTime(), sku.saleEndTime()),
                indexedAt
        );
    }

    private java.time.LocalDateTime later(java.time.LocalDateTime left, java.time.LocalDateTime right) {
        return left.isAfter(right) ? left : right;
    }

    private java.time.LocalDateTime earlier(java.time.LocalDateTime left, java.time.LocalDateTime right) {
        return left.isBefore(right) ? left : right;
    }
}
