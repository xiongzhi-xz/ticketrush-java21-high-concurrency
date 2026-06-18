# TicketRush Prometheus/Grafana 指标证据报告

## 目标

本报告给 TicketRush 补一份压测期间的服务端指标视角，用 Prometheus API 导出与 Grafana Dashboard 同口径的数据，验证压测时不仅接口有结果，监控链路也能看到 HTTP、CPU、Heap、线程和连接池状态。

## 环境

| 项 | 值 |
| --- | --- |
| Date | 2026-06-18 |
| Commit | `4452867` 后的工作区 |
| App | Docker Compose `ticketrush-app` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |
| Scrape interval | 15s |
| Prometheus target | `app:8080/actuator/prometheus` |
| k6 | Docker image `grafana/k6` |

Prometheus target 状态：

```text
job=ticketrush-app
instance=app:8080
health=up
lastError=""
```

## 压测参数

```powershell
docker run --rm `
  --network ticketrush-local_ticketrush `
  -v "<repo>/scripts/k6:/scripts:ro" `
  -v "<repo>/target/prometheus-evidence:/results" `
  grafana/k6 run `
  --summary-export /results/prometheus-protected-vus10-60s.json `
  -e BASE_URL=http://app:8080 `
  -e STRATEGY=REDIS_LUA `
  -e SCENARIO_TAG=prometheus-vus10 `
  -e SKU_SPREAD=1 `
  -e VUS=10 `
  -e DURATION=60s `
  -e STOCK=1000000 `
  -e SLEEP=0.01 `
  /scripts/stability-governance.js
```

原始 k6 summary 和 Prometheus CSV 导出在 `target/prometheus-evidence/`，属于本地构建产物，不提交到 git。

## k6 入口结果

| Metric | Value |
| --- | ---: |
| HTTP requests | 52,173 |
| Request rate | 838.88 req/s |
| Accepted | 7,167 |
| Rate limited | 45,005 |
| Unexpected response rate | 0.00% |
| HTTP failed | 0.00% |
| k6 p95 | 3.24ms |
| Max latency | 25.55ms |

## Prometheus 指标摘要

Prometheus 查询窗口覆盖本轮 k6 run，并在结束后额外等待一次 scrape。

| Metric | PromQL | Samples | Avg | Max | Last |
| --- | --- | ---: | ---: | ---: | ---: |
| Total RPS | `sum(rate(http_server_requests_seconds_count{application="ticketrush",instance="app:8080",uri="/api/rush/tickets"}[1m]))` | 7 | 458.20 | 828.63 | 422.71 |
| Accepted RPS | `sum(rate(http_server_requests_seconds_count{application="ticketrush",instance="app:8080",uri="/api/rush/tickets",status="200"}[1m]))` | 7 | 62.97 | 114.71 | 59.16 |
| Rate-limited RPS | `sum(rate(http_server_requests_seconds_count{application="ticketrush",instance="app:8080",uri="/api/rush/tickets",status="429"}[1m]))` | 7 | 395.23 | 713.92 | 363.55 |
| HTTP p95 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="ticketrush",instance="app:8080",uri="/api/rush/tickets"}[1m])) by (le))` | 6 | 0.0030s | 0.0031s | 0.0031s |
| Process CPU | `process_cpu_usage{application="ticketrush",instance="app:8080"}` | 7 | 0.0125 | 0.0215 | 0.0011 |
| Heap Used | `sum(jvm_memory_used_bytes{application="ticketrush",instance="app:8080",area="heap"})` | 7 | 294,020,820 bytes | 466,801,296 bytes | 122,828,448 bytes |
| Live Threads | `jvm_threads_live_threads{application="ticketrush",instance="app:8080"}` | 7 | 480.14 | 481 | 481 |
| Hikari Active | `hikaricp_connections_active{application="ticketrush",instance="app:8080"}` | 7 | 0 | 0 | 0 |
| Hikari Pending | `hikaricp_connections_pending{application="ticketrush",instance="app:8080"}` | 7 | 0 | 0 | 0 |
| Redis command rate | `sum(rate(lettuce_command_completion_seconds_count{application="ticketrush",instance="app:8080"}[1m]))` | 7 | 251.90 | 458.85 | 236.65 |

## 结论

- Prometheus 抓到了与 k6 接近的峰值入口流量：k6 为 838.88 req/s，Prometheus 1m rate 峰值为 828.63 req/s。
- HTTP p95 在 Prometheus 侧约 3.1ms，与 k6 p95 3.24ms 接近，说明客户端和服务端统计口径一致。
- 单热点压测下大部分请求被 `429` 快速拒绝，Prometheus 中 rate-limited RPS 峰值 713.92，高于 accepted RPS 峰值 114.71。
- Process CPU 峰值约 2.15%，说明默认治理把大部分流量挡在入口，未把服务端 CPU 打满。
- Hikari active/pending 均为 0，说明这轮 `REDIS_LUA` + 入口限流压测没有把 MySQL 连接池打出压力。
- Redis command rate 峰值 458.85/s，能看到 Redis 参与了准入令牌和库存链路。

## Grafana 对应面板

项目已预置 `TicketRush Overview` Dashboard：

| Panel | Query |
| --- | --- |
| HTTP RPS | `sum(rate(http_server_requests_seconds_count{application="ticketrush"}[1m]))` |
| HTTP p95 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="ticketrush"}[5m])) by (le))` |
| Process CPU | `process_cpu_usage{application="ticketrush"}` |
| JVM Heap Used | `sum(jvm_memory_used_bytes{application="ticketrush",area="heap"})` |
| JVM Live Threads | `jvm_threads_live_threads{application="ticketrush"}` |

这份报告使用更窄的 `instance="app:8080"` 和 `uri="/api/rush/tickets"` 过滤条件，适合压测复盘；Grafana 面板适合观察全局趋势。

## 边界

- 本报告没有提交截图，使用 Prometheus API 导出的数值作为可复现证据。
- 这轮只覆盖默认治理开启下的单热点压测，没有导出 RocketMQ、MySQL 和 Redis 容器自身的系统指标。
- 后续如果要做更像生产的容量评估，需要延长压测时间并补充容器 CPU、内存、磁盘和网络指标。
