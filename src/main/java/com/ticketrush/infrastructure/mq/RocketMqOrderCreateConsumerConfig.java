package com.ticketrush.infrastructure.mq;

import com.ticketrush.application.dto.OrderCreateMessage;
import com.ticketrush.application.service.OrderApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * RocketMQ 订单创建消费者配置。
 *
 * <p>Spring Cloud Stream 会将 `orderCreateConsumer` 绑定到 RocketMQ Topic。
 * 消费失败时抛出异常，让 RocketMQ 按配置进行重试。</p>
 */
@Configuration
public class RocketMqOrderCreateConsumerConfig {

    @Bean
    public Consumer<OrderCreateMessage> orderCreateConsumer(OrderApplicationService orderApplicationService) {
        return orderApplicationService::createOrder;
    }
}
