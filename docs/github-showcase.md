# TicketRush GitHub 展示说明

这份说明用于技术读者快速判断 TicketRush 的展示重点。它不是新功能清单，也不替代 README。

## 快速看点

- Java 21 + Spring Boot 3 高并发票务秒杀主链路。
- Sentinel 全局限流、热点参数限流和 Redis 准入令牌保护入口流量。
- Redis Lua、Redis 分布式锁、MySQL 乐观锁三种防超卖策略可对比。
- Virtual Threads 用于库存预占和执行器 benchmark，能展示 IO 等待场景收益。
- RocketMQ 异步下单、消费幂等、订单超时关闭和库存释放补偿形成最终一致性闭环。
- k6、Prometheus/Grafana、Arthas、Docker Compose 和 K3s 文档已形成可验证材料。

## 推荐浏览顺序

1. [README.md](../README.md)：先看后端工程动图、核心 smoke、架构图、压测摘要和验证状态。
2. 运行 `.\scripts\demo-smoke.ps1`：看 `1000 -> 999 -> 999`、`A0429` 和 `processedByVirtualThread=true`。
3. [docs/demo-runbook.md](demo-runbook.md)：看 5 分钟演示路径、CLI 替代演示和设计取舍。
4. [docs/architecture.md](architecture.md)：看抢票主链路、补偿链路和部署视图。
5. [docs/rush-benchmark-report.md](rush-benchmark-report.md)：看三种库存策略 baseline。
6. [docs/executor-benchmark-report.md](executor-benchmark-report.md)：看 Virtual Threads vs 固定线程池对比。
7. [SPEC.md](../SPEC.md)：看阶段进度、验收标准和不继续扩展的边界。

## 本地核心 smoke

推荐演示入口：

```powershell
.\scripts\demo-smoke.ps1
```

这条脚本会真实调用本地接口：

1. 健康检查：`/actuator/health = UP`。
2. 初始化热点票档库存：`1000`。
3. 第一次抢票：库存变为 `999`，`processedByVirtualThread=true`。
4. 第二次请求：换新的 `requestId`，复用同一个 `idempotentKey`。
5. 重复请求返回 `A0429`，库存仍是 `999`。

备用页面入口：

```text
http://localhost:8080/
```

## 已验证证据

- `mvn test`：52 tests，0 failures，0 errors。
- Docker Compose 全链路启动：应用和核心中间件容器已验证。
- 核心 smoke 已覆盖库存预热、抢票成功、用新 requestId 重复提交同一个幂等 Key 和 `A0429` 幂等拦截。
- k6 压测报告覆盖库存策略 baseline、稳定性治理 before/after 和热点分摊对比。
- Prometheus API 导出过压测指标证据。
- Seata AT 示例、Elasticsearch 查询、Redis Lua/Lock、MySQL 乐观锁、RocketMQ Stream binder、MyBatis XML/schema 均有测试或报告覆盖。

## 项目摘要

```text
TicketRush：基于 Java 21 + Spring Boot 3 的高并发票务秒杀系统，围绕抢票主链路实现 Sentinel 限流、Redis 准入令牌、Virtual Threads 库存预占、Redis Lua/分布式锁/MySQL 乐观锁防超卖、RocketMQ 异步下单、消费幂等、订单超时补偿，并补充 k6 压测、Prometheus/Grafana 指标证据和 Docker/K3s 部署材料。
```

## 边界说明

- 本地演示页用于走通核心链路，不是完整后台管理系统。
- 项目不接入真实支付、短信、实名制、多租户 SaaS 或生产订单系统。
- Seata 是 MySQL 库存预占 + 订单落库示例，主抢票链路仍采用 Redis/RocketMQ 最终一致性方案。
- Docker Compose 中的账号密码仅用于本地演示；不要提交 `.env`、真实账号、token、cookie、私钥或本地运行数据。
