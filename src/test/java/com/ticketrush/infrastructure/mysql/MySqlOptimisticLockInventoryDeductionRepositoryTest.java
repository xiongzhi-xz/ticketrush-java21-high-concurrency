package com.ticketrush.infrastructure.mysql;

import com.ticketrush.domain.model.InventoryDeductionCommand;
import com.ticketrush.domain.model.InventoryDeductionResult;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.TicketInventory;
import com.ticketrush.infrastructure.mysql.mapper.TicketInventoryMapper;
import com.ticketrush.infrastructure.redis.RedisIdempotentGuard;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlOptimisticLockInventoryDeductionRepositoryTest {

    private static final Duration IDEMPOTENT_TTL = Duration.ofMinutes(10);

    @Test
    void shouldReserveByOptimisticLock() {
        TicketInventoryMapper mapper = mock(TicketInventoryMapper.class);
        RedisIdempotentGuard idempotentGuard = mock(RedisIdempotentGuard.class);
        MySqlOptimisticLockInventoryDeductionRepository repository =
                new MySqlOptimisticLockInventoryDeductionRepository(mapper, idempotentGuard, IDEMPOTENT_TTL);
        InventoryDeductionCommand command = command();
        TicketInventory inventory = new TicketInventory(1001L, 10, 10, 0, 0, 3L, LocalDateTime.now());

        when(idempotentGuard.tryMark(command.idempotentKey(), IDEMPOTENT_TTL)).thenReturn(true);
        when(mapper.findBySkuId(command.skuId())).thenReturn(Optional.of(inventory));
        when(mapper.reserveByOptimisticLock(command.skuId(), command.quantity(), inventory.version()))
                .thenReturn(1);

        InventoryDeductionResult result = repository.reserve(command);

        assertThat(result.success()).isTrue();
        assertThat(result.strategy()).isEqualTo(InventoryDeductionStrategy.MYSQL_OPTIMISTIC_LOCK);
        assertThat(result.remainingStock()).isEqualTo(8);
    }

    @Test
    void shouldRejectDuplicatedRequestBeforeTouchingMysql() {
        TicketInventoryMapper mapper = mock(TicketInventoryMapper.class);
        RedisIdempotentGuard idempotentGuard = mock(RedisIdempotentGuard.class);
        MySqlOptimisticLockInventoryDeductionRepository repository =
                new MySqlOptimisticLockInventoryDeductionRepository(mapper, idempotentGuard, IDEMPOTENT_TTL);
        InventoryDeductionCommand command = command();

        when(idempotentGuard.tryMark(command.idempotentKey(), IDEMPOTENT_TTL)).thenReturn(false);

        InventoryDeductionResult result = repository.reserve(command);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("重复请求");
        verify(mapper, never()).findBySkuId(command.skuId());
    }

    private InventoryDeductionCommand command() {
        return new InventoryDeductionCommand(
                "req-001",
                2001L,
                3001L,
                1001L,
                2,
                InventoryDeductionStrategy.MYSQL_OPTIMISTIC_LOCK,
                "idem-001"
        );
    }
}
