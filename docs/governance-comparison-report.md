# TicketRush 稳定性治理 before/after 对照报告

## 目标

本报告验证 TicketRush 的入口治理是否真的能保护热点票档抢票链路。

对照方式：

- **Protected**：使用当前 `ticketrush-app`，默认开启 Sentinel 全局限流、Sentinel 热点票档限流、Redis 准入令牌。
- **No Guard**：启动临时容器 `ticketrush-app-noguard`，通过环境变量关闭 Sentinel 规则和 Redis 准入门。

关闭项：

```text
TICKETRUSH_SENTINEL_ENABLED=false
TICKETRUSH_RUSH_ADMISSION_ENABLED=false
```

临时容器只用于本轮对照，测试后已删除。

## 环境

| 项 | 值 |
| --- | --- |
| Date | 2026-06-17 |
| Commit | `91d0ba9` 后的工作区 |
| App | Docker Compose `ticketrush-app` + 临时 `ticketrush-app-noguard` |
| Java | `21.0.11+10-LTS` |
| k6 | Docker image `grafana/k6` |
| Network | `ticketrush-local_ticketrush` |
| Strategy | `REDIS_LUA` |

测试前后主服务 `/api/system/health` 均为 `UP`。

原始 summary JSON 生成在 `target/governance-comparison/`，该目录属于本地构建产物，不提交到 git。

## 参数

两组使用同一份 k6 脚本和同一组参数：

```powershell
docker run --rm `
  --network ticketrush-local_ticketrush `
  -v "<repo>/scripts/k6:/scripts:ro" `
  -v "<repo>/target/governance-comparison:/results" `
  grafana/k6 run `
  --summary-export /results/<result>.json `
  -e BASE_URL=<base-url> `
  -e STRATEGY=REDIS_LUA `
  -e SCENARIO_TAG=<scenario> `
  -e SKU_SPREAD=1 `
  -e VUS=10 `
  -e DURATION=10s `
  -e STOCK=1000000 `
  -e SLEEP=0.01 `
  /scripts/stability-governance.js
```

`SKU_SPREAD=1` 表示所有流量打向同一个热门票档。默认热点票档限流约为 100 QPS，测试流量约 700+ req/s，足以触发保护。

## 结果

| Scenario | Requests | Req/s | Accepted | Rate limited | Rate-limit ratio | Avg | P95 | Max | Unexpected | HTTP failed |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Protected | 8,708 | 741.53 | 1,197 | 7,510 | 86.25% | 1.00ms | 3.21ms | 16.52ms | 0.00% | 0.00% |
| No Guard | 7,502 | 724.48 | 7,501 | 0 | 0.00% | 2.75ms | 4.94ms | 350.89ms | 0.00% | 0.00% |

## 结论

- 默认治理开启时，约 `86.25%` 的单热点流量被快速拒绝为 `C0429`，只有约 1,197 次进入抢票核心链路。
- 关闭治理后，几乎所有请求都进入核心链路，受理数从 1,197 增加到 7,501。
- 关闭治理后 p95 从 3.21ms 增加到 4.94ms，最大延迟从 16.52ms 增加到 350.89ms。
- 两组非预期响应均为 0，说明业务错误码、脚本分类和接口响应口径一致。

这说明 Sentinel 热点票档保护和 Redis 准入门的价值不是“提高受理数”，而是在热点洪峰下主动牺牲一部分入口流量，保护 Redis、RocketMQ、MySQL 和订单消费链路的可控性。

## 设计说明

```text
我用同样的 k6 流量对比了治理开启和关闭两种状态。10 VU、单热点 SKU、10 秒压测下，默认治理开启时 8708 个请求里有 7510 个被限流，只有 1197 个进入核心链路；关闭 Sentinel 规则和 Redis 准入门后，7501 个请求几乎全部进入核心链路，p95 和最大延迟都上升。这个结果说明入口治理不是为了让所有请求都成功，而是为了在热点洪峰时把系统压在可控范围内。
```

## 边界

- 本报告是本地 Docker Compose 环境的对照实验，不等价于生产容量结论。
- 本轮只验证默认保护开启与关闭对照，没有进一步调参 `rush-qps`、`hotspot-sku-qps`、`max-in-flight-per-sku`。
- 后续可结合 Prometheus/Grafana 导出 Redis、RocketMQ、MySQL、JVM 指标，补充下游资源视角。
