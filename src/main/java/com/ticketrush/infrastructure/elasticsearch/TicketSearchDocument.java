package com.ticketrush.infrastructure.elasticsearch;

import com.ticketrush.domain.model.EventStatus;
import com.ticketrush.domain.model.SkuStatus;
import com.ticketrush.domain.model.TicketSearchRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.time.LocalDateTime;

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
        LocalDateTime eventTime,

        @Field(type = FieldType.Date)
        LocalDateTime saleStartTime,

        @Field(type = FieldType.Date)
        LocalDateTime saleEndTime,

        @Field(type = FieldType.Date)
        Instant indexedAt
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
                record.eventTime(),
                record.saleStartTime(),
                record.saleEndTime(),
                record.indexedAt()
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
                eventTime,
                saleStartTime,
                saleEndTime,
                indexedAt
        );
    }
}
