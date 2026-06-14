# Sentinel Dashboard 动态规则演示

## 目标

演示抢票入口在运行时通过 Sentinel Dashboard 调整限流规则，不重启应用即可观察 `C0429` 限流响应变化。

当前项目已经具备：

- Sentinel Dashboard：`docker-compose.yml` 中的 `sentinel-dashboard`，地址 `http://localhost:8858`。
- 应用接入配置：`spring.cloud.sentinel.transport.dashboard=localhost:8858`，客户端端口 `8719`。
- 抢票资源：
  - `ticketrush:rush:ticket`
  - `ticketrush:rush:ticket:sku`

## 启动

```bash
docker compose up -d redis sentinel-dashboard
```

启动应用后，先打一次抢票接口，让 Sentinel Dashboard 发现应用和资源：

```http
POST /api/rush/inventory/preload
Content-Type: application/json

{
  "skuId": 1001,
  "totalStock": 1000000
}
```

```http
POST /api/rush/tickets
Content-Type: application/json

{
  "requestId": "sentinel-demo-001",
  "userId": 2001,
  "eventId": 3001,
  "skuId": 1001,
  "quantity": 1,
  "strategy": "REDIS_LUA"
}
```

打开 `http://localhost:8858`，默认账号密码通常为 `sentinel` / `sentinel`。在应用列表中选择 `ticketrush`。

## 全局抢票限流

在 Flow Control 页面新增或修改规则：

| 字段 | 值 |
| --- | --- |
| Resource | `ticketrush:rush:ticket` |
| Grade | QPS |
| Threshold | `20` |
| Flow control behavior | Default |

样例见 [rush-flow-rule.json](../scripts/sentinel/rush-flow-rule.json)。

验证：

```powershell
k6 run `
  -e SCENARIO_TAG=sentinel-flow `
  -e VUS=200 `
  -e DURATION=30s `
  scripts/k6/stability-governance.js
```

预期：`rush_rate_limited` 增加，接口返回中出现 `C0429`，Redis 和订单消息链路不会被全部流量打满。

## 热点票档限流

在 Hotspot Rules 页面新增或修改规则：

| 字段 | 值 |
| --- | --- |
| Resource | `ticketrush:rush:ticket:sku` |
| Param index | `0` |
| Grade | QPS |
| Single threshold | `10` |
| Statistical window | `1s` |

样例见 [hot-sku-param-flow-rule.json](../scripts/sentinel/hot-sku-param-flow-rule.json)。

验证单票档热点：

```powershell
k6 run `
  -e SCENARIO_TAG=sentinel-hot-sku `
  -e SKU_SPREAD=1 `
  -e VUS=200 `
  -e DURATION=30s `
  scripts/k6/stability-governance.js
```

验证多票档分散流量：

```powershell
k6 run `
  -e SCENARIO_TAG=sentinel-hot-sku-spread `
  -e SKU_SPREAD=20 `
  -e VUS=200 `
  -e DURATION=30s `
  scripts/k6/stability-governance.js
```

预期：`SKU_SPREAD=1` 时 `C0429` 更明显；多票档分散后热点限流压力下降。

## 注意事项

- Dashboard 修改的是运行时内存规则，应用重启后会恢复为 `application.yml` 中的本地规则。
- 生产环境应把规则持久化到 Nacos、Apollo 或其他动态数据源。
- 本地演示前需要先产生一次真实请求，否则 Dashboard 可能还看不到资源。
