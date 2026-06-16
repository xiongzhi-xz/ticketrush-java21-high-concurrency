package com.ticketrush.infrastructure.mq;

import com.ticketrush.application.dto.OrderCreateMessage;
import com.ticketrush.application.service.OrderCreateMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 订单创建消息发布器的本地降级实现。
 *
 * <p>当 RocketMQ / Spring Cloud Stream 不可用时（如本地开发环境不启动 RocketMQ），
 * 此 Bean 作为降级替代，仅打印日志不实际发送消息。</p>
 */
@Component
@ConditionalOnMissingBean(RocketMqOrderCreateMessagePublisher.class)
public class NoOpOrderCreateMessagePublisher implements OrderCreateMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpOrderCreateMessagePublisher.class);

    @Override
    public boolean publish(OrderCreateMessage message) {
        log.warn("RocketMQ 不可用，订单创建消息已丢弃（降级模式）: skuId={}, userId={}, idempotentKey={}",
                message.skuId(), message.userId(), message.idempotentKey());
        return true;
    }
}
