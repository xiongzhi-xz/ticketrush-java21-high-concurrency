# TicketRush 稳定性压测记录

## 目标

用同一套 k6 脚本对比限流/准入策略开启前后的表现，重点观察：

- `C0429` 是否稳定吸收瞬时洪峰。
- `p95` 延迟是否在保护策略开启后保持可控。
- Redis、MySQL、RocketMQ 是否避免被无效流量打满。
- 抢票成功、库存不足、服务降级、非预期响应的分布是否清晰。

## 脚本

```bash
k6 run scripts/k6/stability-governance.js
```

常用参数：

```powershell
k6 run `
  -e BASE_URL=http://localhost:8080 `
  -e SCENARIO_TAG=governed `
  -e STRATEGY=REDIS_LUA `
  -e SKU_ID=1001 `
  -e SKU_SPREAD=1 `
  -e STOCK=1000000 `
  -e VUS=500 `
  -e DURATION=60s `
  scripts/k6/stability-governance.js
```

`SKU_SPREAD=1` 用于制造单票档热点；调大后可以观察全局限流与多票档流量分摊效果。

## 指标

| 指标 | 含义 |
| --- | --- |
| `rush_accepted` | 抢票入口成功受理数量 |
| `rush_rate_limited` | Sentinel 或 Redis 准入令牌返回 `C0429` 的数量 |
| `rush_stock_not_enough` | 库存不足或扣减失败数量 |
| `rush_service_degraded` | 服务降级或核心链路超时数量 |
| `rush_unexpected_responses` | 未归类响应数量 |
| `unexpected_response_rate` | 未归类响应占比，脚本阈值要求 `< 1%` |
| `http_req_duration` | HTTP 延迟分布，脚本默认要求 `p95 < 1000ms` |

## 推荐对比矩阵

| 场景 | 配置 | 目的 |
| --- | --- | --- |
| 基线 | 降低 VUS 或关闭准入门 | 获取无强保护时的入口表现 |
| Sentinel 全局限流 | 调低 `ticketrush.sentinel.rush-qps` | 验证整体 QPS 被稳定截断 |
| 热点票档限流 | `SKU_SPREAD=1`，调低 `hotspot-sku-qps` | 验证单票档热点被保护 |
| Redis 准入门 | 调低 `max-in-flight-per-sku` | 验证进入库存扣减链路的并发被限制 |

## 记录模板

| 日期 | Commit | 场景 | VUS | Duration | QPS/吞吐 | `C0429` 占比 | p95 | 非预期响应 | 结论 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 待补充 | 待补充 | 待补充 | 待补充 | 待补充 | 待补充 | 待补充 | 待补充 | 待补充 | 待补充 |

## 当前状态

已补充稳定性 k6 脚本和记录模板。当前开发机尚未安装 k6，真实压测数据待在具备 k6、JDK 21、Redis、应用服务的环境中执行后补充。
