package com.ticketrush.job;

import com.ticketrush.config.HotInventoryPreloadProperties;
import com.ticketrush.domain.model.TicketInventory;
import com.ticketrush.domain.repository.TicketInventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HotInventoryPreloadRunnerTest {

    @Test
    void shouldPreloadValidHotInventoryAndSkipInvalidItems() {
        FakeTicketInventoryRepository repository = new FakeTicketInventoryRepository();
        HotInventoryPreloadProperties properties = new HotInventoryPreloadProperties(
                true,
                List.of(
                        new HotInventoryPreloadProperties.HotSkuInventory(1001L, 100),
                        new HotInventoryPreloadProperties.HotSkuInventory(null, 100),
                        new HotInventoryPreloadProperties.HotSkuInventory(1002L, 0)
                )
        );
        HotInventoryPreloadRunner runner = new HotInventoryPreloadRunner(properties, repository);

        runner.run(mock(ApplicationArguments.class));

        assertThat(repository.savedInventories).hasSize(1);
        TicketInventory inventory = repository.savedInventories.getFirst();
        assertThat(inventory.skuId()).isEqualTo(1001L);
        assertThat(inventory.totalStock()).isEqualTo(100);
        assertThat(inventory.availableStock()).isEqualTo(100);
        assertThat(inventory.lockedStock()).isZero();
        assertThat(inventory.soldStock()).isZero();
        assertThat(inventory.version()).isEqualTo(1L);
    }

    private static class FakeTicketInventoryRepository implements TicketInventoryRepository {

        private final List<TicketInventory> savedInventories = new ArrayList<>();

        @Override
        public Optional<TicketInventory> findBySkuId(Long skuId) {
            return savedInventories.stream()
                    .filter(inventory -> inventory.skuId().equals(skuId))
                    .findFirst();
        }

        @Override
        public TicketInventory save(TicketInventory inventory) {
            savedInventories.add(inventory);
            return inventory;
        }

        @Override
        public void release(Long skuId, int quantity) {
        }

        @Override
        public void confirm(Long skuId, int quantity) {
        }
    }
}
