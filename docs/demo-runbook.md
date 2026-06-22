# TicketRush 演示 Runbook

这份文档用于快速熟悉 TicketRush 的本地演示路径。目标是把一个高并发票务秒杀系统说明为可运行、可压测、可解释的工程闭环。

## 一句话定位

TicketRush 是一个 Java 21 高并发票务秒杀系统，围绕抢票主链路展示防超卖、幂等、限流、异步削峰、最终一致性、检索、压测和监控。

## 快速概览

```text
TicketRush 是我做的 Java 21 高并发票务秒杀系统，场景来自景区和演出票务抢票。主链路覆盖 Sentinel 限流、Redis 准入令牌、Virtual Threads 库存预占、Redis Lua/Redis Lock/MySQL 乐观锁三种防超卖策略、RocketMQ 异步下单、消费幂等、订单超时关闭补偿，以及 Prometheus/Grafana 监控和 Docker/K3s 部署。它用来证明我能把高并发票务里的防超卖、削峰、幂等、一致性和可观测做成可运行、可压测、可解释的工程闭环。
```

## 完整概览

```text
这个项目不是 CRUD 票务系统，而是聚焦高并发抢票主链路。入口先经过 Sentinel 全局限流和热点票档限流，再经过 Redis 准入令牌，避免所有请求打进库存扣减。库存层提供 Redis Lua、Redis 分布式锁、MySQL 乐观锁三种策略，默认推荐 Redis Lua，因为热点票档库存扣减需要原子性和低延迟。抢票成功后入口快速返回，同时通过 RocketMQ 异步创建订单，消费端用 idempotentKey 保证幂等，订单超时任务负责释放锁定库存。

为了让项目能被验证，我补了 k6 压测、Virtual Threads vs 固定线程池 benchmark、Prometheus/Grafana 指标证据、Seata AT 示例、Elasticsearch 活动/票档读模型和本地辅助演示页。正式展示时优先运行 `.\scripts\demo-smoke.ps1`，用 CLI 证据证明库存 `1000 -> 999 -> 999`、重复请求返回 `A0429`、抢票链路命中 Virtual Thread；页面只作为备用入口。
```

## 演示前检查

使用 JDK 21 构建：

```powershell
$env:JAVA_HOME='C:\Users\xz\.antigravity\extensions\redhat.java-1.54.0-win32-x64\jre\21.0.10-win32-x86_64'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn package -DskipTests
docker compose up -d
Invoke-RestMethod http://localhost:8080/actuator/health
```

运行核心 smoke：

```powershell
.\scripts\demo-smoke.ps1
```

## 5 分钟演示路径

1. 打开 README 顶部的 `TicketRush core demo` 动图，先给结论：这不是票务 CRUD，而是高并发抢票主链路。
2. 运行 `.\scripts\demo-smoke.ps1`，用真实接口跑核心证据：

   ```text
   StockFlow     : 1000 -> 999 -> 999
   DuplicateCode : A0429
   VirtualThread : True
   ```

3. 解释这三个值的含义：

   ```text
   第一次请求扣库存：1000 -> 999
   第二次请求换 requestId：A -> B
   幂等 Key 不变，重复请求被 A0429 拦截
   库存仍然 999，异步下单不重复触发
   ```

4. 打开 README 架构图，讲主链路：

   ```text
   Sentinel -> Redis admission token -> Virtual Thread -> Redis Lua inventory reservation -> RocketMQ async order -> timeout compensation
   ```

5. 展示压测摘要，不现场跑大压测：
   - `docs/rush-benchmark-report.md`：Redis Lua / Redis Lock / MySQL 乐观锁 baseline。
   - `docs/governance-comparison-report.md`：治理开关前后对比。
   - `docs/executor-benchmark-report.md`：Virtual Threads vs 固定线程池。
   - `docs/observability-benchmark-report.md`：Prometheus 指标证据。
6. 解释策略差异：
   - `Redis Lua` 是默认主演示链路。
   - `Redis 分布式锁` 共享 Redis 预热库存。
   - `MySQL 乐观锁` 读取 MySQL 库存表，是数据库热点行对比路径，剩余库存可能和 Redis 路径不同。
7. 需要展示读模型时，再演示 Elasticsearch smoke：

   ```text
   重建索引的活动 ID: 9101781814509
   关键词: Codex Smoke
   活动状态: 售卖中
   票档状态: 上架中
   ```

8. 最后说明边界：本项目不做支付、实名制、完整后台；重点是高并发后端工程闭环。

## CLI 替代演示

优先使用一键脚本：

```powershell
.\scripts\demo-smoke.ps1
```

如果需要手动拆开看每一步，再执行下面命令。

健康检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/system/health
```

库存预热：

```powershell
$body = @{ skuId = 1001; totalStock = 1000 } | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/rush/inventory/preload -Method Post -Body $body -ContentType 'application/json'
```

抢票：

```powershell
$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$body = @{
  requestId = "demo-$suffix"
  userId = 2001
  eventId = 3001
  skuId = 1001
  quantity = 1
  strategy = 'REDIS_LUA'
  idempotentKey = "rush:demo:2001:1001:$suffix"
} | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/rush/tickets -Method Post -Body $body -ContentType 'application/json'
```

执行器 benchmark：

```powershell
$body = @{ mode='VIRTUAL_THREAD'; taskCount=1000; blockingMillis=20; cpuTokens=0; timeoutSeconds=30 } | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/benchmark/executors -Method Post -Body $body -ContentType 'application/json'
```

Elasticsearch smoke：

```powershell
Invoke-RestMethod http://localhost:8080/api/search/events/9101781814509/index -Method Post
Invoke-RestMethod 'http://localhost:8080/api/search/ticket-skus?keyword=Codex%20Smoke&eventId=9101781814509&eventStatus=SELLING&skuStatus=ON_SALE&page=0&size=10'
```

## 设计取舍

### 为什么 Redis Lua 是主推荐方案？

热点票档库存扣减需要把检查库存、扣减库存、写幂等 Key、更新版本放进一个原子操作。Redis Lua 在 Redis 单线程执行模型下天然原子，避免分布式锁的加解锁开销，也比 MySQL 热点行乐观锁更抗冲突。

### Redis 分布式锁和 MySQL 乐观锁有什么瓶颈？

Redis 锁会在热点票档上形成锁竞争，请求需要排队或失败重试，吞吐受锁粒度影响。MySQL 乐观锁依赖热点行条件更新，高并发下大量版本冲突会放大数据库写压力，适合作为对比方案，不适合作为热点抢票主路径。

### Sentinel 和 Redis 准入令牌为什么都要有？

Sentinel 负责入口级全局 QPS 和热点参数保护，尽量在应用侧拦截。Redis 准入令牌进一步限制同一票档同时进入库存扣减的请求数，是靠近核心资源的二级保护。两者组合能避免 Redis/MySQL/RocketMQ 被打穿。

### RocketMQ 异步下单后怎么保证一致性？

抢票入口只做库存预占和消息发送，订单异步创建。消费端基于 `idempotentKey` 幂等落单；消息发送失败时释放已预占库存；订单长时间未支付或未完成时由超时关闭任务释放锁定库存。这是最终一致性，不是同步强一致。

### Seata 在项目里是什么定位？

Seata AT 是一个 scoped demo，用来说明 MySQL 库存预占和订单落库可以用全局事务包裹。但高并发主链路仍选择 Redis/RocketMQ 最终一致性，因为抢票入口需要低延迟和削峰。

### Elasticsearch 为什么不参与抢票写链路？

ES 是活动/票档查询读模型，用于搜索和展示。抢票写链路依赖 Redis/MySQL/RocketMQ 保证库存和订单状态，不能把 ES 这种近实时搜索引擎放进核心一致性路径。

### Virtual Threads 解决什么，不解决什么？

它适合大量 IO 等待任务，比如 Redis、MySQL、MQ、HTTP 调用。它不让 CPU 计算变快，也不能替代限流、缓存、队列和数据库设计。项目里通过执行器 benchmark 展示了纯 IO 等待场景的吞吐差异。

## 范围边界

- 本地演示页用于走通核心链路，不是完整后台管理系统。
- 项目不接真实支付、实名制、短信或多租户 SaaS。
- Seata 是示例，不是抢票主链路默认方案。
- ES 是读模型，不是库存一致性来源。
- 压测数据是本地 Docker 环境结果，不能直接等同生产容量。

## 推荐阅读顺序

```text
README.md
docs/demo-runbook.md
docs/architecture.md
docs/stability-governance.md
docs/final-consistency.md
docs/executor-benchmark-report.md
docs/rush-benchmark-report.md
docs/observability-benchmark-report.md
docs/pitfalls.md
HANDOFF.md
```

## 验证命令

```powershell
mvn test
mvn package -DskipTests
docker compose up -d --no-deps --force-recreate app
Invoke-RestMethod http://localhost:8080/actuator/health
git diff --check
```
