package com.ticketrush.infrastructure.mysql.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseSchemaTest {

    @Test
    void shouldProvideNonDestructiveSchemaForCoreTables() throws Exception {
        ClassPathResource resource = new ClassPathResource("schema.sql");

        String schema = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(schema).contains(
                "CREATE TABLE IF NOT EXISTS ticket_event",
                "CREATE TABLE IF NOT EXISTS ticket_sku",
                "CREATE TABLE IF NOT EXISTS ticket_inventory",
                "CREATE TABLE IF NOT EXISTS ticket_order"
        );
        assertThat(schema).contains(
                "uk_ticket_order_order_no",
                "uk_ticket_order_idempotent_key",
                "idx_ticket_order_status_expire_id",
                "chk_ticket_inventory_consistency"
        );
        assertThat(schema.toUpperCase()).doesNotContain("DROP TABLE");
    }
}
