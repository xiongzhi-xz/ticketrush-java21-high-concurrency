package com.ticketrush.infrastructure.mysql.mapper;

import com.ticketrush.domain.model.EventStatus;
import com.ticketrush.domain.model.InventoryDeductionStrategy;
import com.ticketrush.domain.model.OrderStatus;
import com.ticketrush.domain.model.SkuStatus;
import com.ticketrush.domain.model.TicketEvent;
import com.ticketrush.domain.model.TicketInventory;
import com.ticketrush.domain.model.TicketOrder;
import com.ticketrush.domain.model.TicketSku;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlMapperIntegrationTest {

    private DataSource dataSource;
    private SqlSessionFactory sqlSessionFactory;
    private long eventId;
    private long skuId;

    @BeforeEach
    void setUp() throws Exception {
        String url = jdbcUrl();
        String username = propertyOrEnv("ticketrush.test.mysql.username", "TICKETRUSH_TEST_MYSQL_USERNAME", "ticketrush");
        String password = propertyOrEnv("ticketrush.test.mysql.password", "TICKETRUSH_TEST_MYSQL_PASSWORD", "ticketrush");

        Assumptions.assumeTrue(canConnect(url, username, password), "MySQL is not available");

        dataSource = new DriverManagerDataSource(url, username, password);
        executeSchema(dataSource);
        sqlSessionFactory = sqlSessionFactory(dataSource);

        long suffix = ThreadLocalRandom.current().nextLong(100_000, 999_999);
        eventId = 9_000_000_000L + suffix;
        skuId = 8_000_000_000L + suffix;
        cleanupRows();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dataSource != null && eventId > 0 && skuId > 0) {
            cleanupRows();
        }
    }

    @Test
    void shouldReserveInventoryByOptimisticLock() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            TicketEventMapper eventMapper = session.getMapper(TicketEventMapper.class);
            TicketSkuMapper skuMapper = session.getMapper(TicketSkuMapper.class);
            TicketInventoryMapper inventoryMapper = session.getMapper(TicketInventoryMapper.class);

            eventMapper.insert(event());
            skuMapper.insert(sku());
            inventoryMapper.insert(new TicketInventory(skuId, 10, 10, 0, 0, 1L, LocalDateTime.now()));

            int updatedRows = inventoryMapper.reserveByOptimisticLock(skuId, 2, 1L);
            int staleUpdatedRows = inventoryMapper.reserveByOptimisticLock(skuId, 2, 1L);

            TicketInventory inventory = inventoryMapper.findBySkuId(skuId).orElseThrow();

            assertThat(updatedRows).isEqualTo(1);
            assertThat(staleUpdatedRows).isZero();
            assertThat(inventory.availableStock()).isEqualTo(8);
            assertThat(inventory.lockedStock()).isEqualTo(2);
            assertThat(inventory.version()).isEqualTo(2);

            assertThat(inventoryMapper.release(skuId, 1)).isEqualTo(1);
            assertThat(inventoryMapper.confirm(skuId, 1)).isEqualTo(1);

            TicketInventory finalInventory = inventoryMapper.findBySkuId(skuId).orElseThrow();
            assertThat(finalInventory.availableStock()).isEqualTo(9);
            assertThat(finalInventory.lockedStock()).isZero();
            assertThat(finalInventory.soldStock()).isEqualTo(1);
        }
    }

    @Test
    void shouldCreateAndCloseExpiredPendingOrder() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            TicketEventMapper eventMapper = session.getMapper(TicketEventMapper.class);
            TicketSkuMapper skuMapper = session.getMapper(TicketSkuMapper.class);
            TicketOrderMapper orderMapper = session.getMapper(TicketOrderMapper.class);

            eventMapper.insert(event());
            skuMapper.insert(sku());

            LocalDateTime now = LocalDateTime.now();
            TicketOrder order = new TicketOrder(
                    null,
                    "TRIT" + skuId,
                    2001L,
                    eventId,
                    skuId,
                    1,
                    0L,
                    InventoryDeductionStrategy.REDIS_LUA,
                    OrderStatus.PENDING,
                    "mysql-it-" + skuId,
                    now.minusMinutes(30),
                    now.minusMinutes(15),
                    null,
                    null
            );
            orderMapper.insert(order);

            assertThat(orderMapper.countByIdempotentKey(order.idempotentKey())).isEqualTo(1);
            assertThat(orderMapper.findByOrderNo(order.orderNo())).isPresent();

            List<TicketOrder> expiredOrders = orderMapper.findExpiredPendingOrders(now, 10);
            assertThat(expiredOrders).extracting(TicketOrder::orderNo).contains(order.orderNo());

            int closedRows = orderMapper.closeExpiredOrder(
                    order.orderNo(),
                    now,
                    OrderStatus.PENDING,
                    OrderStatus.CLOSED
            );

            assertThat(closedRows).isEqualTo(1);
            assertThat(orderMapper.findByIdempotentKey(order.idempotentKey()))
                    .get()
                    .extracting(TicketOrder::status)
                    .isEqualTo(OrderStatus.CLOSED);
        }
    }

    private TicketEvent event() {
        LocalDateTime now = LocalDateTime.now();
        return new TicketEvent(
                eventId,
                "MySQL IT Event",
                "Integration Hall",
                now.plusDays(7),
                now.minusMinutes(5),
                now.plusDays(1),
                EventStatus.SELLING,
                now,
                now
        );
    }

    private TicketSku sku() {
        LocalDateTime now = LocalDateTime.now();
        return new TicketSku(
                skuId,
                eventId,
                "MySQL IT SKU",
                9900L,
                10,
                now.minusMinutes(5),
                now.plusDays(1),
                SkuStatus.ON_SALE,
                now,
                now
        );
    }

    private void executeSchema(DataSource schemaDataSource) throws Exception {
        String schema = new ClassPathResource("schema.sql").getContentAsString(StandardCharsets.UTF_8);
        try (Connection connection = schemaDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }
    }

    private void cleanupRows() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM ticket_order WHERE sku_id = " + skuId + " OR event_id = " + eventId);
            statement.executeUpdate("DELETE FROM ticket_inventory WHERE sku_id = " + skuId);
            statement.executeUpdate("DELETE FROM ticket_sku WHERE id = " + skuId + " OR event_id = " + eventId);
            statement.executeUpdate("DELETE FROM ticket_event WHERE id = " + eventId);
        }
    }

    private SqlSessionFactory sqlSessionFactory(DataSource mapperDataSource) throws Exception {
        Configuration configuration = new Configuration(new Environment(
                "mysql-it",
                new JdbcTransactionFactory(),
                mapperDataSource
        ));
        configuration.setMapUnderscoreToCamelCase(true);

        parse(configuration, "mapper/TicketEventMapper.xml");
        parse(configuration, "mapper/TicketSkuMapper.xml");
        parse(configuration, "mapper/TicketInventoryMapper.xml");
        parse(configuration, "mapper/TicketOrderMapper.xml");

        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private void parse(Configuration configuration, String resource) throws Exception {
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    resource,
                    configuration.getSqlFragments()
            );
            mapperBuilder.parse();
        }
    }

    private boolean canConnect(String url, String username, String password) {
        try (Connection ignored = DriverManager.getConnection(url, username, password)) {
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    private String jdbcUrl() {
        String host = propertyOrEnv("ticketrush.test.mysql.host", "TICKETRUSH_TEST_MYSQL_HOST", "127.0.0.1");
        String port = propertyOrEnv("ticketrush.test.mysql.port", "TICKETRUSH_TEST_MYSQL_PORT", "3306");
        String database = propertyOrEnv("ticketrush.test.mysql.database", "TICKETRUSH_TEST_MYSQL_DATABASE", "ticketrush");
        return "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
                .formatted(host, port, database);
    }

    private String propertyOrEnv(String propertyName, String envName, String defaultValue) {
        String value = System.getProperty(propertyName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return defaultValue;
    }
}
