package com.ticketrush.infrastructure.mq;

import com.ticketrush.application.dto.OrderCreateMessage;
import com.ticketrush.application.service.OrderCreateMessagePublisher;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 订单创建消息发布器。
 */
@Component
public class RocketMqOrderCreateMessagePublisher implements OrderCreateMessagePublisher {

    public static final String ORDER_CREATE_OUT_BINDING = "orderCreate-out-0";

    private final StreamBridge streamBridge;

    public RocketMqOrderCreateMessagePublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean publish(OrderCreateMessage message) {
        return streamBridge.send(
                ORDER_CREATE_OUT_BINDING,
                MessageBuilder.withPayload(message)
                        .setHeader("KEYS", message.idempotentKey())
                        .setHeader("TAGS", "order-create")
                        .build()
        );
    }
}
