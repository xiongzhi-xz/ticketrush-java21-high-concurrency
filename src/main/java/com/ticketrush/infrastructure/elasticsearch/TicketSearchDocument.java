package com.ticketrush.infrastructure.elasticsearch;

import com.ticketrush.domain.model.EventStatus;
import com.ticketrush.domain.model.SkuStatus;
import com.ticketrush.domain.model.TicketSearchRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Document(indexName = "ticketrush_ticket_search")
public record TicketSearchDocument(
        @Id
        String id,

        @Field(type = FieldType.Long)
        Long eventId,

        @Field(type = FieldType.Long)
        Long skuId,

        @Field(type = FieldType.Text)
        String eventName,

        @Field(type = FieldType.Text)
        String venueName,

        @Field(type = FieldType.Text)
        String skuName,

        @Field(type = FieldType.Long)
        Long priceFen,

        @Field(type = FieldType.Integer)
        Integer totalStock,

        @Field(type = FieldType.Keyword)
        String eventStatus,

        @Field(type = FieldType.Keyword)
        String skuStatus,

        @Field(type = FieldType.Date)
        String eventTime,

        @Field(type = FieldType.Date)
        String saleStartTime,

        @Field(type = FieldType.Date)
        String saleEndTime,

        @Field(type = FieldType.Date)
        String indexedAt
) {

    static TicketSearchDocument fromRecord(TicketSearchRecord record) {
        return new TicketSearchDocument(
                record.id(),
                record.eventId(),
                record.skuId(),
                record.eventName(),
                record.venueName(),
                record.skuName(),
                record.priceFen(),
                record.totalStock(),
                record.eventStatus().name(),
                record.skuStatus().name(),
                formatLocalDateTime(record.eventTime()),
                formatLocalDateTime(record.saleStartTime()),
                formatLocalDateTime(record.saleEndTime()),
                formatInstant(record.indexedAt())
        );
    }

    TicketSearchRecord toRecord() {
        return new TicketSearchRecord(
                id,
                eventId,
                skuId,
                eventName,
                venueName,
                skuName,
                priceFen,
                totalStock,
                EventStatus.valueOf(eventStatus),
                SkuStatus.valueOf(skuStatus),
                parseLocalDateTime(eventTime),
                parseLocalDateTime(saleStartTime),
                parseLocalDateTime(saleEndTime),
                parseInstant(indexedAt)
        );
    }

    private static String formatLocalDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static String formatInstant(Instant value) {
        return value == null ? null : value.toString();
    }

    private static LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // Elasticsearch may return date-only values when the indexed time is midnight.
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDateTime();
        }
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // Keep runtime search tolerant of older demo documents with date-only values.
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
        }
    }
}
