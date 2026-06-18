# TicketRush 热点票档分摊压测报告

## 目标

验证同一批抢票流量打向单一热门票档和分散到多个票档时，Sentinel 热点参数限流的表现差异。

本轮重点观察：

- `SKU_SPREAD=1` 时，单票档是否被稳定保护并返回 `C0429`。
- `SKU_SPREAD=20` 时，流量按票档分散后是否减少热点限流。
- 两组请求是否保持 0 非预期响应和可控 p95 延迟。

## 环境

| 项目 | 值 |
| --- | --- |
| 日期 | 2026-06-18 |
| Commit | `eb6c99e` 后工作区 |
| 应用 | Docker Compose `ticketrush-app` |
| Java | 21.0.11 |
| 压测工具 | Dockerized k6 `grafana/k6` |
| 库存策略 | `REDIS_LUA` |
| VUS | 10 |
| Duration | 10s |
| Sleep | 0.01s |
| Stock | 1,000,000 per SKU |

健康检查结果：

```text
status=UP
virtualThreadsEnabled=true
currentThreadVirtual=true
```

## 命令

单热点票档：

```powershell
docker run --rm --network ticketrush-local_ticketrush `
  -v "${scriptPath}:/scripts:ro" `
  -v "${resultPath}:/results" `
  grafana/k6 run `
  --summary-export /results/single-sku.json `
  -e BASE_URL=http://app:8080 `
  -e STRATEGY=REDIS_LUA `
  -e SCENARIO_TAG=hotspot-single-sku `
  -e SKU_SPREAD=1 `
  -e VUS=10 `
  -e DURATION=10s `
  -e STOCK=1000000 `
  -e SLEEP=0.01 `
  /scripts/stability-governance.js
```

多票档分摊：

```powershell
docker run --rm --network ticketrush-local_ticketrush `
  -v "${scriptPath}:/scripts:ro" `
  -v "${resultPath}:/results" `
  grafana/k6 run `
  --summary-export /results/multi-sku-20.json `
  -e BASE_URL=http://app:8080 `
  -e STRATEGY=REDIS_LUA `
  -e SCENARIO_TAG=hotspot-multi-sku-20 `
  -e SKU_SPREAD=20 `
  -e VUS=10 `
  -e DURATION=10s `
  -e STOCK=1000000 `
  -e SLEEP=0.01 `
  /scripts/stability-governance.js
```

## 结果

| 场景 | SKU_SPREAD | HTTP 请求数 | 吞吐 | 受理数 | `C0429` 数 | `C0429` 占比 | p95 | max | 非预期响应 |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 单热点票档 | 1 | 8,724 | 871.47 req/s | 1,085 | 7,638 | 87.56% | 3.23ms | 13.61ms | 0.00% |
| 多票档分摊 | 20 | 7,516 | 749.51 req/s | 7,496 | 0 | 0.00% | 4.32ms | 20.70ms | 0.00% |

## 结论

- 单热点票档下，默认热点保护稳定触发，`87.56%` 的请求被快速拒绝为 `C0429`，p95 仍保持在 `3.23ms`。
- 分散到 20 个票档后，每个票档的热点压力下降，本轮没有出现 `C0429`，受理数从 `1,085` 提升到 `7,496`。
- 两组都没有 HTTP 失败和非预期响应，说明脚本分类、限流兜底和核心链路返回都稳定。
- 多票档分摊不是替代全局限流，而是降低单个 `skuId` 热点参数限流触发概率。真实生产中仍要同时保留全局 QPS、热点参数和 Redis 准入门。

## 原始数据

原始 k6 summary JSON 位于本地 `target/multi-sku-comparison/`：

- `single-sku.json`
- `multi-sku-20.json`

`target/` 为本地构建和压测产物目录，不提交到 Git。
