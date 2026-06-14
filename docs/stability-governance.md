# TicketRush 稳定性治理说明

## 当前治理点

TicketRush 当前已在抢票主链路前接入 Sentinel：

```text
/api/rush/tickets
  -> RushTrafficGuard
  -> Sentinel 全局抢票资源
  -> Sentinel 热点票档资源
  -> RushAdmissionGuard
  -> Redis 准入令牌
  -> 库存预占
  -> RocketMQ 异步下单
```

## Sentinel 资源

| 资源名 | 保护目标 |
| --- | --- |
| `ticketrush:rush:ticket` | 抢票接口整体 QPS |
| `ticketrush:rush:ticket:sku` | 以 `skuId` 为热点参数保护热门票档 |

## 本地规则

配置位置：`src/main/resources/application.yml`

```yaml
ticketrush:
  sentinel:
    enabled: true
    rush-qps: 1000
    hotspot-sku-qps: 100
    hotspot-duration-seconds: 1
    hotspot-burst-count: 20
```

## Redis 准入令牌

Sentinel 放行后，`RushAdmissionGuard` 会按 `skuId` 获取 Redis 准入令牌，限制同一票档同时进入库存扣减链路的请求数。

Key：

```text
ticketrush:rush:admission:{skuId}
```

Lua 脚本：

```text
src/main/resources/lua/acquire_admission_token.lua
src/main/resources/lua/release_admission_token.lua
```

配置：

```yaml
ticketrush:
  rush:
    admission:
      enabled: true
      max-in-flight-per-sku: 500
      token-ttl: 10s
```

准入失败同样映射为：

```text
ErrorCode.RATE_LIMITED -> C0429
```

## 热点库存自动预热

本地演示或压测前可以打开启动预热，把指定票档库存写入 Redis Hash。默认关闭，避免应用启动强依赖 Redis 数据初始化。

```yaml
ticketrush:
  rush:
    hot-inventory-preload:
      enabled: false
      items:
        - sku-id: 1001
          total-stock: 100000
```

## 降级兜底

当 Sentinel 或 Redis 准入令牌拒绝请求时，系统抛出业务异常：

```text
ErrorCode.RATE_LIMITED -> C0429
```

HTTP 响应会保持统一响应格式，调用方可以根据错误码判断是限流而不是库存不足或系统异常。

## 面试讲解点

- 全局限流保护应用实例不被瞬时流量打穿。
- 热点参数限流按 `skuId` 保护热门票档，避免单个票档拖垮整个抢票服务。
- 限流和准入发生在库存扣减前，避免无意义地打 Redis、MySQL 或 RocketMQ。
- Redis 准入令牌控制进入核心链路的并发数，用于平滑热门活动开售瞬时洪峰。
- 当前规则是本地静态规则，生产环境可以迁移到 Nacos 动态规则。

## 后续增强

- Sentinel Dashboard 动态规则演示见 [sentinel-dashboard-demo.md](./sentinel-dashboard-demo.md)。
- 用 k6 对限流前后 QPS、失败率、Redis 压力做对比记录，记录模板见 [stability-benchmark.md](./stability-benchmark.md)。
