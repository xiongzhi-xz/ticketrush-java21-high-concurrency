package com.ticketrush.infrastructure.sentinel;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sentinel 本地规则配置。
 */
@ConfigurationProperties(prefix = "ticketrush.sentinel")
public record SentinelRuleProperties(
        boolean enabled,
        double rushQps,
        double hotspotSkuQps,
        int hotspotDurationSeconds,
        int hotspotBurstCount
) {

    public SentinelRuleProperties {
        if (rushQps <= 0) {
            rushQps = 1000;
        }
        if (hotspotSkuQps <= 0) {
            hotspotSkuQps = 100;
        }
        if (hotspotDurationSeconds <= 0) {
            hotspotDurationSeconds = 1;
        }
        if (hotspotBurstCount < 0) {
            hotspotBurstCount = 0;
        }
    }
}
