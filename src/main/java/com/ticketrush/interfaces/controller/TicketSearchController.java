package com.ticketrush.interfaces.controller;

import com.ticketrush.application.command.IndexTicketEventCommand;
import com.ticketrush.application.command.TicketSearchCommand;
import com.ticketrush.application.dto.TicketEventIndexResult;
import com.ticketrush.application.dto.TicketSearchResult;
import com.ticketrush.application.service.TicketSearchApplicationService;
import com.ticketrush.common.api.ApiResponse;
import com.ticketrush.domain.model.EventStatus;
import com.ticketrush.domain.model.SkuStatus;
import com.ticketrush.domain.model.TicketSearchRecord;
import com.ticketrush.interfaces.response.TicketEventIndexResponse;
import com.ticketrush.interfaces.response.TicketSearchItemResponse;
import com.ticketrush.interfaces.response.TicketSearchResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/search")
public class TicketSearchController {

    private final TicketSearchApplicationService ticketSearchApplicationService;

    public TicketSearchController(TicketSearchApplicationService ticketSearchApplicationService) {
        this.ticketSearchApplicationService = ticketSearchApplicationService;
    }

    @PostMapping("/events/{eventId}/index")
    public ApiResponse<TicketEventIndexResponse> indexEvent(@PathVariable Long eventId) {
        TicketEventIndexResult result = ticketSearchApplicationService.indexEvent(
                new IndexTicketEventCommand(eventId)
        );
        return ApiResponse.success(new TicketEventIndexResponse(
                result.eventId(),
                result.indexedSkuCount(),
                result.indexedAt()
        ));
    }

    @GetMapping("/ticket-skus")
    public ApiResponse<TicketSearchResponse> searchTicketSkus(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) EventStatus eventStatus,
            @RequestParam(required = false) SkuStatus skuStatus,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        TicketSearchResult result = ticketSearchApplicationService.search(new TicketSearchCommand(
                keyword,
                eventId,
                eventStatus,
                skuStatus,
                page,
                size
        ));
        return ApiResponse.success(toResponse(result));
    }

    private TicketSearchResponse toResponse(TicketSearchResult result) {
        return new TicketSearchResponse(
                result.records().stream().map(this::toItemResponse).toList(),
                result.total(),
                result.page(),
                result.size()
        );
    }

    private TicketSearchItemResponse toItemResponse(TicketSearchRecord record) {
        return new TicketSearchItemResponse(
                record.eventId(),
                record.skuId(),
                record.eventName(),
                record.venueName(),
                record.skuName(),
                record.priceFen(),
                record.totalStock(),
                record.eventStatus(),
                record.skuStatus(),
                record.eventTime(),
                record.saleStartTime(),
                record.saleEndTime(),
                record.indexedAt()
        );
    }
}
