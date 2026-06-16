package com.ticketrush.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketrush.application.dto.OrderCreateMessage;
import com.ticketrush.application.service.OrderApplicationService;
import com.ticketrush.common.id.OrderNoGenerator;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.TicketOrder;
import com.ticketrush.domain.repository.TicketOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.MimeTypeUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DirtiesContext
@SpringBootTest(
        classes = RocketMqOrderCreateStreamIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=test",
                "spring.cloud.bootstrap.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.config.import=",
                "seata.enabled=false",
                "spring.cloud.stream.enabled=true",
                "spring.cloud.function.definition=orderCreateConsumer",
                "spring.cloud.stream.defaultBinder=test",
                "spring.cloud.stream.bindings.orderCreate-out-0.destination=ticketrush-order-create-topic",
                "spring.cloud.stream.bindings.orderCreate-out-0.content-type=application/json",
                "spring.cloud.stream.bindings.orderCreateConsumer-in-0.destination=ticketrush-order-create-topic",
                "spring.cloud.stream.bindings.orderCreateConsumer-in-0.group=ticketrush-order-consumer-group",
                "spring.cloud.stream.bindings.orderCreateConsumer-in-0.content-type=application/json",
                "ticketrush.order.expire-ttl=15m"
        }
)
class RocketMqOrderCreateStreamIntegrationTest {

    private static final String ORDER_CREATE_TOPIC = "ticketrush-order-create-topic";

    @Autowired
    private RocketMqOrderCreateMessagePublisher publisher;

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryTicketOrderRepository ticketOrderRepository;

    @BeforeEach
    void setUp() {
        ticketOrderRepository.clear();
    }

    @Test
    void shouldPublishAndConsumeOrderCreateMessageThroughStreamBinder() throws Exception {
        OrderCreateMessage message = message("req-mq-001", "idem-mq-001");

        boolean published = publisher.publish(message);

        assertThat(published).isTrue();
        awaitSavedOrders(1);

        TicketOrder order = ticketOrderRepository.findByIdempotentKey("idem-mq-001").orElseThrow();
        assertThat(order.userId()).isEqualTo(2001L);
        assertThat(order.skuId()).isEqualTo(1001L);
    }

    @Test
    void shouldConsumeOrderCreateMessageAndKeepIdempotency() throws Exception {
        OrderCreateMessage message = message("req-mq-002", "idem-mq-002");

        sendToConsumer(message);
        awaitSavedOrders(1);

        sendToConsumer(message);
        awaitSavedOrders(1);

        assertThat(ticketOrderRepository.saveCount()).isEqualTo(1);
        TicketOrder order = ticketOrderRepository.findByIdempotentKey("idem-mq-002").orElseThrow();
        assertThat(order.userId()).isEqualTo(2001L);
        assertThat(order.eventId()).isEqualTo(3001L);
        assertThat(order.skuId()).isEqualTo(1001L);
        assertThat(order.inventoryDeductionStrategy()).isEqualTo(InventoryDeductionStrategy.REDIS_LUA);
    }

    @Test
    void shouldBuildRocketMqHeadersWhenPublishing() {
        StreamBridge streamBridge = mock(StreamBridge.class);
        when(streamBridge.send(eq(RocketMqOrderCreateMessagePublisher.ORDER_CREATE_OUT_BINDING), any()))
                .thenReturn(true);
        RocketMqOrderCreateMessagePublisher messagePublisher = new RocketMqOrderCreateMessagePublisher(streamBridge);

        boolean published = messagePublisher.publish(message("req-mq-003", "idem-mq-003"));

        assertThat(published).isTrue();
        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(streamBridge).send(eq(RocketMqOrderCreateMessagePublisher.ORDER_CREATE_OUT_BINDING), messageCaptor.capture());
        Message<?> outbound = (Message<?>) messageCaptor.getValue();
        assertThat(outbound.getHeaders().get("KEYS")).isEqualTo("idem-mq-003");
        assertThat(outbound.getHeaders().get("TAGS")).isEqualTo("order-create");
        assertThat(outbound.getPayload()).isInstanceOf(OrderCreateMessage.class);
    }

    private void sendToConsumer(OrderCreateMessage message) throws Exception {
        inputDestination.send(
                MessageBuilder.withPayload(objectMapper.writeValueAsBytes(message))
                        .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                        .build(),
                ORDER_CREATE_TOPIC
        );
    }

    private void awaitSavedOrders(int expectedCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < deadline && ticketOrderRepository.saveCount() != expectedCount) {
            Thread.sleep(20);
        }
        assertThat(ticketOrderRepository.saveCount()).isEqualTo(expectedCount);
    }

    private OrderCreateMessage message(String requestId, String idempotentKey) {
        return new OrderCreateMessage(
                requestId,
                2001L,
                3001L,
                1001L,
                1,
                InventoryDeductionStrategy.REDIS_LUA,
                idempotentKey,
                Instant.parse("2026-06-14T02:29:59Z")
        );
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            MybatisAutoConfiguration.class
    })
    @Import({
            TestChannelBinderConfiguration.class,
            RocketMqOrderCreateMessagePublisher.class,
            RocketMqOrderCreateConsumerConfig.class
    })
    static class TestApplication {

        @Bean
        OrderApplicationService orderApplicationService(
                TicketOrderRepository ticketOrderRepository,
                OrderNoGenerator orderNoGenerator
        ) {
            return new OrderApplicationService(ticketOrderRepository, orderNoGenerator, java.time.Duration.ofMinutes(15));
        }

        @Bean
        OrderNoGenerator orderNoGenerator() {
            return new OrderNoGenerator();
        }

        @Bean
        InMemoryTicketOrderRepository ticketOrderRepository() {
            return new InMemoryTicketOrderRepository();
        }
    }

    static class InMemoryTicketOrderRepository implements TicketOrderRepository {

        private final Map<String, TicketOrder> ordersByIdempotentKey = new ConcurrentHashMap<>();
        private final AtomicInteger saveCount = new AtomicInteger();

        @Override
        public Optional<TicketOrder> findByOrderNo(String orderNo) {
            return ordersByIdempotentKey.values()
                    .stream()
                    .filter(order -> order.orderNo().equals(orderNo))
                    .findFirst();
        }

        @Override
        public Optional<TicketOrder> findByIdempotentKey(String idempotentKey) {
            return Optional.ofNullable(ordersByIdempotentKey.get(idempotentKey));
        }

        @Override
        public boolean existsByIdempotentKey(String idempotentKey) {
            return ordersByIdempotentKey.containsKey(idempotentKey);
        }

        @Override
        public TicketOrder save(TicketOrder order) {
            saveCount.incrementAndGet();
            ordersByIdempotentKey.put(order.idempotentKey(), order);
            return order;
        }

        @Override
        public List<TicketOrder> findExpiredPendingOrders(LocalDateTime now, int limit) {
            return List.of();
        }

        @Override
        public boolean closeExpiredOrder(String orderNo, LocalDateTime closedAt) {
            return false;
        }

        int saveCount() {
            return saveCount.get();
        }

        void clear() {
            ordersByIdempotentKey.clear();
            saveCount.set(0);
        }
    }
}
