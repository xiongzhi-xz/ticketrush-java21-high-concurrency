package com.ticketrush.job;

import com.ticketrush.config.HotInventoryPreloadProperties;
import com.ticketrush.domain.model.TicketInventory;
import com.ticketrush.domain.repository.TicketInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 热点库存自动预热任务。
 *
 * <p>本地演示可通过配置直接把热门票档库存写入 Redis，生产环境应由活动发布流程触发预热。</p>
 */
@Component
@EnableConfigurationProperties(HotInventoryPreloadProperties.class)
@ConditionalOnProperty(
        prefix = "ticketrush.rush.hot-inventory-preload",
        name = "enabled",
        havingValue = "true"
)
public class HotInventoryPreloadRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HotInventoryPreloadRunner.class);

    private final HotInventoryPreloadProperties properties;
    private final TicketInventoryRepository ticketInventoryRepository;

    public HotInventoryPreloadRunner(
            HotInventoryPreloadProperties properties,
            TicketInventoryRepository ticketInventoryRepository
    ) {
        this.properties = properties;
        this.ticketInventoryRepository = ticketInventoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (HotInventoryPreloadProperties.HotSkuInventory item : properties.items()) {
            if (item.skuId() == null || item.totalStock() == null || item.totalStock() <= 0) {
                log.warn("跳过无效热点库存预热配置 skuId={}, totalStock={}", item.skuId(), item.totalStock());
                continue;
            }
            TicketInventory inventory = new TicketInventory(
                    item.skuId(),
                    item.totalStock(),
                    item.totalStock(),
                    0,
                    0,
                    1L,
                    LocalDateTime.now()
            );
            ticketInventoryRepository.save(inventory);
            log.info("热点库存预热完成 skuId={}, totalStock={}", item.skuId(), item.totalStock());
        }
    }
}
