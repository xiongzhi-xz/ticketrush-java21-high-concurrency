package com.ticketrush;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TicketRush 应用启动入口。
 *
 * <p>项目固定使用 Java 21，并在后续抢票、库存扣减、异步下单等高并发链路中重点落地
 * Virtual Threads 和 Structured Concurrency。</p>
 */
@SpringBootApplication
public class TicketRushApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketRushApplication.class, args);
    }
}
