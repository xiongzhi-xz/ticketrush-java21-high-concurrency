package com.ticketrush.application.service;

import com.ticketrush.application.command.IndexTicketEventCommand;
import com.ticketrush.application.command.TicketSearchCommand;
import com.ticketrush.application.dto.TicketEventIndexResult;
import com.ticketrush.common.exception.BusinessException;
import com.ticketrush.domain.model.EventStatus;
import com.ticketrush.domain.model.SkuStatus;
import com.ticketrush.domain.model.TicketEvent;
import com.ticketrush.domain.model.TicketSearchPage;
import com.ticketrush.domain.model.TicketSearchQuery;
import com.ticketrush.domain.model.TicketSearchRecord;
import com.ticketrush.domain.model.TicketSku;
import com.ticketrush.domain.repository.TicketEventRepository;
import com.ticketrush.domain.repository.TicketSearchRepository;
import com.ticketrush.domain.repository.TicketSkuRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketSearchApplicationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-18T01:20:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Test
    void shouldIndexEventSkusAsSearchRecords() {
        FakeTicketEventRepository eventRepository = new FakeTicketEventRepository(event());
        FakeTicketSkuRepository skuRepository = new FakeTicketSkuRepository(List.of(sku(1001L), sku(1002L)));
        FakeTicketSearchRepository searchRepository = new FakeTicketSearchRepository();
        TicketSearchApplicationService service = new TicketSearchApplicationService(
                eventRepository,
                skuRepository,
                searchRepository,
                FIXED_CLOCK
        );

        TicketEventIndexResult result = service.indexEvent(new IndexTicketEventCommand(3001L));

        assertThat(result.eventId()).isEqualTo(3001L);
        assertThat(result.indexedSkuCount()).isEqualTo(2);
        assertThat(result.indexedAt()).isEqualTo(Instant.parse("2026-06-18T01:20:00Z"));
        assertThat(searchRepository.savedRecords).hasSize(2);

        TicketSearchRecord first = searchRepository.savedRecords.getFirst();
        assertThat(first.id()).isEqualTo("3001:1001");
        assertThat(first.eventName()).isEqualTo("TicketRush Live");
        assertThat(first.venueName()).isEqualTo("Main Hall");
        assertThat(first.skuName()).isEqualTo("VIP-1001");
        assertThat(first.eventStatus()).isEqualTo(EventStatus.SELLING);
        assertThat(first.skuStatus()).isEqualTo(SkuStatus.ON_SALE);
        assertThat(first.saleStartTime()).isEqualTo(LocalDateTime.of(2026, 6, 18, 10, 0));
        assertThat(first.saleEndTime()).isEqualTo(LocalDateTime.of(2026, 6, 18, 22, 0));
    }

    @Test
    void shouldRejectMissingEventDuringIndexing() {
        TicketSearchApplicationService service = new TicketSearchApplicationService(
                new FakeTicketEventRepository(null),
                new FakeTicketSkuRepository(List.of()),
                new FakeTicketSearchRepository(),
                FIXED_CLOCK
        );

        assertThatThrownBy(() -> service.indexEvent(new IndexTicketEventCommand(3001L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ticket event not found");
    }

    @Test
    void shouldNormalizeSearchCommand() {
        FakeTicketSearchRepository searchRepository = new FakeTicketSearchRepository();
        TicketSearchApplicationService service = new TicketSearchApplicationService(
                new FakeTicketEventRepository(event()),
                new FakeTicketSkuRepository(List.of()),
                searchRepository,
                FIXED_CLOCK
        );

        service.search(new TicketSearchCommand("  live  ", 3001L, EventStatus.SELLING, SkuStatus.ON_SALE, -1, 500));

        assertThat(searchRepository.lastQuery.keyword()).isEqualTo("live");
        assertThat(searchRepository.lastQuery.eventId()).isEqualTo(3001L);
        assertThat(searchRepository.lastQuery.eventStatus()).isEqualTo(EventStatus.SELLING);
        assertThat(searchRepository.lastQuery.skuStatus()).isEqualTo(SkuStatus.ON_SALE);
        assertThat(searchRepository.lastQuery.page()).isZero();
        assertThat(searchRepository.lastQuery.size()).isEqualTo(100);
    }

    private TicketEvent event() {
        return new TicketEvent(
                3001L,
                "TicketRush Live",
                "Main Hall",
                LocalDateTime.of(2026, 6, 19, 20, 0),
                LocalDateTime.of(2026, 6, 18, 9, 0),
                LocalDateTime.of(2026, 6, 18, 23, 0),
                EventStatus.SELLING,
                LocalDateTime.of(2026, 6, 18, 8, 0),
                LocalDateTime.of(2026, 6, 18, 8, 0)
        );
    }

    private TicketSku sku(Long skuId) {
        return new TicketSku(
                skuId,
                3001L,
                "VIP-" + skuId,
                19900L,
                100,
                LocalDateTime.of(2026, 6, 18, 10, 0),
                LocalDateTime.of(2026, 6, 18, 22, 0),
                SkuStatus.ON_SALE,
                LocalDateTime.of(2026, 6, 18, 8, 0),
                LocalDateTime.of(2026, 6, 18, 8, 0)
        );
    }

    private static class FakeTicketEventRepository implements TicketEventRepository {

        private final TicketEvent event;

        private FakeTicketEventRepository(TicketEvent event) {
            this.event = event;
        }

        @Override
        public Optional<TicketEvent> findById(Long eventId) {
            return Optional.ofNullable(event);
        }

        @Override
        public TicketEvent save(TicketEvent event) {
            return event;
        }
    }

    private static class FakeTicketSkuRepository implements TicketSkuRepository {

        private final List<TicketSku> skus;

        private FakeTicketSkuRepository(List<TicketSku> skus) {
            this.skus = skus;
        }

        @Override
        public Optional<TicketSku> findById(Long skuId) {
            return skus.stream().filter(sku -> sku.id().equals(skuId)).findFirst();
        }

        @Override
        public List<TicketSku> findByEventId(Long eventId) {
            return skus;
        }

        @Override
        public TicketSku save(TicketSku sku) {
            return sku;
        }
    }

    private static class FakeTicketSearchRepository implements TicketSearchRepository {

        private final List<TicketSearchRecord> savedRecords = new ArrayList<>();
        private TicketSearchQuery lastQuery;

        @Override
        public void saveAll(List<TicketSearchRecord> records) {
            savedRecords.clear();
            savedRecords.addAll(records);
        }

        @Override
        public TicketSearchPage search(TicketSearchQuery query) {
            lastQuery = query;
            return new TicketSearchPage(List.of(), 0, query.page(), query.size());
        }
    }
}
