package com.ticketrush.infrastructure.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketrush.domain.model.EventStatus;
import com.ticketrush.domain.model.SkuStatus;
import com.ticketrush.domain.model.TicketSearchQuery;
import com.ticketrush.domain.model.TicketSearchRecord;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticsearchTicketSearchRepositoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldBuildKeywordAndFilterQuery() throws Exception {
        ElasticsearchTicketSearchRepository repository = new ElasticsearchTicketSearchRepository(
                mock(ElasticsearchOperations.class)
        );

        String source = repository.buildQuerySource(new TicketSearchQuery(
                "  vip live  ",
                3001L,
                EventStatus.SELLING,
                SkuStatus.ON_SALE,
                0,
                20
        ));

        JsonNode root = MAPPER.readTree(source);
        JsonNode multiMatch = root.at("/bool/must/0/multi_match");
        assertThat(multiMatch.get("query").asText()).isEqualTo("vip live");
        List<String> fields = StreamSupport.stream(multiMatch.get("fields").spliterator(), false)
                .map(JsonNode::asText)
                .toList();
        assertThat(fields).containsExactly("eventName^2", "venueName", "skuName^2");
        assertThat(root.at("/bool/filter/0/term/eventId").asLong()).isEqualTo(3001L);
        assertThat(root.at("/bool/filter/1/term/eventStatus").asText()).isEqualTo("SELLING");
        assertThat(root.at("/bool/filter/2/term/skuStatus").asText()).isEqualTo("ON_SALE");
    }

    @Test
    void shouldUseMatchAllWhenKeywordIsBlank() throws Exception {
        ElasticsearchTicketSearchRepository repository = new ElasticsearchTicketSearchRepository(
                mock(ElasticsearchOperations.class)
        );

        String source = repository.buildQuerySource(new TicketSearchQuery(" ", null, null, null, 0, 20));

        JsonNode root = MAPPER.readTree(source);
        assertThat(root.at("/bool/must/0").has("match_all")).isTrue();
        assertThat(root.at("/bool/filter").size()).isZero();
    }

    @Test
    void shouldCreateIndexAndSaveDocuments() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations indexOperations = mock(IndexOperations.class);
        when(operations.indexOps(TicketSearchDocument.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);
        ElasticsearchTicketSearchRepository repository = new ElasticsearchTicketSearchRepository(operations);

        repository.saveAll(List.of(record()));

        verify(indexOperations).createWithMapping();
        verify(operations).save(any(Iterable.class));
        verify(indexOperations).refresh();
    }

    @Test
    void shouldRoundTripDocumentMapping() {
        TicketSearchRecord record = record();

        TicketSearchRecord mapped = TicketSearchDocument.fromRecord(record).toRecord();

        assertThat(mapped).isEqualTo(record);
    }

    @Test
    void shouldReadDateOnlyValuesFromElasticsearchSource() {
        TicketSearchDocument document = new TicketSearchDocument(
                "3001:1001",
                3001L,
                1001L,
                "TicketRush Live",
                "Main Hall",
                "VIP",
                19900L,
                100,
                "SELLING",
                "ON_SALE",
                "2026-06-19",
                "2026-06-18",
                "2026-06-18",
                "2026-06-18T01:20:00Z"
        );

        TicketSearchRecord mapped = document.toRecord();

        assertThat(mapped.eventTime()).isEqualTo(LocalDateTime.of(2026, 6, 19, 0, 0));
        assertThat(mapped.saleStartTime()).isEqualTo(LocalDateTime.of(2026, 6, 18, 0, 0));
        assertThat(mapped.saleEndTime()).isEqualTo(LocalDateTime.of(2026, 6, 18, 0, 0));
        assertThat(mapped.indexedAt()).isEqualTo(Instant.parse("2026-06-18T01:20:00Z"));
    }

    private TicketSearchRecord record() {
        return new TicketSearchRecord(
                "3001:1001",
                3001L,
                1001L,
                "TicketRush Live",
                "Main Hall",
                "VIP",
                19900L,
                100,
                EventStatus.SELLING,
                SkuStatus.ON_SALE,
                LocalDateTime.of(2026, 6, 19, 20, 0),
                LocalDateTime.of(2026, 6, 18, 10, 0),
                LocalDateTime.of(2026, 6, 18, 22, 0),
                Instant.parse("2026-06-18T01:20:00Z")
        );
    }
}
