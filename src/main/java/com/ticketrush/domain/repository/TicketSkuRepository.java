package com.ticketrush.domain.repository;

import com.ticketrush.domain.model.TicketSku;

import java.util.List;
import java.util.Optional;

/**
 * 票档仓储接口。
 */
public interface TicketSkuRepository {

    Optional<TicketSku> findById(Long skuId);

    List<TicketSku> findByEventId(Long eventId);

    TicketSku save(TicketSku sku);
}
