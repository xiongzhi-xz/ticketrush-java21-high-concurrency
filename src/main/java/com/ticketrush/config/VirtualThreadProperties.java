package com.ticketrush.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 虚拟线程配置项。
 */
@ConfigurationProperties(prefix = "ticketrush.concurrency.virtual-threads")
public record VirtualThreadProperties(
        boolean enabled,
        String namePrefix
) {

    public VirtualThreadProperties {
        if (namePrefix == null || namePrefix.isBlank()) {
            namePrefix = "ticketrush-vt-";
        }
    }
}
