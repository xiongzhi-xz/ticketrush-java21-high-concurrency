package com.ticketrush.infrastructure.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ticketrush.domain.model.TicketSearchPage;
import com.ticketrush.domain.model.TicketSearchQuery;
import com.ticketrush.domain.model.TicketSearchRecord;
import com.ticketrush.domain.repository.TicketSearchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ElasticsearchTicketSearchRepository implements TicketSearchRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ElasticsearchOperations operations;

    public ElasticsearchTicketSearchRepository(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public void saveAll(List<TicketSearchRecord> records) {
        ensureIndex();
        if (records == null || records.isEmpty()) {
            return;
        }
        List<TicketSearchDocument> documents = records.stream()
                .map(TicketSearchDocument::fromRecord)
                .toList();
        operations.save(documents);
        operations.indexOps(TicketSearchDocument.class).refresh();
    }

    @Override
    public TicketSearchPage search(TicketSearchQuery query) {
        TicketSearchQuery safeQuery = query == null
                ? new TicketSearchQuery(null, null, null, null, 0, 20)
                : query;
        ensureIndex();
        StringQuery esQuery = new StringQuery(
                buildQuerySource(safeQuery),
                PageRequest.of(safeQuery.page(), safeQuery.size())
        );
        SearchHits<TicketSearchDocument> hits = operations.search(esQuery, TicketSearchDocument.class);
        List<TicketSearchRecord> records = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(TicketSearchDocument::toRecord)
                .toList();
        return new TicketSearchPage(records, hits.getTotalHits(), safeQuery.page(), safeQuery.size());
    }

    String buildQuerySource(TicketSearchQuery query) {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode bool = root.putObject("bool");
        ArrayNode must = bool.putArray("must");
        ArrayNode filter = bool.putArray("filter");

        if (query.keyword() == null) {
            must.addObject().putObject("match_all");
        } else {
            ObjectNode multiMatch = must.addObject().putObject("multi_match");
            multiMatch.put("query", query.keyword());
            ArrayNode fields = multiMatch.putArray("fields");
            fields.add("eventName^2");
            fields.add("venueName");
            fields.add("skuName^2");
        }

        addTermFilter(filter, "eventId", query.eventId());
        addTermFilter(filter, "eventStatus", query.eventStatus() == null ? null : query.eventStatus().name());
        addTermFilter(filter, "skuStatus", query.skuStatus() == null ? null : query.skuStatus().name());

        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to build Elasticsearch query", e);
        }
    }

    private void addTermFilter(ArrayNode filter, String field, Object value) {
        if (value == null) {
            return;
        }
        ObjectNode term = filter.addObject().putObject("term");
        if (value instanceof Number number) {
            term.put(field, number.longValue());
        } else {
            term.put(field, String.valueOf(value));
        }
    }

    private void ensureIndex() {
        IndexOperations indexOperations = operations.indexOps(TicketSearchDocument.class);
        if (!indexOperations.exists()) {
            indexOperations.createWithMapping();
        }
    }
}
