package com.ticketrush.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Java 21 Virtual Threads 基础配置。
 *
 * <p>Spring Boot 的 `spring.threads.virtual.enabled=true` 会让 Web 请求处理优先使用虚拟线程。
 * 这里额外暴露业务专用执行器，后续抢票资格校验、库存预扣、订单异步编排会显式使用它。</p>
 */
@Configuration
@EnableConfigurationProperties(VirtualThreadProperties.class)
public class VirtualThreadConfig {

    @Bean(name = "ticketRushVirtualThreadExecutor", destroyMethod = "close")
    @ConditionalOnProperty(
            prefix = "ticketrush.concurrency.virtual-threads",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public ExecutorService ticketRushVirtualThreadExecutor(VirtualThreadProperties properties) {
        ThreadFactory threadFactory = Thread.ofVirtual()
                .name(properties.namePrefix(), 0)
                .factory();
        return Executors.newThreadPerTaskExecutor(threadFactory);
    }

    @Bean(name = "applicationTaskExecutor")
    @ConditionalOnProperty(
            prefix = "ticketrush.concurrency.virtual-threads",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public AsyncTaskExecutor applicationTaskExecutor(VirtualThreadProperties properties) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(properties.namePrefix() + "async-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
