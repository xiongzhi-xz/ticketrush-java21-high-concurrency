package com.ticketrush.application.service;

import com.ticketrush.application.dto.OrderCreateMessage;
import com.ticketrush.common.id.OrderNoGenerator;
import com.ticketrush.domain.model.OrderStatus;
import com.ticketrush.domain.model.TicketOrder;
import com.ticketrush.domain.repository.TicketOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 订单应用服务。
 *
 * <p>RocketMQ 消费端调用该服务创建订单。消费幂等以抢票阶段生成的 idempotentKey 为准，
 * 重复消息不会重复创建订单。</p>
 */
@Service
public class OrderApplicationService {

    private final TicketOrderRepository ticketOrderRepository;
    private final OrderNoGenerator orderNoGenerator;
    private final Duration orderExpireTtl;
    private final Clock clock;

    public OrderApplicationService(
            TicketOrderRepository ticketOrderRepository,
            OrderNoGenerator orderNoGenerator,
            @Value("${ticketrush.order.expire-ttl:15m}") Duration orderExpireTtl
    ) {
        this(ticketOrderRepository, orderNoGenerator, orderExpireTtl, Clock.systemDefaultZone());
    }

    OrderApplicationService(
            TicketOrderRepository ticketOrderRepository,
            OrderNoGenerator orderNoGenerator,
            Duration orderExpireTtl,
            Clock clock
    ) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.orderNoGenerator = orderNoGenerator;
        this.orderExpireTtl = orderExpireTtl;
        this.clock = clock;
    }

    public void createOrder(OrderCreateMessage message) {
        if (ticketOrderRepository.existsByIdempotentKey(message.idempotentKey())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        TicketOrder order = new TicketOrder(
                null,
                orderNoGenerator.nextOrderNo(),
                message.userId(),
                message.eventId(),
                message.skuId(),
                message.quantity(),
                0L,
                OrderStatus.PENDING,
                message.idempotentKey(),
                now,
                now.plus(orderExpireTtl),
                null,
                null
        );
        ticketOrderRepository.save(order);
    }
}
