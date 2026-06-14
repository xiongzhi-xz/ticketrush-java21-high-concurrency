package com.ticketrush.infrastructure.mysql.repository;

import com.ticketrush.domain.model.EventStatus;
import com.ticketrush.domain.model.SkuStatus;
import com.ticketrush.domain.model.TicketEvent;
import com.ticketrush.domain.model.TicketInventory;
import com.ticketrush.domain.model.TicketSku;
import com.ticketrush.infrastructure.mysql.mapper.TicketEventMapper;
import com.ticketrush.infrastructure.mysql.mapper.TicketInventoryMapper;
import com.ticketrush.infrastructure.mysql.mapper.TicketSkuMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MyBatisTicketRepositoryTest {

    @Test
    void shouldDelegateEventRepositoryToMapper() {
        TicketEventMapper mapper = mock(TicketEventMapper.class);
        MyBatisTicketEventRepository repository = new MyBatisTicketEventRepository(mapper);
        TicketEvent event = event();

        when(mapper.findById(3001L)).thenReturn(Optional.of(event));

        assertThat(repository.findById(3001L)).contains(event);
        assertThat(repository.save(event)).isSameAs(event);
        verify(mapper).insert(event);
    }

    @Test
    void shouldDelegateSkuRepositoryToMapper() {
        TicketSkuMapper mapper = mock(TicketSkuMapper.class);
        MyBatisTicketSkuRepository repository = new MyBatisTicketSkuRepository(mapper);
        TicketSku sku = sku();

        when(mapper.findById(1001L)).thenReturn(Optional.of(sku));
        when(mapper.findByEventId(3001L)).thenReturn(List.of(sku));

        assertThat(repository.findById(1001L)).contains(sku);
        assertThat(repository.findByEventId(3001L)).containsExactly(sku);
        assertThat(repository.save(sku)).isSameAs(sku);
        verify(mapper).insert(sku);
    }

    @Test
    void shouldDelegateInventoryRepositoryToMapper() {
        TicketInventoryMapper mapper = mock(TicketInventoryMapper.class);
        MyBatisTicketInventoryRepository repository = new MyBatisTicketInventoryRepository(mapper);
        TicketInventory inventory = inventory();

        when(mapper.findBySkuId(1001L)).thenReturn(Optional.of(inventory));

        assertThat(repository.findBySkuId(1001L)).contains(inventory);
        assertThat(repository.save(inventory)).isSameAs(inventory);

        repository.release(1001L, 1);
        repository.confirm(1001L, 1);

        verify(mapper).insert(inventory);
        verify(mapper).release(1001L, 1);
        verify(mapper).confirm(1001L, 1);
    }

    private TicketEvent event() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 10, 0);
        return new TicketEvent(
                3001L,
                "TicketRush Live",
                "Main Hall",
                now.plusDays(10),
                now,
                now.plusDays(1),
                EventStatus.SELLING,
                now,
                now
        );
    }

    private TicketSku sku() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 10, 0);
        return new TicketSku(
                1001L,
                3001L,
                "VIP",
                19900L,
                100,
                now,
                now.plusDays(1),
                SkuStatus.ON_SALE,
                now,
                now
        );
    }

    private TicketInventory inventory() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 10, 0);
        return new TicketInventory(1001L, 100, 99, 1, 0, 2L, now);
    }
}
