package com.ticketrush.interfaces.controller;

import com.ticketrush.application.command.PreloadInventoryCommand;
import com.ticketrush.application.command.RushTicketCommand;
import com.ticketrush.application.dto.PreloadInventoryResult;
import com.ticketrush.application.dto.RushTicketResult;
import com.ticketrush.application.service.RushTicketApplicationService;
import com.ticketrush.common.api.ApiResponse;
import com.ticketrush.interfaces.request.PreloadInventoryRequest;
import com.ticketrush.interfaces.request.RushTicketRequest;
import com.ticketrush.interfaces.response.PreloadInventoryResponse;
import com.ticketrush.interfaces.response.RushTicketResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 高并发抢票接口。
 */
@RestController
@RequestMapping("/api/rush")
public class RushTicketController {

    private final RushTicketApplicationService rushTicketApplicationService;

    public RushTicketController(RushTicketApplicationService rushTicketApplicationService) {
        this.rushTicketApplicationService = rushTicketApplicationService;
    }

    @PostMapping("/tickets")
    public ApiResponse<RushTicketResponse> rush(@Valid @RequestBody RushTicketRequest request) {
        RushTicketResult result = rushTicketApplicationService.rush(new RushTicketCommand(
                request.requestId(),
                request.userId(),
                request.eventId(),
                request.skuId(),
                request.quantity(),
                request.strategy(),
                request.idempotentKey()
        ));
        return ApiResponse.success(toResponse(result));
    }

    @PostMapping("/inventory/preload")
    public ApiResponse<PreloadInventoryResponse> preloadInventory(
            @Valid @RequestBody PreloadInventoryRequest request
    ) {
        PreloadInventoryResult result = rushTicketApplicationService.preloadInventory(
                new PreloadInventoryCommand(request.skuId(), request.totalStock())
        );
        return ApiResponse.success(toResponse(result));
    }

    private RushTicketResponse toResponse(RushTicketResult result) {
        return new RushTicketResponse(
                result.accepted(),
                result.requestId(),
                result.userId(),
                result.eventId(),
                result.skuId(),
                result.quantity(),
                result.strategy(),
                result.remainingStock(),
                result.idempotentKey(),
                result.message(),
                result.processedThreadName(),
                result.processedByVirtualThread(),
                result.acceptedAt()
        );
    }

    private PreloadInventoryResponse toResponse(PreloadInventoryResult result) {
        return new PreloadInventoryResponse(
                result.skuId(),
                result.totalStock(),
                result.availableStock(),
                result.lockedStock(),
                result.soldStock(),
                result.version(),
                result.preloadedAt()
        );
    }
}
