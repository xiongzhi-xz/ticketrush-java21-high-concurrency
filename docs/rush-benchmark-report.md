# TicketRush k6 本地压测报告

## 目标

本轮压测用于给 TicketRush 补第一份真实本地数据，重点验证两件事：

- 三种库存扣减策略在默认治理配置下的低负载基线表现。
- 单热点票档被高频请求冲击时，Sentinel 热点参数限流和 Redis 准入门是否能把无效流量挡在核心链路外。

这不是生产性能结论，而是本机 Docker Compose 环境下的可复现实测记录。

## 环境

| 项 | 值 |
| --- | --- |
| Date | 2026-06-17 |
| Commit | `c264471` 后的工作区 |
| App | Docker Compose `ticketrush-app` |
| Java | `21.0.11+10-LTS` |
| Spring profile | `docker` |
| k6 | Docker image `grafana/k6` |
| Network | `ticketrush-local_ticketrush` |
| Base URL | `http://app:8080` |

说明：

- 本机未安装 k6，使用 Dockerized k6 执行。
- 原始 summary JSON 生成在 `target/k6/`，该目录属于本地构建产物，不提交到 git。
- k6 脚本已把 `409/429/503` 注册为 expected status，因为这些是可解释的业务响应，不应按传输失败统计。

## 启动状态

测试前应用健康检查：

```text
GET /api/system/health
status=UP
javaVersion=21.0.11+10-LTS
virtualThreadsEnabled=true
currentThreadVirtual=true
```

Docker Compose 中应用、MySQL、Redis、Nacos、RocketMQ、Seata、Elasticsearch、Prometheus、Grafana 均处于运行状态。

## 三策略低负载基线

### 参数

```powershell
docker run --rm `
  --network ticketrush-local_ticketrush `
  -v "<repo>/scripts/k6:/scripts:ro" `
  -v "<repo>/target/k6:/results" `
  grafana/k6 run `
  --summary-export /results/<result>.json `
  -e BASE_URL=http://app:8080 `
  -e STRATEGY=<strategy> `
  -e VUS=2 `
  -e DURATION=20s `
  -e STOCK=1000000 `
  -e SLEEP=0.03 `
  /scripts/rush-ticket.js
```

脚本包含 10s ramp-up、20s steady、10s ramp-down。该组故意低于默认热点票档限流线，用于观察库存策略本身的相对延迟。

MySQL 乐观锁策略额外写入了本地压测种子数据：`ticket_event=3001`、`ticket_sku=1001`、`ticket_inventory=1000000`。只写本地 Docker MySQL，不改 schema。

### 结果

| Strategy | Requests | Req/s | Iter/s | Avg | Median | P90 | P95 | Max | HTTP failed |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `REDIS_LUA` | 1,963 | 45.40 | 45.37 | 2.70ms | 2.33ms | 3.33ms | 4.09ms | 67.70ms | 0.00% |
| `REDIS_LOCK` | 1,919 | 44.26 | 44.24 | 3.34ms | 2.76ms | 4.21ms | 5.72ms | 84.12ms | 0.00% |
| `MYSQL_OPTIMISTIC_LOCK` | 1,827 | 39.15 | 39.13 | 7.30ms | 4.59ms | 6.42ms | 7.98ms | 3905.70ms | 0.00% |

### 结论

- `REDIS_LUA` 延迟最低，单脚本原子扣减路径最短，适合作为热点票档主方案。
- `REDIS_LOCK` 在低负载下吞吐接近 Redis Lua，但 p95 更高，说明加锁/解锁路径有可见成本。
- `MYSQL_OPTIMISTIC_LOCK` p95 仍可控，但平均延迟更高，并出现一次 3.9s 长尾；热点库存不应优先压到 MySQL 行锁/版本冲突路径。
- 本组是低负载 baseline，不代表高压极限。默认 Sentinel 热点票档 QPS 为 100，高压单 SKU 会优先进入限流路径。

## 热点票档治理观察

### 参数

```powershell
docker run --rm `
  --network ticketrush-local_ticketrush `
  -v "<repo>/scripts/k6:/scripts:ro" `
  -v "<repo>/target/k6:/results" `
  grafana/k6 run `
  --summary-export /results/governed-hot-sku-vus30.json `
  -e BASE_URL=http://app:8080 `
  -e STRATEGY=REDIS_LUA `
  -e SCENARIO_TAG=guarded-vus30 `
  -e SKU_SPREAD=1 `
  -e VUS=30 `
  -e DURATION=20s `
  -e STOCK=1000000 `
  /scripts/stability-governance.js
```

### 结果

| Metric | Value |
| --- | ---: |
| Total requests | 833,629 |
| Request rate | 35,765.45 req/s |
| `rush_accepted` | 2,023 |
| `rush_rate_limited` | 831,605 |
| Unexpected response rate | 0.00% |
| HTTP failed | 0.00% |
| HTTP p95 | 1.18ms |
| Max latency | 3938.75ms |

### 结论

- 单热点票档在 30 VU、无 sleep 的冲击下，大部分流量被稳定归类为 `C0429`。
- 非预期响应为 0，说明限流、业务错误码和脚本分类口径一致。
- p95 很低是因为绝大多数请求在入口被快速拒绝，不代表库存扣减链路承受了 35k req/s。
- 这轮证明了默认保护生效；严格的“限流前后对比”仍需要单独调低/关闭治理配置后再跑一组对照。

## 注意事项

- k6 原始输出中高压场景出现过一次负数 `min` 的时钟统计异常，因此本报告不使用 `min` 作为结论依据。
- Docker Desktop、本机 CPU/内存、容器资源限制都会影响结果；本报告只用于项目展示和方案解释。
- 订单异步链路会写入本地 MySQL 订单数据，属于本地演示环境副作用。

## 下一步

- 跑 `POST /api/benchmark/executors`，补 Virtual Threads vs traditional thread pool 对比报告。
- 做稳定性治理 before/after 对照：默认保护、调低阈值、关闭准入门三组对比。
- 用 Prometheus/Grafana 截取压测期间指标，补充 CPU、线程和 HTTP p95 视角。
