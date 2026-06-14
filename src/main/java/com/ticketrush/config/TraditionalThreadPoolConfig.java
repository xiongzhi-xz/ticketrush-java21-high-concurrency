package com.ticketrush.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 传统平台线程池配置。
 *
 * <p>该线程池只用于压测对比，不承载抢票主链路。抢票主链路默认使用 Java 21 Virtual Threads。</p>
 */
@Configuration
public class TraditionalThreadPoolConfig {

    @Bean(name = "ticketRushTraditionalExecutor", destroyMethod = "shutdown")
    public ExecutorService ticketRushTraditionalExecutor(
            @Value("${ticketrush.concurrency.benchmark.traditional-pool-size:200}") int poolSize
    ) {
        return Executors.newFixedThreadPool(poolSize, new NamedPlatformThreadFactory("ticketrush-pt-"));
    }

    private static final class NamedPlatformThreadFactory implements ThreadFactory {

        private final String namePrefix;
        private final AtomicInteger index = new AtomicInteger();

        private NamedPlatformThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable task) {
            return Thread.ofPlatform()
                    .name(namePrefix + index.getAndIncrement())
                    .daemon(false)
                    .unstarted(task);
        }
    }
}
