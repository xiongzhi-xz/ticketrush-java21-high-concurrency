package com.ticketrush.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 热点库存预热配置。
 */
@ConfigurationProperties(prefix = "ticketrush.rush.hot-inventory-preload")
public record HotInventoryPreloadProperties(
        boolean enabled,
        List<HotSkuInventory> items
) {

    public HotInventoryPreloadProperties {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record HotSkuInventory(
            Long skuId,
            Integer totalStock
    ) {
    }
}
