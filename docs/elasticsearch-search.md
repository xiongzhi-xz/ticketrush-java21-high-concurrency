# Elasticsearch Activity/SKU Search

This slice adds a read-side Elasticsearch model for activity and ticket SKU lookup.

## Design

- MySQL remains the source of truth for `ticket_event` and `ticket_sku`.
- Elasticsearch stores one denormalized document per `(eventId, skuId)` in `ticketrush_ticket_search`.
- The rush path is unchanged: Sentinel, Redis admission, inventory deduction, and RocketMQ order creation do not depend on Elasticsearch.
- Indexing is explicit and demo-friendly: call the rebuild endpoint after seed data exists in MySQL.

## APIs

Rebuild the search documents for one event:

```bash
curl -X POST http://localhost:8080/api/search/events/3001/index
```

Search activity and SKU documents:

```bash
curl "http://localhost:8080/api/search/ticket-skus?keyword=VIP&eventStatus=SELLING&skuStatus=ON_SALE&page=0&size=20"
```

Supported filters:

- `keyword`: full-text query across event name, venue name, and SKU name.
- `eventId`: exact event filter.
- `eventStatus`: exact `EventStatus` filter.
- `skuStatus`: exact `SkuStatus` filter.
- `page` / `size`: zero-based pagination, `size` is capped at 100.

## Implementation Notes

- Application service: `TicketSearchApplicationService`
- Domain port: `TicketSearchRepository`
- Elasticsearch adapter: `ElasticsearchTicketSearchRepository`
- HTTP entry: `TicketSearchController`
- Search document: `TicketSearchDocument`

## Verification

Automated coverage:

```powershell
mvn "-Dtest=TicketSearchApplicationServiceTest,ElasticsearchTicketSearchRepositoryTest" test
```

Covered behavior:

- Aggregates one MySQL event and its SKUs into search records.
- Normalizes search query page, size, and keyword.
- Builds Elasticsearch `multi_match` plus `term` filter JSON.
- Creates the index with mapping before saving documents.

Runtime smoke still requires a running Docker Compose stack with MySQL data and Elasticsearch.
