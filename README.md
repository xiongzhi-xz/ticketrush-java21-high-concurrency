# TicketRush

TicketRush 是一个基于 Java 21 的高并发票务秒杀系统，用于模拟演出、景区、剧院、年卡等票务场景中的高并发抢票链路。

项目目标不是做一个简单 CRUD，而是完整展示生产级高并发系统的关键能力：抢票入口、库存防超卖、幂等控制、限流降级、异步削峰、分布式事务、可观测性、压测和本地部署。

## 技术栈

- Java 21
- Spring Boot 3
- Spring Cloud Alibaba
- Virtual Threads
- Structured Concurrency
- MySQL
- Redis
- RocketMQ
- Nacos
- Sentinel
- Seata
- Elasticsearch
- Docker Compose
- Kubernetes/K3s
- Prometheus
- Grafana
- Arthas

## 核心目标

- 高并发抢票核心接口：在真实业务链路中使用 Java 21 Virtual Threads。
- 防超卖方案对比：Redis Lua、Redis 分布式锁、MySQL 乐观锁。
- 流量治理：Sentinel 限流、热点参数保护、降级兜底。
- 异步削峰：RocketMQ 解耦抢票入口和订单创建。
- 一致性保障：幂等、补偿、订单超时关闭、Seata 示例。
- 可观测性：Actuator、Prometheus、Grafana、Arthas。
- 压测报告：Virtual Threads 与传统线程池的 QPS、CPU、内存、GC 对比。

## 架构分层

```text
com.ticketrush
├─ common          # 通用响应、异常、错误码、工具、ID
├─ config          # Web、虚拟线程、Redis、MQ、Sentinel、Seata、监控配置
├─ interfaces      # Controller、Request、Response
├─ application     # 应用服务、用例编排、命令对象、DTO
├─ domain          # 领域模型、领域服务、仓储接口
├─ infrastructure  # MySQL、Redis、MQ、ES、Seata、Sentinel 适配
└─ job             # 定时任务、补偿任务、库存预热、订单关闭
```

## 本地环境

### 基础要求

- JDK 21
- Maven 3.9+
- Docker Desktop 或兼容 Docker Compose 的本地环境

当前项目使用 Java 21，并为 Structured Concurrency 预览能力保留 `--enable-preview` 编译参数。请使用 JDK 21 进行完整编译和测试。

### 启动中间件

```bash
docker compose up -d
```

本地服务端口：

| 组件 | 地址 |
| --- | --- |
| MySQL | `localhost:3306` |
| Redis | `localhost:6379` |
| Nacos | `http://localhost:8848` |
| Sentinel Dashboard | `http://localhost:8858` |
| RocketMQ NameServer | `localhost:9876` |
| Seata | `localhost:8091` |
| Elasticsearch | `http://localhost:9200` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

### Maven 校验

```bash
mvn clean verify
```

如果当前机器不是 JDK 21，项目中的 Java 版本门禁会阻止构建。这是有意设计，用于保证项目最终围绕 Java 21 能力交付。

## 当前进度

详见 [SPEC.md](./SPEC.md)。

已完成：

- Maven 基础配置
- Spring Boot 配置文件
- Docker Compose 本地中间件环境
- Git 仓库初始化
- 基础包结构和启动类
- README 初稿
- 统一响应、错误码和全局异常处理
- 虚拟线程基础配置
- 系统健康检查接口
- 参数校验示例接口
- 票务活动、票档、库存、订单领域模型
- 库存仓储接口、MyBatis Mapper 边界
- Redis 库存 Key 和 Lua 预占脚本
- 库存领域服务和单元测试
- Redis Lua 真实库存预占适配器
- Redis 分布式锁库存扣减适配器
- MySQL 乐观锁库存扣减适配器
- 抢票核心接口
- 本地库存预热接口
- 抢票应用服务单元测试
- 库存扣减策略适配器单元测试
- 虚拟线程 vs 传统线程池对比接口
- 第一版 k6 抢票压测脚本
- RocketMQ 异步订单创建消息
- 订单创建消费者
- 订单消费幂等
- 订单超时关闭任务
- 锁定库存释放补偿
- 最终一致性说明文档
- Sentinel 全局抢票限流
- Sentinel 热点票档参数限流
- 限流兜底响应
- Redis 抢票准入令牌门禁
- 热点库存自动预热 Runner
- 准入令牌与预热任务单元测试
- 稳定性治理 k6 压测脚本和记录模板
- Sentinel Dashboard 动态规则演示文档和样例
- Prometheus 抓取配置和 Grafana 自动面板
- Arthas 抢票链路诊断案例

下一步：

- 使用 JDK 21 完整验证应用启动
- 确认数据库表结构后创建 schema
- 实现 MyBatis XML 或注解 SQL
- 使用 Redis 运行 Lua 和分布式锁集成测试
- 使用 MySQL 运行乐观锁集成测试
- 使用 k6 对三种库存策略跑第一轮本地压测
- 补充 RocketMQ 集成测试
- 补充 Seata 示例
- 对限流前后做 k6 稳定性测试

## 基础接口

### 健康检查

```http
GET /api/system/health
```

返回应用名、Java 版本、虚拟线程开关、当前请求线程是否为虚拟线程等信息。

### 参数校验示例

```http
POST /api/system/validation-check
Content-Type: application/json

{
  "requestId": "bench-001",
  "scenario": "virtual-thread-health-check",
  "concurrency": 1000
}
```

该接口用于验证统一参数校验和全局异常响应格式，后续业务接口沿用同样的校验方式。

### 本地库存预热

```http
POST /api/rush/inventory/preload
Content-Type: application/json

{
  "skuId": 1001,
  "totalStock": 100
}
```

该接口用于本地开发和压测前把票档库存写入 Redis Hash。生产环境应改为后台任务或管控系统触发。

### 抢票

```http
POST /api/rush/tickets
Content-Type: application/json

{
  "requestId": "req-001",
  "userId": 2001,
  "eventId": 3001,
  "skuId": 1001,
  "quantity": 1,
  "strategy": "REDIS_LUA"
}
```

当前抢票链路会通过应用服务把库存预占提交到虚拟线程执行。响应中的 `processedByVirtualThread` 用于确认本次库存预占是否由虚拟线程处理。

`strategy` 可选，默认值为 `REDIS_LUA`：

| 策略 | 说明 |
| --- | --- |
| `REDIS_LUA` | Redis Lua 原子扣减，主推荐方案 |
| `REDIS_LOCK` | Redis 分布式锁扣减，用于和 Lua 方案对比 |
| `MYSQL_OPTIMISTIC_LOCK` | MySQL 乐观锁扣减，用于数据库热点写冲突对比 |

常见错误码：

| 错误码 | 含义 |
| --- | --- |
| `A0429` | 重复请求 |
| `B0401` | 库存不足 |
| `B0402` | 库存未预热、锁竞争失败、版本冲突或扣减失败 |
| `C0429` | Sentinel 或准入令牌限流 |
| `C0503` | 库存预占超时或执行失败 |

### 执行器对比

```http
POST /api/benchmark/executors
Content-Type: application/json

{
  "mode": "VIRTUAL_THREAD",
  "taskCount": 10000,
  "blockingMillis": 50,
  "cpuTokens": 0,
  "timeoutSeconds": 60
}
```

`mode` 可选：

| 模式 | 说明 |
| --- | --- |
| `VIRTUAL_THREAD` | 使用 Java 21 虚拟线程执行器 |
| `TRADITIONAL_THREAD_POOL` | 使用固定平台线程池 |

该接口用于构造相同的阻塞任务，对比两种执行器的耗时、吞吐、参与线程数和虚拟线程任务数。

## 领域模型进度

当前已建立四个核心领域模型：

- `TicketEvent`：票务活动，控制整体售卖窗口。
- `TicketSku`：票档，控制价格、票档售卖窗口和总库存。
- `TicketInventory`：库存，维护可售、锁定、已售三段库存。
- `TicketOrder`：订单，维护幂等键、过期时间和订单状态。

库存模型遵守以下不变量：

```text
totalStock = availableStock + lockedStock + soldStock
```

抢票阶段会先把可售库存转入锁定库存；订单创建成功后转入已售库存；订单失败或超时后释放回可售库存。

## Redis 库存结构

库存以票档为粒度缓存为 Redis Hash：

```text
ticketrush:inventory:{skuId}
├─ total
├─ available
├─ locked
├─ sold
└─ version
```

Lua 脚本位置：

```text
src/main/resources/lua/reserve_stock.lua
```

脚本负责原子完成库存检查、库存预占、版本递增和幂等 Key 写入。

## 防超卖方案对比

当前已完成三种库存预占代码路径：

| 方案 | 一致性手段 | 优点 | 风险 |
| --- | --- | --- | --- |
| Redis Lua | 单线程执行 Lua 脚本 | 原子性强、网络往返少、适合热点票档 | 脚本复杂后维护成本上升 |
| Redis Lock | 票档粒度分布式锁 | 容易理解、便于对比 | 锁竞争激烈时吞吐下降 |
| MySQL Optimistic Lock | `version` + 条件更新 | 不依赖 Redis 库存缓存 | 热点行冲突高，数据库压力大 |

MySQL 乐观锁方案当前已完成 Java 代码和 Mapper 边界，真实运行还需要确认表结构并补充 MyBatis SQL。

## 异步下单

抢票成功后的链路：

```text
/api/rush/tickets
  -> 库存预占
  -> 发布 OrderCreateMessage
  -> RocketMQ Topic: ticketrush-order-create-topic
  -> orderCreateConsumer
  -> OrderApplicationService
  -> TicketOrderRepository
```

当前订单创建为 `PENDING` 状态，库存保持锁定。订单超时关闭任务会释放未支付订单的锁定库存。

消费幂等：

- 幂等键：`idempotentKey`
- 重复消息：直接跳过，不重复创建订单
- 消费失败：抛出异常，交给 RocketMQ/Spring Cloud Stream 重试
- 发送失败：抢票入口释放已预占库存并返回服务繁忙

订单超时关闭：

- 定时任务：`OrderTimeoutCloseJob`
- 配置前缀：`ticketrush.order.timeout-close`
- 行为：批量扫描已过期 `PENDING` 订单，原子关闭成功后释放锁定库存

最终一致性说明详见 [docs/final-consistency.md](./docs/final-consistency.md)。

## 稳定性治理

抢票入口已接入 Sentinel：

- 全局资源：`ticketrush:rush:ticket`
- 热点票档资源：`ticketrush:rush:ticket:sku`
- 热点参数：`skuId`
- 限流错误码：`C0429`

本地规则配置：

```yaml
ticketrush:
  sentinel:
    enabled: true
    rush-qps: 1000
    hotspot-sku-qps: 100
    hotspot-duration-seconds: 1
    hotspot-burst-count: 20
```

Sentinel 放行后，抢票链路还会进入 Redis 准入令牌门禁，用于限制同一票档同时进入库存扣减链路的请求数：

```yaml
ticketrush:
  rush:
    admission:
      enabled: true
      max-in-flight-per-sku: 500
      token-ttl: 10s
```

热点库存可在应用启动时自动预热，默认关闭，适合本地演示或压测准备：

```yaml
ticketrush:
  rush:
    hot-inventory-preload:
      enabled: false
      items:
        - sku-id: 1001
          total-stock: 100000
```

说明详见 [docs/stability-governance.md](./docs/stability-governance.md)。

## 压测脚本

第一版 k6 抢票压测脚本：

```bash
k6 run scripts/k6/rush-ticket.js
```

运行前需要先安装 k6，并启动应用与 Redis。

常用环境变量：

```powershell
k6 run `
  -e BASE_URL=http://localhost:8080 `
  -e STRATEGY=REDIS_LUA `
  -e VUS=200 `
  -e DURATION=60s `
  -e STOCK=100000 `
  scripts/k6/rush-ticket.js
```

`STRATEGY` 可设置为 `REDIS_LUA`、`REDIS_LOCK` 或 `MYSQL_OPTIMISTIC_LOCK`，用于对比三种防超卖方案。脚本启动时会先调用库存预热接口，再压测抢票接口。

稳定性治理压测脚本：

```powershell
k6 run `
  -e BASE_URL=http://localhost:8080 `
  -e SCENARIO_TAG=governed `
  -e SKU_SPREAD=1 `
  -e VUS=500 `
  -e DURATION=60s `
  scripts/k6/stability-governance.js
```

该脚本会统计 `rush_rate_limited`、`rush_accepted`、`rush_service_degraded`、`unexpected_response_rate` 等指标，用于对比 Sentinel 和 Redis 准入门开启前后的效果。记录模板见 [docs/stability-benchmark.md](./docs/stability-benchmark.md)。

Sentinel Dashboard 动态规则演示见 [docs/sentinel-dashboard-demo.md](./docs/sentinel-dashboard-demo.md)，规则样例位于 `scripts/sentinel/`。

## 可观测性

Prometheus 会抓取本机应用的 `/actuator/prometheus`：

```text
docker/prometheus/prometheus.yml
```

Grafana 会自动配置 Prometheus 数据源和 `TicketRush Overview` 面板：

```text
docker/grafana/provisioning
docker/grafana/dashboards/ticketrush-overview.json
```

说明详见 [docs/observability.md](./docs/observability.md)。

Arthas 抢票链路诊断案例见 [docs/arthas-diagnostics.md](./docs/arthas-diagnostics.md)。

## 文档规划

后续会逐步补齐：

- 架构图
- 压测报告
- Virtual Threads 使用说明
- 防超卖方案对比
- Sentinel 限流示例
- RocketMQ 异步下单说明
- Seata 示例
- Arthas 诊断案例
- Kubernetes/K3s 部署说明
- 踩坑记录
